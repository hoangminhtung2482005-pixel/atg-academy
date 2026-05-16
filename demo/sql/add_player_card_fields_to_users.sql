-- Persist Player Card badge/title preferences on users so Account Center can
-- save settings that are reused by Solo Ban/Pick and other shared surfaces.
-- Review on local MySQL 8+ and take a backup before execution.

START TRANSACTION;

SET @add_player_badge_code := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'users'
          AND COLUMN_NAME = 'player_badge_code'
    ),
    'SELECT ''users.player_badge_code already exists''',
    'ALTER TABLE users ADD COLUMN player_badge_code VARCHAR(40) NULL DEFAULT ''default'' AFTER level'
);
PREPARE add_player_badge_code_stmt FROM @add_player_badge_code;
EXECUTE add_player_badge_code_stmt;
DEALLOCATE PREPARE add_player_badge_code_stmt;

SET @add_player_badge_name := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'users'
          AND COLUMN_NAME = 'player_badge_name'
    ),
    'SELECT ''users.player_badge_name already exists''',
    'ALTER TABLE users ADD COLUMN player_badge_name VARCHAR(80) NULL DEFAULT ''ATG Player'' AFTER player_badge_code'
);
PREPARE add_player_badge_name_stmt FROM @add_player_badge_name;
EXECUTE add_player_badge_name_stmt;
DEALLOCATE PREPARE add_player_badge_name_stmt;

SET @add_player_badge_icon_url := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'users'
          AND COLUMN_NAME = 'player_badge_icon_url'
    ),
    'SELECT ''users.player_badge_icon_url already exists''',
    'ALTER TABLE users ADD COLUMN player_badge_icon_url VARCHAR(500) NULL DEFAULT NULL AFTER player_badge_name'
);
PREPARE add_player_badge_icon_url_stmt FROM @add_player_badge_icon_url;
EXECUTE add_player_badge_icon_url_stmt;
DEALLOCATE PREPARE add_player_badge_icon_url_stmt;

SET @add_player_title := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'users'
          AND COLUMN_NAME = 'player_title'
    ),
    'SELECT ''users.player_title already exists''',
    'ALTER TABLE users ADD COLUMN player_title VARCHAR(120) NULL DEFAULT ''✦ Tân Binh Ban/Pick ✦'' AFTER player_badge_icon_url'
);
PREPARE add_player_title_stmt FROM @add_player_title;
EXECUTE add_player_title_stmt;
DEALLOCATE PREPARE add_player_title_stmt;

UPDATE users
SET player_badge_code = COALESCE(NULLIF(TRIM(player_badge_code), ''), 'default'),
    player_badge_name = COALESCE(NULLIF(TRIM(player_badge_name), ''), 'ATG Player'),
    player_badge_icon_url = NULLIF(TRIM(player_badge_icon_url), ''),
    player_title = COALESCE(NULLIF(TRIM(player_title), ''), '✦ Tân Binh Ban/Pick ✦')
WHERE player_badge_code IS NULL
   OR TRIM(player_badge_code) = ''
   OR player_badge_name IS NULL
   OR TRIM(player_badge_name) = ''
   OR (player_badge_icon_url IS NOT NULL AND TRIM(player_badge_icon_url) = '')
   OR player_title IS NULL
   OR TRIM(player_title) = '';

COMMIT;
