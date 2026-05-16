-- Persist per-match Solo Ban/Pick rating deltas so rolling-50 rebuilds can
-- replay the exact macro-adjusted win delta that was active when each draft
-- result was recorded.
-- Review on local MySQL 8+ and take a backup before execution.

START TRANSACTION;

SET @add_win_rating_delta := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'draft_histories'
          AND COLUMN_NAME = 'win_rating_delta'
    ),
    'SELECT ''draft_histories.win_rating_delta already exists''',
    'ALTER TABLE draft_histories ADD COLUMN win_rating_delta INT NOT NULL DEFAULT 30 AFTER result_recorded_at'
);
PREPARE add_win_rating_delta_stmt FROM @add_win_rating_delta;
EXECUTE add_win_rating_delta_stmt;
DEALLOCATE PREPARE add_win_rating_delta_stmt;

SET @add_loss_rating_delta := IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'draft_histories'
          AND COLUMN_NAME = 'loss_rating_delta'
    ),
    'SELECT ''draft_histories.loss_rating_delta already exists''',
    'ALTER TABLE draft_histories ADD COLUMN loss_rating_delta INT NOT NULL DEFAULT -20 AFTER win_rating_delta'
);
PREPARE add_loss_rating_delta_stmt FROM @add_loss_rating_delta;
EXECUTE add_loss_rating_delta_stmt;
DEALLOCATE PREPARE add_loss_rating_delta_stmt;

UPDATE draft_histories
SET win_rating_delta = COALESCE(win_rating_delta, 30),
    loss_rating_delta = COALESCE(loss_rating_delta, -20)
WHERE win_rating_delta IS NULL
   OR loss_rating_delta IS NULL;

COMMIT;
