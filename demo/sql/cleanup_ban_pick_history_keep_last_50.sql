-- Retain only the 50 most recent draft histories per participant for stats.
-- A draft history is deleted only when it is outside the top 50 recent histories
-- of both participants. Ordering uses result_recorded_at DESC, then created_at DESC,
-- then id DESC.
--
-- Review on local MySQL 8+ and take a backup before execution.

START TRANSACTION;

SET @retention_limit := 50;
SET SESSION group_concat_max_len = 1048576;

DROP TEMPORARY TABLE IF EXISTS tmp_ranked_participant_histories;
CREATE TEMPORARY TABLE tmp_ranked_participant_histories AS
SELECT ranked.player_id,
       ranked.history_id,
       ranked.winner_user_id,
       ranked.picks,
       ranked.win_rating_delta,
       ranked.loss_rating_delta,
       ranked.recent_rank
FROM (
    SELECT participant.player_id,
           participant.history_id,
           participant.winner_user_id,
           participant.picks,
           participant.win_rating_delta,
           participant.loss_rating_delta,
           ROW_NUMBER() OVER (
               PARTITION BY participant.player_id
               ORDER BY participant.sort_time DESC,
                        participant.created_at DESC,
                        participant.history_id DESC
           ) AS recent_rank
    FROM (
        SELECT h.blue_user_id AS player_id,
               h.id AS history_id,
               h.winner_user_id,
               h.blue_picks AS picks,
               COALESCE(h.win_rating_delta, 30) AS win_rating_delta,
               COALESCE(h.loss_rating_delta, -20) AS loss_rating_delta,
               COALESCE(h.result_recorded_at, h.created_at) AS sort_time,
               h.created_at
        FROM draft_histories h
        UNION ALL
        SELECT h.red_user_id AS player_id,
               h.id AS history_id,
               h.winner_user_id,
               h.red_picks AS picks,
               COALESCE(h.win_rating_delta, 30) AS win_rating_delta,
               COALESCE(h.loss_rating_delta, -20) AS loss_rating_delta,
               COALESCE(h.result_recorded_at, h.created_at) AS sort_time,
               h.created_at
        FROM draft_histories h
    ) participant
) ranked;

CREATE INDEX idx_tmp_rph_player_history
    ON tmp_ranked_participant_histories (player_id, history_id, recent_rank);

DROP TEMPORARY TABLE IF EXISTS tmp_ranked_participant_histories_blue;
CREATE TEMPORARY TABLE tmp_ranked_participant_histories_blue AS
SELECT player_id, history_id, recent_rank
FROM tmp_ranked_participant_histories;

CREATE INDEX idx_tmp_rph_blue_player_history
    ON tmp_ranked_participant_histories_blue (player_id, history_id, recent_rank);

DROP TEMPORARY TABLE IF EXISTS tmp_ranked_participant_histories_red;
CREATE TEMPORARY TABLE tmp_ranked_participant_histories_red AS
SELECT player_id, history_id, recent_rank
FROM tmp_ranked_participant_histories;

CREATE INDEX idx_tmp_rph_red_player_history
    ON tmp_ranked_participant_histories_red (player_id, history_id, recent_rank);

DROP TEMPORARY TABLE IF EXISTS tmp_draft_histories_to_delete;
CREATE TEMPORARY TABLE tmp_draft_histories_to_delete AS
SELECT h.id AS history_id
FROM draft_histories h
JOIN tmp_ranked_participant_histories_blue blue_rank
    ON blue_rank.history_id = h.id
   AND blue_rank.player_id = h.blue_user_id
JOIN tmp_ranked_participant_histories_red red_rank
    ON red_rank.history_id = h.id
   AND red_rank.player_id = h.red_user_id
WHERE blue_rank.recent_rank > @retention_limit
  AND red_rank.recent_rank > @retention_limit;

CREATE INDEX idx_tmp_dhtd_history_id
    ON tmp_draft_histories_to_delete (history_id);

DELETE h
FROM draft_histories h
JOIN tmp_draft_histories_to_delete delete_queue
    ON delete_queue.history_id = h.id;

DROP TEMPORARY TABLE IF EXISTS tmp_player_recent_histories;
DROP TEMPORARY TABLE IF EXISTS tmp_ranked_participant_histories;
CREATE TEMPORARY TABLE tmp_ranked_participant_histories AS
SELECT ranked.player_id,
       ranked.history_id,
       ranked.winner_user_id,
       ranked.picks,
       ranked.win_rating_delta,
       ranked.loss_rating_delta,
       ranked.recent_rank
FROM (
    SELECT participant.player_id,
           participant.history_id,
           participant.winner_user_id,
           participant.picks,
           participant.win_rating_delta,
           participant.loss_rating_delta,
           ROW_NUMBER() OVER (
               PARTITION BY participant.player_id
               ORDER BY participant.sort_time DESC,
                        participant.created_at DESC,
                        participant.history_id DESC
           ) AS recent_rank
    FROM (
        SELECT h.blue_user_id AS player_id,
               h.id AS history_id,
               h.winner_user_id,
               h.blue_picks AS picks,
               COALESCE(h.win_rating_delta, 30) AS win_rating_delta,
               COALESCE(h.loss_rating_delta, -20) AS loss_rating_delta,
               COALESCE(h.result_recorded_at, h.created_at) AS sort_time,
               h.created_at
        FROM draft_histories h
        UNION ALL
        SELECT h.red_user_id AS player_id,
               h.id AS history_id,
               h.winner_user_id,
               h.red_picks AS picks,
               COALESCE(h.win_rating_delta, 30) AS win_rating_delta,
               COALESCE(h.loss_rating_delta, -20) AS loss_rating_delta,
               COALESCE(h.result_recorded_at, h.created_at) AS sort_time,
               h.created_at
        FROM draft_histories h
    ) participant
) ranked;

CREATE INDEX idx_tmp_rph_player_history_after_delete
    ON tmp_ranked_participant_histories (player_id, history_id, recent_rank);

CREATE TEMPORARY TABLE tmp_player_recent_histories AS
SELECT player_id,
       history_id,
       winner_user_id,
       picks,
       win_rating_delta,
       loss_rating_delta,
       recent_rank
FROM tmp_ranked_participant_histories
WHERE recent_rank <= @retention_limit;

CREATE INDEX idx_tmp_prh_player
    ON tmp_player_recent_histories (player_id, recent_rank, history_id);

DROP TEMPORARY TABLE IF EXISTS tmp_player_pick_counts;
CREATE TEMPORARY TABLE tmp_player_pick_counts AS
SELECT recent.player_id AS user_id,
       TRIM(hero_tokens.hero_name) AS hero_name,
       COUNT(*) AS pick_count
FROM tmp_player_recent_histories recent
JOIN JSON_TABLE(
         CONCAT(
             '["',
             REPLACE(
                 REPLACE(
                     REPLACE(
                         REPLACE(COALESCE(recent.picks, ''), '\\', '\\\\'),
                         '"',
                         '\\"'
                     ),
                     CHAR(13),
                     ''
                 ),
                 CHAR(10),
                 '","'
             ),
             '"]'
         ),
         '$[*]' COLUMNS (hero_name VARCHAR(255) PATH '$')
     ) hero_tokens
WHERE TRIM(hero_tokens.hero_name) <> ''
GROUP BY recent.player_id, TRIM(hero_tokens.hero_name);

CREATE INDEX idx_tmp_ppc_user_hero
    ON tmp_player_pick_counts (user_id, hero_name);

DROP TEMPORARY TABLE IF EXISTS tmp_player_pick_counts_serialized;
CREATE TEMPORARY TABLE tmp_player_pick_counts_serialized AS
SELECT pick_counts.user_id,
       GROUP_CONCAT(
           CONCAT(pick_counts.hero_name, CHAR(9), pick_counts.pick_count)
           ORDER BY pick_counts.pick_count DESC, pick_counts.hero_name ASC
           SEPARATOR '\n'
       ) AS picked_hero_counts
FROM tmp_player_pick_counts pick_counts
GROUP BY pick_counts.user_id;

DROP TEMPORARY TABLE IF EXISTS tmp_player_recent_aggregate;
CREATE TEMPORARY TABLE tmp_player_recent_aggregate AS
SELECT recent.player_id AS user_id,
       COUNT(*) AS total_matches,
       SUM(CASE WHEN recent.winner_user_id = recent.player_id THEN 1 ELSE 0 END) AS wins,
       SUM(CASE WHEN recent.winner_user_id IS NOT NULL AND recent.winner_user_id <> recent.player_id THEN 1 ELSE 0 END) AS losses,
       GREATEST(
           0,
           1000
           + COALESCE(SUM(CASE
                              WHEN recent.winner_user_id = recent.player_id
                                  THEN COALESCE(recent.win_rating_delta, 30)
                              ELSE 0
                          END), 0)
           + COALESCE(SUM(CASE
                              WHEN recent.winner_user_id IS NOT NULL
                                   AND recent.winner_user_id <> recent.player_id
                                  THEN COALESCE(recent.loss_rating_delta, -20)
                              ELSE 0
                          END), 0)
       ) AS rating
FROM tmp_player_recent_histories recent
GROUP BY recent.player_id;

DELETE ps
FROM player_stats ps
LEFT JOIN tmp_player_recent_aggregate aggregate_stats
    ON aggregate_stats.user_id = ps.user_id
WHERE aggregate_stats.user_id IS NULL;

INSERT INTO player_stats (
    user_id,
    total_matches,
    wins,
    losses,
    rating,
    picked_hero_counts,
    created_at,
    updated_at
)
SELECT aggregate_stats.user_id,
       aggregate_stats.total_matches,
       aggregate_stats.wins,
       aggregate_stats.losses,
       aggregate_stats.rating,
       serialized_picks.picked_hero_counts,
       NOW(6),
       NOW(6)
FROM tmp_player_recent_aggregate aggregate_stats
LEFT JOIN tmp_player_pick_counts_serialized serialized_picks
    ON serialized_picks.user_id = aggregate_stats.user_id
ON DUPLICATE KEY UPDATE
    total_matches = VALUES(total_matches),
    wins = VALUES(wins),
    losses = VALUES(losses),
    rating = VALUES(rating),
    picked_hero_counts = VALUES(picked_hero_counts),
    updated_at = VALUES(updated_at);

SELECT COUNT(*) AS deleted_history_count
FROM tmp_draft_histories_to_delete;

SELECT COUNT(*) AS remaining_draft_histories
FROM draft_histories;

SELECT COUNT(*) AS remaining_player_stats
FROM player_stats;

COMMIT;
