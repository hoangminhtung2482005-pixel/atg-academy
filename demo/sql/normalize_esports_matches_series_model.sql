-- Normalize esports_matches as series parents and esports_game_drafts as per-game rows.
-- Safe scope:
-- 1. Only groups that already have game drafts.
-- 2. Only groups with unique game_number values inside the candidate series.
-- 3. Recalculate score1/score2 from esports_game_drafts.winner_team_id after merge.
-- 4. Delete only duplicate esports_matches rows that no longer have any draft after the merge.

-- ============================================================================
-- Audit queries requested by task
-- ============================================================================

SELECT COUNT(*) AS total_games
FROM esports_game_drafts;

SELECT COUNT(DISTINCT match_id) AS matches_with_game_drafts
FROM esports_game_drafts;

SELECT match_id, COUNT(*) AS game_count
FROM esports_game_drafts
GROUP BY match_id
ORDER BY game_count DESC, match_id;

SELECT
    d.match_id,
    COUNT(*) AS game_count,
    m.match_date,
    m.team1_code,
    m.team2_code,
    m.score1,
    m.score2,
    m.stage,
    m.tournament_id
FROM esports_game_drafts d
JOIN esports_matches m ON m.id = d.match_id
GROUP BY d.match_id, m.match_date, m.team1_code, m.team2_code, m.score1, m.score2, m.stage, m.tournament_id
HAVING COUNT(*) > 1
ORDER BY game_count DESC, d.match_id;

SELECT
    DATE(m.match_date) AS match_day,
    m.tournament_id,
    m.stage,
    LEAST(m.team1_code, m.team2_code) AS team_a,
    GREATEST(m.team1_code, m.team2_code) AS team_b,
    COUNT(DISTINCT m.id) AS match_rows,
    COUNT(d.id) AS game_rows,
    GROUP_CONCAT(DISTINCT m.id ORDER BY m.id) AS match_ids
FROM esports_matches m
LEFT JOIN esports_game_drafts d ON d.match_id = m.id
GROUP BY
    DATE(m.match_date),
    m.tournament_id,
    m.stage,
    LEAST(m.team1_code, m.team2_code),
    GREATEST(m.team1_code, m.team2_code)
HAVING COUNT(DISTINCT m.id) > 1
ORDER BY match_day, tournament_id, team_a, team_b;

SELECT match_id, game_number, COUNT(*) AS duplicate_count
FROM esports_game_drafts
GROUP BY match_id, game_number
HAVING COUNT(*) > 1;

SELECT id, match_date, team1_code, team2_code, score1, score2, stage, tier, tournament_id
FROM esports_matches
WHERE tournament_id IS NULL
ORDER BY match_date, id;

-- ============================================================================
-- Preview safe merge candidates
-- ============================================================================

DROP TEMPORARY TABLE IF EXISTS tmp_esports_series_safe_groups;
CREATE TEMPORARY TABLE tmp_esports_series_safe_groups AS
SELECT
    DATE(m.match_date) AS match_day,
    m.tournament_id,
    m.stage,
    LEAST(m.team1_code, m.team2_code) AS team_a,
    GREATEST(m.team1_code, m.team2_code) AS team_b,
    MIN(m.id) AS canonical_match_id,
    COUNT(DISTINCT m.id) AS match_rows,
    COUNT(d.id) AS game_rows,
    COUNT(DISTINCT d.game_number) AS distinct_game_numbers,
    SUM(CASE WHEN d.game_number IS NULL THEN 1 ELSE 0 END) AS null_game_numbers,
    GROUP_CONCAT(DISTINCT m.id ORDER BY m.id) AS match_ids,
    GROUP_CONCAT(DISTINCT d.game_number ORDER BY d.game_number) AS game_numbers
FROM esports_matches m
JOIN esports_game_drafts d ON d.match_id = m.id
GROUP BY
    DATE(m.match_date),
    m.tournament_id,
    m.stage,
    LEAST(m.team1_code, m.team2_code),
    GREATEST(m.team1_code, m.team2_code)
HAVING COUNT(DISTINCT m.id) > 1
   AND COUNT(d.id) = COUNT(DISTINCT d.game_number)
   AND SUM(CASE WHEN d.game_number IS NULL THEN 1 ELSE 0 END) = 0;

DROP TEMPORARY TABLE IF EXISTS tmp_esports_series_merge_map;
CREATE TEMPORARY TABLE tmp_esports_series_merge_map AS
SELECT DISTINCT
    m.id AS old_match_id,
    g.canonical_match_id,
    g.match_day,
    g.tournament_id,
    g.stage,
    g.team_a,
    g.team_b
FROM tmp_esports_series_safe_groups g
JOIN esports_matches m
    ON DATE(m.match_date) = g.match_day
   AND ((m.tournament_id = g.tournament_id) OR (m.tournament_id IS NULL AND g.tournament_id IS NULL))
   AND m.stage = g.stage
   AND LEAST(m.team1_code, m.team2_code) = g.team_a
   AND GREATEST(m.team1_code, m.team2_code) = g.team_b
JOIN esports_game_drafts d
    ON d.match_id = m.id
WHERE m.id <> g.canonical_match_id;

SELECT *
FROM tmp_esports_series_safe_groups
ORDER BY match_day, tournament_id, stage, team_a, team_b;

SELECT
    COUNT(*) AS safe_groups,
    SUM(match_rows) AS drafted_match_rows_in_safe_groups,
    SUM(game_rows) AS drafted_game_rows_in_safe_groups
FROM tmp_esports_series_safe_groups;

SELECT COUNT(*) AS duplicate_match_rows_to_merge
FROM tmp_esports_series_merge_map;

SELECT COUNT(*) AS draft_rows_to_repoint
FROM esports_game_drafts d
JOIN tmp_esports_series_merge_map map ON map.old_match_id = d.match_id;

SELECT
    map.canonical_match_id,
    map.old_match_id,
    d.id AS draft_id,
    d.game_number
FROM tmp_esports_series_merge_map map
JOIN esports_game_drafts d ON d.match_id = map.old_match_id
ORDER BY map.canonical_match_id, map.old_match_id, d.game_number;

-- ============================================================================
-- Apply safe merge + score backfill
-- ============================================================================

START TRANSACTION;

UPDATE esports_game_drafts d
JOIN tmp_esports_series_merge_map map ON map.old_match_id = d.match_id
SET d.match_id = map.canonical_match_id;

DROP TEMPORARY TABLE IF EXISTS tmp_esports_series_score_calc;
CREATE TEMPORARY TABLE tmp_esports_series_score_calc AS
SELECT
    d.match_id,
    SUM(CASE WHEN UPPER(COALESCE(wt.team_code, '')) = UPPER(COALESCE(m.team1_code, '')) THEN 1 ELSE 0 END) AS team1_wins,
    SUM(CASE WHEN UPPER(COALESCE(wt.team_code, '')) = UPPER(COALESCE(m.team2_code, '')) THEN 1 ELSE 0 END) AS team2_wins
FROM esports_game_drafts d
JOIN esports_matches m ON m.id = d.match_id
LEFT JOIN esports_teams wt ON wt.id = d.winner_team_id
GROUP BY d.match_id;

SELECT
    m.id,
    m.team1_code,
    m.team2_code,
    m.score1 AS old_score1,
    m.score2 AS old_score2,
    score_calc.team1_wins AS new_score1,
    score_calc.team2_wins AS new_score2
FROM esports_matches m
JOIN tmp_esports_series_score_calc score_calc ON score_calc.match_id = m.id
WHERE COALESCE(m.score1, 0) <> COALESCE(score_calc.team1_wins, 0)
   OR COALESCE(m.score2, 0) <> COALESCE(score_calc.team2_wins, 0)
ORDER BY m.id;

UPDATE esports_matches m
JOIN tmp_esports_series_score_calc score_calc ON score_calc.match_id = m.id
SET m.score1 = score_calc.team1_wins,
    m.score2 = score_calc.team2_wins;

SELECT COUNT(*) AS orphan_duplicate_matches_ready_to_delete
FROM esports_matches m
JOIN tmp_esports_series_merge_map map ON map.old_match_id = m.id
LEFT JOIN esports_game_drafts d ON d.match_id = m.id
WHERE d.id IS NULL;

DELETE m
FROM esports_matches m
JOIN tmp_esports_series_merge_map map ON map.old_match_id = m.id
LEFT JOIN esports_game_drafts d ON d.match_id = m.id
WHERE d.id IS NULL;

COMMIT;

-- ============================================================================
-- Verification queries after update
-- ============================================================================

SELECT COUNT(*) AS total_games_after
FROM esports_game_drafts;

SELECT COUNT(DISTINCT match_id) AS matches_with_game_drafts_after
FROM esports_game_drafts;

SELECT match_id, game_number, COUNT(*) AS duplicate_count_after
FROM esports_game_drafts
GROUP BY match_id, game_number
HAVING COUNT(*) > 1;

SELECT
    DATE(m.match_date) AS match_day,
    m.tournament_id,
    m.stage,
    LEAST(m.team1_code, m.team2_code) AS team_a,
    GREATEST(m.team1_code, m.team2_code) AS team_b,
    COUNT(DISTINCT m.id) AS drafted_match_rows_after,
    COUNT(d.id) AS game_rows_after
FROM esports_matches m
JOIN esports_game_drafts d ON d.match_id = m.id
GROUP BY
    DATE(m.match_date),
    m.tournament_id,
    m.stage,
    LEAST(m.team1_code, m.team2_code),
    GREATEST(m.team1_code, m.team2_code)
HAVING COUNT(DISTINCT m.id) > 1
ORDER BY match_day, tournament_id, stage, team_a, team_b;

SELECT
    m.id,
    m.team1_code,
    m.team2_code,
    m.score1,
    m.score2,
    score_calc.team1_wins,
    score_calc.team2_wins
FROM esports_matches m
JOIN tmp_esports_series_score_calc score_calc ON score_calc.match_id = m.id
WHERE COALESCE(m.score1, 0) <> COALESCE(score_calc.team1_wins, 0)
   OR COALESCE(m.score2, 0) <> COALESCE(score_calc.team2_wins, 0)
ORDER BY m.id;

SELECT id, match_date, team1_code, team2_code, score1, score2, stage, tournament_id
FROM esports_matches
WHERE id IN (868, 869)
ORDER BY id;

SELECT COUNT(*) AS remaining_old_match_rows_from_safe_merge
FROM esports_matches m
JOIN tmp_esports_series_merge_map map ON map.old_match_id = m.id;
