-- TASK 6B: cleanup 8 exact duplicate esports series created by a previous import.
-- Scope is limited to the exact duplicate pairs below:
--   1682 -> 1618
--   1683 -> 1623
--   1684 -> 1627
--   1685 -> 1631
--   1686 -> 1635
--   1687 -> 1639
--   1688 -> 1644
--   1689 -> 1648
--
-- Safety rules:
-- - Keep the canonical old parents.
-- - Do not touch the 12 stage-conflict ids: 1674..1681, 1690..1693.
-- - Do not repoint drafts unless a pair is proven safe. For this exact cleanup,
--   delete duplicate child drafts only when the canonical parent already has the
--   same game_number and the draft payload matches exactly.
-- - Delete duplicate parent rows only after all of their child drafts are gone.
-- - No schema changes.

DROP TEMPORARY TABLE IF EXISTS tmp_cleanup_exact_series_map;
CREATE TEMPORARY TABLE tmp_cleanup_exact_series_map (
    canonical_match_id BIGINT NOT NULL,
    duplicate_match_id BIGINT NOT NULL,
    PRIMARY KEY (duplicate_match_id),
    UNIQUE KEY uk_tmp_cleanup_exact_series_map_pair (canonical_match_id, duplicate_match_id)
);

INSERT INTO tmp_cleanup_exact_series_map (canonical_match_id, duplicate_match_id)
VALUES
    (1618, 1682),
    (1623, 1683),
    (1627, 1684),
    (1631, 1685),
    (1635, 1686),
    (1639, 1687),
    (1644, 1688),
    (1648, 1689);

-- ============================================================================
-- Pre-cleanup audit requested by the task
-- ============================================================================

SELECT
    m.id,
    DATE(m.match_date) AS match_day,
    TIME(m.match_date) AS match_time,
    m.tournament_id,
    t.name AS tournament_name,
    m.stage,
    m.team1_code,
    m.team2_code,
    m.score1,
    m.score2,
    COUNT(d.id) AS draft_count,
    GROUP_CONCAT(d.game_number ORDER BY d.game_number) AS game_numbers
FROM esports_matches m
LEFT JOIN esports_tournaments t ON t.id = m.tournament_id
LEFT JOIN esports_game_drafts d ON d.match_id = m.id
WHERE m.id IN (
    1618, 1623, 1627, 1631, 1635, 1639, 1644, 1648,
    1682, 1683, 1684, 1685, 1686, 1687, 1688, 1689
)
GROUP BY m.id, match_day, match_time, m.tournament_id, t.name, m.stage,
         m.team1_code, m.team2_code, m.score1, m.score2
ORDER BY match_day, m.team1_code, m.team2_code, m.id;

SELECT match_id, game_number, COUNT(*) AS count
FROM esports_game_drafts
WHERE match_id IN (
    1618, 1623, 1627, 1631, 1635, 1639, 1644, 1648,
    1682, 1683, 1684, 1685, 1686, 1687, 1688, 1689
)
GROUP BY match_id, game_number
HAVING COUNT(*) > 1;

-- Build the exact draft match table. The payload comparison intentionally ignores
-- row ids, match_id, and timestamps; the imported duplicates only differed by
-- created_at in the local audit.
DROP TEMPORARY TABLE IF EXISTS tmp_cleanup_exact_series_exact_drafts;
CREATE TEMPORARY TABLE tmp_cleanup_exact_series_exact_drafts AS
SELECT
    map.canonical_match_id,
    map.duplicate_match_id,
    cd.id AS canonical_draft_id,
    dd.id AS duplicate_draft_id,
    dd.game_number
FROM tmp_cleanup_exact_series_map map
JOIN esports_game_drafts dd
    ON dd.match_id = map.duplicate_match_id
JOIN esports_game_drafts cd
    ON cd.match_id = map.canonical_match_id
   AND cd.game_number = dd.game_number
WHERE cd.blue_team_id <=> dd.blue_team_id
  AND cd.red_team_id <=> dd.red_team_id
  AND cd.winner_team_id <=> dd.winner_team_id
  AND cd.duration_seconds <=> dd.duration_seconds
  AND cd.draft_format_code <=> dd.draft_format_code
  AND cd.source <=> dd.source
  AND cd.blue_ban_1_hero_id <=> dd.blue_ban_1_hero_id
  AND cd.blue_ban_2_hero_id <=> dd.blue_ban_2_hero_id
  AND cd.blue_ban_3_hero_id <=> dd.blue_ban_3_hero_id
  AND cd.blue_ban_4_hero_id <=> dd.blue_ban_4_hero_id
  AND cd.blue_ban_5_hero_id <=> dd.blue_ban_5_hero_id
  AND cd.red_ban_1_hero_id <=> dd.red_ban_1_hero_id
  AND cd.red_ban_2_hero_id <=> dd.red_ban_2_hero_id
  AND cd.red_ban_3_hero_id <=> dd.red_ban_3_hero_id
  AND cd.red_ban_4_hero_id <=> dd.red_ban_4_hero_id
  AND cd.red_ban_5_hero_id <=> dd.red_ban_5_hero_id
  AND cd.blue_dsl_hero_id <=> dd.blue_dsl_hero_id
  AND cd.blue_jgl_hero_id <=> dd.blue_jgl_hero_id
  AND cd.blue_mid_hero_id <=> dd.blue_mid_hero_id
  AND cd.blue_adl_hero_id <=> dd.blue_adl_hero_id
  AND cd.blue_sup_hero_id <=> dd.blue_sup_hero_id
  AND cd.red_dsl_hero_id <=> dd.red_dsl_hero_id
  AND cd.red_jgl_hero_id <=> dd.red_jgl_hero_id
  AND cd.red_mid_hero_id <=> dd.red_mid_hero_id
  AND cd.red_adl_hero_id <=> dd.red_adl_hero_id
  AND cd.red_sup_hero_id <=> dd.red_sup_hero_id
  AND cd.raw_draft_json <=> dd.raw_draft_json;

DROP TEMPORARY TABLE IF EXISTS tmp_cleanup_exact_series_blocked_drafts;
CREATE TEMPORARY TABLE tmp_cleanup_exact_series_blocked_drafts AS
SELECT
    map.canonical_match_id,
    map.duplicate_match_id,
    dd.id AS duplicate_draft_id,
    dd.game_number
FROM tmp_cleanup_exact_series_map map
JOIN esports_game_drafts dd
    ON dd.match_id = map.duplicate_match_id
LEFT JOIN tmp_cleanup_exact_series_exact_drafts exact_draft
    ON exact_draft.duplicate_draft_id = dd.id
WHERE exact_draft.duplicate_draft_id IS NULL;

DROP TEMPORARY TABLE IF EXISTS tmp_cleanup_exact_series_pair_status;
CREATE TEMPORARY TABLE tmp_cleanup_exact_series_pair_status AS
SELECT
    map.canonical_match_id,
    map.duplicate_match_id,
    DATE(cm.match_date) AS canonical_match_day,
    DATE(dm.match_date) AS duplicate_match_day,
    TIME(cm.match_date) AS canonical_match_time,
    TIME(dm.match_date) AS duplicate_match_time,
    cm.tournament_id AS canonical_tournament_id,
    dm.tournament_id AS duplicate_tournament_id,
    cm.stage AS canonical_stage,
    dm.stage AS duplicate_stage,
    cm.team1_code AS canonical_team1_code,
    cm.team2_code AS canonical_team2_code,
    dm.team1_code AS duplicate_team1_code,
    dm.team2_code AS duplicate_team2_code,
    cm.score1 AS canonical_score1,
    cm.score2 AS canonical_score2,
    dm.score1 AS duplicate_score1,
    dm.score2 AS duplicate_score2,
    COALESCE(cdraft.canonical_draft_count, 0) AS canonical_draft_count,
    COALESCE(ddraft.duplicate_draft_count, 0) AS duplicate_draft_count,
    COALESCE(exact_count.exact_payload_draft_count, 0) AS exact_payload_draft_count,
    COALESCE(cgames.canonical_game_numbers, '') AS canonical_game_numbers,
    COALESCE(dgames.duplicate_game_numbers, '') AS duplicate_game_numbers,
    (
        DATE(cm.match_date) = DATE(dm.match_date)
        AND cm.tournament_id <=> dm.tournament_id
        AND cm.stage <=> dm.stage
        AND LEAST(cm.team1_code, cm.team2_code) = LEAST(dm.team1_code, dm.team2_code)
        AND GREATEST(cm.team1_code, cm.team2_code) = GREATEST(dm.team1_code, dm.team2_code)
        AND cm.score1 <=> dm.score1
        AND cm.score2 <=> dm.score2
    ) AS exact_parent_signature,
    (
        DATE(cm.match_date) = DATE(dm.match_date)
        AND cm.tournament_id <=> dm.tournament_id
        AND cm.stage <=> dm.stage
        AND LEAST(cm.team1_code, cm.team2_code) = LEAST(dm.team1_code, dm.team2_code)
        AND GREATEST(cm.team1_code, cm.team2_code) = GREATEST(dm.team1_code, dm.team2_code)
        AND cm.score1 <=> dm.score1
        AND cm.score2 <=> dm.score2
        AND COALESCE(cdraft.canonical_draft_count, 0) = COALESCE(ddraft.duplicate_draft_count, 0)
        AND COALESCE(ddraft.duplicate_draft_count, 0) = COALESCE(exact_count.exact_payload_draft_count, 0)
        AND COALESCE(cgames.canonical_game_numbers, '') = COALESCE(dgames.duplicate_game_numbers, '')
    ) AS safe_to_cleanup
FROM tmp_cleanup_exact_series_map map
JOIN esports_matches cm
    ON cm.id = map.canonical_match_id
JOIN esports_matches dm
    ON dm.id = map.duplicate_match_id
LEFT JOIN (
    SELECT
        match_id,
        COUNT(*) AS canonical_draft_count
    FROM esports_game_drafts
    GROUP BY match_id
) cdraft
    ON cdraft.match_id = map.canonical_match_id
LEFT JOIN (
    SELECT
        match_id,
        COUNT(*) AS duplicate_draft_count
    FROM esports_game_drafts
    GROUP BY match_id
) ddraft
    ON ddraft.match_id = map.duplicate_match_id
LEFT JOIN (
    SELECT
        duplicate_match_id,
        COUNT(*) AS exact_payload_draft_count
    FROM tmp_cleanup_exact_series_exact_drafts
    GROUP BY duplicate_match_id
) exact_count
    ON exact_count.duplicate_match_id = map.duplicate_match_id
LEFT JOIN (
    SELECT
        match_id,
        GROUP_CONCAT(game_number ORDER BY game_number) AS canonical_game_numbers
    FROM esports_game_drafts
    GROUP BY match_id
) cgames
    ON cgames.match_id = map.canonical_match_id
LEFT JOIN (
    SELECT
        match_id,
        GROUP_CONCAT(game_number ORDER BY game_number) AS duplicate_game_numbers
    FROM esports_game_drafts
    GROUP BY match_id
) dgames
    ON dgames.match_id = map.duplicate_match_id;

SELECT *
FROM tmp_cleanup_exact_series_pair_status
ORDER BY canonical_match_id;

SELECT *
FROM tmp_cleanup_exact_series_blocked_drafts
ORDER BY duplicate_match_id, game_number, duplicate_draft_id;

SELECT
    SUM(CASE WHEN safe_to_cleanup = 1 THEN 1 ELSE 0 END) AS safe_pair_count,
    SUM(CASE WHEN safe_to_cleanup = 0 THEN 1 ELSE 0 END) AS blocked_pair_count,
    (SELECT COUNT(*) FROM tmp_cleanup_exact_series_exact_drafts) AS exact_payload_draft_rows,
    (SELECT COUNT(*) FROM tmp_cleanup_exact_series_blocked_drafts) AS blocked_draft_rows
FROM tmp_cleanup_exact_series_pair_status;

-- No repoint is needed for this task because every duplicate parent already has a
-- canonical parent with the same game_number set and the same draft payload.

START TRANSACTION;

DELETE d
FROM esports_game_drafts d
JOIN tmp_cleanup_exact_series_exact_drafts exact_draft
    ON exact_draft.duplicate_draft_id = d.id
JOIN tmp_cleanup_exact_series_pair_status pair_status
    ON pair_status.duplicate_match_id = exact_draft.duplicate_match_id
WHERE pair_status.safe_to_cleanup = 1;

SELECT ROW_COUNT() AS deleted_duplicate_draft_rows;

DELETE m
FROM esports_matches m
JOIN tmp_cleanup_exact_series_pair_status pair_status
    ON pair_status.duplicate_match_id = m.id
LEFT JOIN esports_game_drafts d
    ON d.match_id = m.id
WHERE pair_status.safe_to_cleanup = 1
  AND d.id IS NULL;

SELECT ROW_COUNT() AS deleted_duplicate_match_rows;

COMMIT;

-- ============================================================================
-- Verification after cleanup
-- ============================================================================

SELECT COUNT(*) AS total_games FROM esports_game_drafts;

SELECT COUNT(DISTINCT match_id) AS total_series FROM esports_game_drafts;

SELECT match_id, game_number, COUNT(*) AS count
FROM esports_game_drafts
GROUP BY match_id, game_number
HAVING COUNT(*) > 1;

SELECT
    m.id,
    m.team1_code,
    m.team2_code,
    m.score1,
    m.score2,
    SUM(CASE WHEN UPPER(COALESCE(wt.team_code, '')) = UPPER(COALESCE(m.team1_code, '')) THEN 1 ELSE 0 END) AS calc_score1,
    SUM(CASE WHEN UPPER(COALESCE(wt.team_code, '')) = UPPER(COALESCE(m.team2_code, '')) THEN 1 ELSE 0 END) AS calc_score2
FROM esports_matches m
JOIN esports_game_drafts d
    ON d.match_id = m.id
LEFT JOIN esports_teams wt
    ON wt.id = d.winner_team_id
GROUP BY m.id, m.team1_code, m.team2_code, m.score1, m.score2
HAVING COALESCE(m.score1, 0) <> COALESCE(calc_score1, 0)
    OR COALESCE(m.score2, 0) <> COALESCE(calc_score2, 0)
ORDER BY m.id;

SELECT *
FROM esports_matches
WHERE id IN (1682, 1683, 1684, 1685, 1686, 1687, 1688, 1689);

SELECT
    m.id,
    DATE(m.match_date) AS match_day,
    TIME(m.match_date) AS match_time,
    m.tournament_id,
    t.name AS tournament_name,
    m.stage,
    m.team1_code,
    m.team2_code,
    m.score1,
    m.score2,
    COUNT(d.id) AS draft_count,
    GROUP_CONCAT(d.game_number ORDER BY d.game_number) AS game_numbers
FROM esports_matches m
LEFT JOIN esports_tournaments t ON t.id = m.tournament_id
LEFT JOIN esports_game_drafts d ON d.match_id = m.id
WHERE m.id IN (
    1618, 1623, 1627, 1631, 1635, 1639, 1644, 1648,
    1682, 1683, 1684, 1685, 1686, 1687, 1688, 1689
)
GROUP BY m.id, match_day, match_time, m.tournament_id, t.name, m.stage,
         m.team1_code, m.team2_code, m.score1, m.score2
ORDER BY match_day, m.team1_code, m.team2_code, m.id;
