-- WARNING:
-- Backup DB before running this script.
-- Adds numeric AER tier for tournament-level export/conversion logic.

SELECT COLUMN_NAME
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'esports_tournaments'
  AND COLUMN_NAME = 'aer_tier';

SET @has_aer_tier := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'esports_tournaments'
      AND COLUMN_NAME = 'aer_tier'
);

SET @sql := IF(
    @has_aer_tier = 0,
    'ALTER TABLE esports_tournaments ADD COLUMN aer_tier INT NOT NULL DEFAULT 1 AFTER tier_level',
    'SELECT ''esports_tournaments.aer_tier already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE esports_tournaments
SET aer_tier = 1
WHERE aer_tier IS NULL
   OR aer_tier <= 0;

ALTER TABLE esports_tournaments
    MODIFY COLUMN aer_tier INT NOT NULL DEFAULT 1;

SELECT
    t.id,
    t.name,
    t.slug,
    t.season_year,
    t.split_name,
    t.tier_level,
    t.aer_tier,
    t.status,
    f.code AS franchise_code,
    f.name AS franchise_name
FROM esports_tournaments t
LEFT JOIN esports_franchises f ON f.id = t.franchise_id
ORDER BY f.code, t.season_year, t.name;

SELECT *
FROM esports_tournaments
WHERE name IS NULL
   OR slug IS NULL
   OR franchise_id IS NULL
   OR aer_tier IS NULL
   OR aer_tier <= 0;

SELECT slug, COUNT(*) AS count
FROM esports_tournaments
GROUP BY slug
HAVING COUNT(*) > 1;
