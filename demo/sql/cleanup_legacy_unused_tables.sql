-- WARNING:
-- Backup database before running this script.
-- Run only after Java code no longer references the legacy entities/tables.
-- Do NOT drop meta_tier_lists in this cleanup.

SELECT TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (
    'arcanas',
    'bang_ngoc',
    'huong_dan_ngoc',
    'items',
    'vat_pham',
    'huong_dan_vat_pham',
    'esports_match_draft_actions',
    'esports_match_game_lineups',
    'esports_match_games'
  )
ORDER BY TABLE_NAME;

SELECT
    TABLE_NAME,
    COLUMN_NAME,
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
  AND REFERENCED_TABLE_NAME IS NOT NULL
  AND (
    TABLE_NAME IN (
      'arcanas',
      'bang_ngoc',
      'huong_dan_ngoc',
      'items',
      'vat_pham',
      'huong_dan_vat_pham',
      'esports_match_draft_actions',
      'esports_match_game_lineups',
      'esports_match_games'
    )
    OR REFERENCED_TABLE_NAME IN (
      'arcanas',
      'bang_ngoc',
      'huong_dan_ngoc',
      'items',
      'vat_pham',
      'huong_dan_vat_pham',
      'esports_match_draft_actions',
      'esports_match_game_lineups',
      'esports_match_games'
    )
  )
ORDER BY REFERENCED_TABLE_NAME, TABLE_NAME, COLUMN_NAME;

SELECT CONCAT(
    'SELECT ''', TABLE_NAME, ''' AS table_name, COUNT(*) AS rows_count FROM `', TABLE_NAME, '`;'
) AS count_sql
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (
    'arcanas',
    'bang_ngoc',
    'huong_dan_ngoc',
    'items',
    'vat_pham',
    'huong_dan_vat_pham',
    'esports_match_draft_actions',
    'esports_match_game_lineups',
    'esports_match_games'
  )
ORDER BY TABLE_NAME;

-- RUN ONLY AFTER BACKUP + COMPILE/TEST PASS + FK CHECK REVIEWED
-- Do NOT drop meta_tier_lists.
-- Drops are ordered from dependent child tables to parent tables.

-- Legacy Arcana/Ngoc
DROP TABLE IF EXISTS huong_dan_ngoc;
DROP TABLE IF EXISTS bang_ngoc;
DROP TABLE IF EXISTS arcanas;

-- Legacy Item/Trang bi
DROP TABLE IF EXISTS huong_dan_vat_pham;
DROP TABLE IF EXISTS vat_pham;
DROP TABLE IF EXISTS items;

-- Old Esports Data model, replaced by esports_game_drafts.
DROP TABLE IF EXISTS esports_match_draft_actions;
DROP TABLE IF EXISTS esports_match_game_lineups;
DROP TABLE IF EXISTS esports_match_games;

SELECT TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN (
    'arcanas',
    'bang_ngoc',
    'huong_dan_ngoc',
    'items',
    'vat_pham',
    'huong_dan_vat_pham',
    'esports_match_draft_actions',
    'esports_match_game_lineups',
    'esports_match_games'
  )
ORDER BY TABLE_NAME;
