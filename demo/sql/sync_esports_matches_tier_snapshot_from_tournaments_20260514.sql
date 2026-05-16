-- Sync legacy snapshot `esports_matches.tier` from source-of-truth
-- `esports_tournaments.aer_tier` for all linked matches.
--
-- Safe usage:
-- 1. Backup DB before executing on a real dataset.
-- 2. Run the audit query first.
-- 3. Execute the UPDATE only when mismatches > 0.
-- 4. Run the verify query after UPDATE and expect 0 rows.

SELECT
    COUNT(*) AS mismatch_count
FROM esports_matches m
JOIN esports_tournaments t ON t.id = m.tournament_id
WHERE m.tournament_id IS NOT NULL
  AND t.aer_tier IS NOT NULL
  AND (m.tier IS NULL OR m.tier <> CAST(t.aer_tier AS CHAR));

UPDATE esports_matches m
JOIN esports_tournaments t ON t.id = m.tournament_id
SET m.tier = CAST(t.aer_tier AS CHAR)
WHERE m.tournament_id IS NOT NULL
  AND t.aer_tier IS NOT NULL
  AND (m.tier IS NULL OR m.tier <> CAST(t.aer_tier AS CHAR));

SELECT
    m.id,
    m.tier AS match_tier,
    t.aer_tier AS tournament_tier,
    t.name AS tournament_name
FROM esports_matches m
JOIN esports_tournaments t ON t.id = m.tournament_id
WHERE m.tier IS NULL
   OR m.tier <> CAST(t.aer_tier AS CHAR);
