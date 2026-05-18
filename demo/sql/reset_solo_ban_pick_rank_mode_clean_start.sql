-- Reset legacy Solo Ban/Pick data and prepare clean split between
-- SIMULATION (legacy solo) and RANKED (new rank mode).
-- Safe intent:
--   - verifies current database is aov_tactics
--   - preserves users, heroes, hero ban_pick_score, player card fields,
--     and rating/admin configuration
--   - clears legacy Solo Ban/Pick rank/history/room data only

DROP PROCEDURE IF EXISTS reset_solo_ban_pick_rank_mode_clean_start;
DELIMITER $$
CREATE PROCEDURE reset_solo_ban_pick_rank_mode_clean_start()
BEGIN
    IF DATABASE() <> 'aov_tactics' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Abort reset: expected database aov_tactics.';
    END IF;

    SET @room_mode_column_exists = (
        SELECT COUNT(*)
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'ban_pick_rooms'
          AND COLUMN_NAME = 'mode'
    );

    IF @room_mode_column_exists = 0 THEN
        ALTER TABLE ban_pick_rooms
            ADD COLUMN mode VARCHAR(16) NOT NULL DEFAULT 'SIMULATION' AFTER room_code;
    END IF;

    UPDATE ban_pick_rooms
    SET mode = 'SIMULATION'
    WHERE mode IS NULL OR mode NOT IN ('SIMULATION', 'RANKED');

    SET @history_mode_column_exists = (
        SELECT COUNT(*)
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'draft_histories'
          AND COLUMN_NAME = 'mode'
    );

    IF @history_mode_column_exists = 0 THEN
        ALTER TABLE draft_histories
            ADD COLUMN mode VARCHAR(16) NOT NULL DEFAULT 'SIMULATION' AFTER room_code;
    END IF;

    UPDATE draft_histories
    SET mode = 'SIMULATION'
    WHERE mode IS NULL OR mode NOT IN ('SIMULATION', 'RANKED');

    SELECT 'before' AS phase, 'player_stats' AS table_name, COUNT(*) AS row_count FROM player_stats
    UNION ALL
    SELECT 'before', 'draft_histories', COUNT(*) FROM draft_histories
    UNION ALL
    SELECT 'before', 'ban_pick_rank_resets', COUNT(*) FROM ban_pick_rank_resets
    UNION ALL
    SELECT 'before', 'ban_pick_rooms', COUNT(*) FROM ban_pick_rooms
    UNION ALL
    SELECT 'before', 'ban_pick_room_participants', COUNT(*) FROM ban_pick_room_participants
    UNION ALL
    SELECT 'before', 'ban_pick_actions', COUNT(*) FROM ban_pick_actions
    UNION ALL
    SELECT 'before', 'users_cooldown_not_null', COUNT(*) FROM users WHERE ban_pick_cooldown_until IS NOT NULL
    UNION ALL
    SELECT 'before', 'users_cooldown_active', COUNT(*) FROM users WHERE ban_pick_cooldown_until IS NOT NULL AND ban_pick_cooldown_until > NOW();

    START TRANSACTION;

    DELETE FROM ban_pick_actions;
    DELETE FROM ban_pick_room_participants;
    DELETE FROM ban_pick_rooms;
    DELETE FROM draft_histories;
    DELETE FROM player_stats;
    DELETE FROM ban_pick_rank_resets;
    UPDATE users
    SET ban_pick_cooldown_until = NULL
    WHERE ban_pick_cooldown_until IS NOT NULL;

    COMMIT;

    SELECT 'after' AS phase, 'player_stats' AS table_name, COUNT(*) AS row_count FROM player_stats
    UNION ALL
    SELECT 'after', 'draft_histories', COUNT(*) FROM draft_histories
    UNION ALL
    SELECT 'after', 'ban_pick_rank_resets', COUNT(*) FROM ban_pick_rank_resets
    UNION ALL
    SELECT 'after', 'ban_pick_rooms', COUNT(*) FROM ban_pick_rooms
    UNION ALL
    SELECT 'after', 'ban_pick_room_participants', COUNT(*) FROM ban_pick_room_participants
    UNION ALL
    SELECT 'after', 'ban_pick_actions', COUNT(*) FROM ban_pick_actions
    UNION ALL
    SELECT 'after', 'users_cooldown_not_null', COUNT(*) FROM users WHERE ban_pick_cooldown_until IS NOT NULL
    UNION ALL
    SELECT 'after', 'users_cooldown_active', COUNT(*) FROM users WHERE ban_pick_cooldown_until IS NOT NULL AND ban_pick_cooldown_until > NOW();
END $$
DELIMITER ;

CALL reset_solo_ban_pick_rank_mode_clean_start();
DROP PROCEDURE reset_solo_ban_pick_rank_mode_clean_start;
