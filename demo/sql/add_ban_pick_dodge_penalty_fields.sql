-- Persist Solo Ban/Pick dodge cooldown + audit metadata for dodge penalty v1.
-- Review on local MySQL 8+ and take a backup before execution.

START TRANSACTION;

SET @add_ban_pick_cooldown_until := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'users'
          AND COLUMN_NAME = 'ban_pick_cooldown_until'
    ),
    'SELECT ''users.ban_pick_cooldown_until already exists''',
    'ALTER TABLE users ADD COLUMN ban_pick_cooldown_until DATETIME NULL AFTER note'
);
PREPARE add_ban_pick_cooldown_until_stmt FROM @add_ban_pick_cooldown_until;
EXECUTE add_ban_pick_cooldown_until_stmt;
DEALLOCATE PREPARE add_ban_pick_cooldown_until_stmt;

SET @add_dodged_user_id := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'draft_histories'
          AND COLUMN_NAME = 'dodged_user_id'
    ),
    'SELECT ''draft_histories.dodged_user_id already exists''',
    'ALTER TABLE draft_histories ADD COLUMN dodged_user_id BIGINT NULL AFTER winner_user_id'
);
PREPARE add_dodged_user_id_stmt FROM @add_dodged_user_id;
EXECUTE add_dodged_user_id_stmt;
DEALLOCATE PREPARE add_dodged_user_id_stmt;

SET @add_end_reason := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'draft_histories'
          AND COLUMN_NAME = 'end_reason'
    ),
    'SELECT ''draft_histories.end_reason already exists''',
    'ALTER TABLE draft_histories ADD COLUMN end_reason VARCHAR(32) NOT NULL DEFAULT ''NORMAL'' AFTER dodged_user_id'
);
PREPARE add_end_reason_stmt FROM @add_end_reason;
EXECUTE add_end_reason_stmt;
DEALLOCATE PREPARE add_end_reason_stmt;

UPDATE draft_histories
SET end_reason = 'NORMAL'
WHERE end_reason IS NULL
   OR TRIM(end_reason) = '';

SET @add_dodged_user_index := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'draft_histories'
          AND INDEX_NAME = 'idx_draft_histories_dodged_user_id'
    ),
    'SELECT ''idx_draft_histories_dodged_user_id already exists''',
    'ALTER TABLE draft_histories ADD INDEX idx_draft_histories_dodged_user_id (dodged_user_id)'
);
PREPARE add_dodged_user_index_stmt FROM @add_dodged_user_index;
EXECUTE add_dodged_user_index_stmt;
DEALLOCATE PREPARE add_dodged_user_index_stmt;

SET @add_dodged_user_fk := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.TABLE_CONSTRAINTS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'draft_histories'
          AND CONSTRAINT_NAME = 'fk_draft_histories_dodged_user'
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ),
    'SELECT ''fk_draft_histories_dodged_user already exists''',
    'ALTER TABLE draft_histories ADD CONSTRAINT fk_draft_histories_dodged_user FOREIGN KEY (dodged_user_id) REFERENCES users (id)'
);
PREPARE add_dodged_user_fk_stmt FROM @add_dodged_user_fk;
EXECUTE add_dodged_user_fk_stmt;
DEALLOCATE PREPARE add_dodged_user_fk_stmt;

COMMIT;
