-- Backfill tournament_id for the specific GCS Spring 2026 parent matches used by
-- the XLSX Esports Game Draft import preview on 2026-03-27.
--
-- Context:
-- - Tournament: GCS Spring 2026 (expected id = 4 on the current local DB)
-- - Guaranteed-safe parent match: ONE vs ANK, score 3-0, stage bang -> match id 869
-- - Targeted companion match for the same verified XLSX block: DCG vs LIT, score 3-0, stage bang -> match id 868
-- - A second DCG vs LIT 3-0 seed row also exists as match id 862 and is intentionally left NULL
--   to avoid creating multiple exact tournament matches for the importer.

SELECT id, name, slug, aer_tier
FROM esports_tournaments
WHERE name = 'GCS Spring 2026';

SELECT id,
       match_date,
       team1_code,
       team2_code,
       score1,
       score2,
       tier,
       stage,
       tournament_id
FROM esports_matches
WHERE DATE(match_date) = '2026-03-27'
  AND (
    (team1_code IN ('ANK', 'ONE') AND team2_code IN ('ANK', 'ONE'))
    OR
    (team1_code IN ('LIT', 'DCG') AND team2_code IN ('LIT', 'DCG'))
  )
ORDER BY id;

SELECT id,
       match_date,
       team1_code,
       team2_code,
       score1,
       score2,
       tier,
       stage,
       tournament_id
FROM esports_matches
WHERE tournament_id IS NULL
  AND (
    (id = 868
        AND DATE(match_date) = '2026-03-27'
        AND stage = 'bang'
        AND team1_code = 'DCG'
        AND team2_code = 'LIT'
        AND score1 = 3
        AND score2 = 0)
    OR
    (id = 869
        AND DATE(match_date) = '2026-03-27'
        AND stage = 'bang'
        AND team1_code = 'ONE'
        AND team2_code = 'ANK'
        AND score1 = 3
        AND score2 = 0)
  )
ORDER BY id;

UPDATE esports_matches
SET tournament_id = 4
WHERE tournament_id IS NULL
  AND (
    (id = 868
        AND DATE(match_date) = '2026-03-27'
        AND stage = 'bang'
        AND team1_code = 'DCG'
        AND team2_code = 'LIT'
        AND score1 = 3
        AND score2 = 0)
    OR
    (id = 869
        AND DATE(match_date) = '2026-03-27'
        AND stage = 'bang'
        AND team1_code = 'ONE'
        AND team2_code = 'ANK'
        AND score1 = 3
        AND score2 = 0)
  );

SELECT ROW_COUNT() AS rows_updated;

SELECT id,
       match_date,
       team1_code,
       team2_code,
       score1,
       score2,
       tier,
       stage,
       tournament_id
FROM esports_matches
WHERE DATE(match_date) = '2026-03-27'
  AND (
    (team1_code IN ('ANK', 'ONE') AND team2_code IN ('ANK', 'ONE'))
    OR
    (team1_code IN ('LIT', 'DCG') AND team2_code IN ('LIT', 'DCG'))
  )
ORDER BY id;
