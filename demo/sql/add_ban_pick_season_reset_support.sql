-- Support Solo Ban/Pick seasonal rank reset with admin preview/trigger,
-- scheduled reset audit log, and replay-safe rating anchors.
-- Review on local MySQL 8+ and take a backup before execution.

START TRANSACTION;

SET @add_rating_anchor := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'player_stats'
          AND COLUMN_NAME = 'rating_anchor'
    ),
    'SELECT ''player_stats.rating_anchor already exists''',
    'ALTER TABLE player_stats ADD COLUMN rating_anchor INT NULL AFTER rating'
);
PREPARE add_rating_anchor_stmt FROM @add_rating_anchor;
EXECUTE add_rating_anchor_stmt;
DEALLOCATE PREPARE add_rating_anchor_stmt;

SET @add_rating_anchor_at := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'player_stats'
          AND COLUMN_NAME = 'rating_anchor_at'
    ),
    'SELECT ''player_stats.rating_anchor_at already exists''',
    'ALTER TABLE player_stats ADD COLUMN rating_anchor_at DATETIME(6) NULL AFTER rating_anchor'
);
PREPARE add_rating_anchor_at_stmt FROM @add_rating_anchor_at;
EXECUTE add_rating_anchor_at_stmt;
DEALLOCATE PREPARE add_rating_anchor_at_stmt;

SET @add_last_reset_type := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'player_stats'
          AND COLUMN_NAME = 'last_reset_type'
    ),
    'SELECT ''player_stats.last_reset_type already exists''',
    'ALTER TABLE player_stats ADD COLUMN last_reset_type VARCHAR(16) NULL AFTER rating_anchor_at'
);
PREPARE add_last_reset_type_stmt FROM @add_last_reset_type;
EXECUTE add_last_reset_type_stmt;
DEALLOCATE PREPARE add_last_reset_type_stmt;

CREATE TABLE IF NOT EXISTS ban_pick_rank_resets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reset_type VARCHAR(16) NOT NULL,
    scheduled_date DATE NOT NULL,
    executed_at DATETIME(6) NOT NULL,
    affected_players INT NOT NULL DEFAULT 0,
    base_rating INT NOT NULL DEFAULT 1000,
    formula VARCHAR(255) NOT NULL,
    executed_by VARCHAR(255) NULL,
    note TEXT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ban_pick_rank_resets_scheduled_date (scheduled_date)
);

COMMIT;
