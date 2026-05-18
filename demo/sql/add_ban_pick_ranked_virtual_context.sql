-- Add persisted virtual BO7 context fields for Solo Ban/Pick Rank Mode.
-- Target DB is resolved from demo/src/main/resources/application.properties:
--   jdbc:mysql://localhost:3306/aov_tactics

SET @schema_name = DATABASE();

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'ban_pick_rooms'
          AND column_name = 'virtual_series_format'
    ),
    'SELECT ''virtual_series_format already exists''',
    'ALTER TABLE ban_pick_rooms ADD COLUMN virtual_series_format VARCHAR(8) NULL AFTER mode'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'ban_pick_rooms'
          AND column_name = 'virtual_game_index'
    ),
    'SELECT ''virtual_game_index already exists''',
    'ALTER TABLE ban_pick_rooms ADD COLUMN virtual_game_index INT NULL AFTER virtual_series_format'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'ban_pick_rooms'
          AND column_name = 'ultimate_battle'
    ),
    'SELECT ''ultimate_battle already exists''',
    'ALTER TABLE ban_pick_rooms ADD COLUMN ultimate_battle BOOLEAN NOT NULL DEFAULT FALSE AFTER virtual_game_index'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'ban_pick_rooms'
          AND column_name = 'prep_duration_seconds'
    ),
    'SELECT ''prep_duration_seconds already exists''',
    'ALTER TABLE ban_pick_rooms ADD COLUMN prep_duration_seconds INT NOT NULL DEFAULT 0 AFTER ultimate_battle'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'ban_pick_rooms'
          AND column_name = 'blue_previous_used_hero_ids'
    ),
    'SELECT ''blue_previous_used_hero_ids already exists''',
    'ALTER TABLE ban_pick_rooms ADD COLUMN blue_previous_used_hero_ids TEXT NULL AFTER prep_duration_seconds'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'ban_pick_rooms'
          AND column_name = 'red_previous_used_hero_ids'
    ),
    'SELECT ''red_previous_used_hero_ids already exists''',
    'ALTER TABLE ban_pick_rooms ADD COLUMN red_previous_used_hero_ids TEXT NULL AFTER blue_previous_used_hero_ids'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'ban_pick_rooms'
          AND column_name = 'prep_phase_start_at'
    ),
    'SELECT ''prep_phase_start_at already exists''',
    'ALTER TABLE ban_pick_rooms ADD COLUMN prep_phase_start_at DATETIME(6) NULL AFTER red_previous_used_hero_ids'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF (
    EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = @schema_name
          AND table_name = 'ban_pick_rooms'
          AND column_name = 'prep_phase_end_at'
    ),
    'SELECT ''prep_phase_end_at already exists''',
    'ALTER TABLE ban_pick_rooms ADD COLUMN prep_phase_end_at DATETIME(6) NULL AFTER prep_phase_start_at'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE ban_pick_rooms
SET virtual_series_format = 'BO7',
    series_type = 'BO1'
WHERE mode = 'RANKED';

UPDATE ban_pick_rooms
SET ultimate_battle = COALESCE(ultimate_battle, FALSE),
    prep_duration_seconds = COALESCE(prep_duration_seconds, 0);
