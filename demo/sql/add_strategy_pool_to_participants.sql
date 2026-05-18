-- Migration: Add strategy_pool column to ban_pick_room_participants
-- Strategy pool stores per-participant hero priority list (comma-separated hero IDs).
-- Scoped to the current room only. Not a pick/ban lock.
-- Safe to run multiple times (uses IF NOT EXISTS pattern via ALTER IGNORE or check).

ALTER TABLE ban_pick_room_participants
    ADD COLUMN IF NOT EXISTS strategy_pool TEXT NULL COMMENT 'Comma-separated hero IDs for strategy pool priority. Per-participant, room-scoped only.';

-- Verify
SELECT COUNT(*) AS participant_count,
       SUM(CASE WHEN strategy_pool IS NOT NULL THEN 1 ELSE 0 END) AS with_pool
FROM ban_pick_room_participants;
