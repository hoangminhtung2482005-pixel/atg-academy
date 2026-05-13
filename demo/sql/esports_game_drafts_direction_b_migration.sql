-- Esports Data Direction B migration
-- Keep esports_matches as the parent series table.
-- Do NOT drop legacy tables automatically in this script.

CREATE TABLE IF NOT EXISTS esports_game_drafts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    game_number INT NOT NULL,
    blue_team_id BIGINT NOT NULL,
    red_team_id BIGINT NOT NULL,
    winner_team_id BIGINT NULL,
    duration_seconds INT NULL,
    draft_format_code VARCHAR(50) NOT NULL DEFAULT 'AOV_STANDARD_18',
    source VARCHAR(100) NULL,
    blue_ban_1_hero_id BIGINT NULL,
    blue_ban_2_hero_id BIGINT NULL,
    blue_ban_3_hero_id BIGINT NULL,
    blue_ban_4_hero_id BIGINT NULL,
    blue_ban_5_hero_id BIGINT NULL,
    red_ban_1_hero_id BIGINT NULL,
    red_ban_2_hero_id BIGINT NULL,
    red_ban_3_hero_id BIGINT NULL,
    red_ban_4_hero_id BIGINT NULL,
    red_ban_5_hero_id BIGINT NULL,
    blue_dsl_hero_id BIGINT NULL,
    blue_jgl_hero_id BIGINT NULL,
    blue_mid_hero_id BIGINT NULL,
    blue_adl_hero_id BIGINT NULL,
    blue_sup_hero_id BIGINT NULL,
    red_dsl_hero_id BIGINT NULL,
    red_jgl_hero_id BIGINT NULL,
    red_mid_hero_id BIGINT NULL,
    red_adl_hero_id BIGINT NULL,
    red_sup_hero_id BIGINT NULL,
    raw_draft_json LONGTEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_esports_game_drafts_match_game UNIQUE (match_id, game_number),
    CONSTRAINT fk_esports_game_drafts_match
        FOREIGN KEY (match_id) REFERENCES esports_matches (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_esports_game_drafts_blue_team
        FOREIGN KEY (blue_team_id) REFERENCES esports_teams (id),
    CONSTRAINT fk_esports_game_drafts_red_team
        FOREIGN KEY (red_team_id) REFERENCES esports_teams (id),
    CONSTRAINT fk_esports_game_drafts_winner_team
        FOREIGN KEY (winner_team_id) REFERENCES esports_teams (id),
    INDEX idx_esports_game_drafts_match_id (match_id),
    INDEX idx_esports_game_drafts_blue_team_id (blue_team_id),
    INDEX idx_esports_game_drafts_red_team_id (red_team_id),
    INDEX idx_esports_game_drafts_winner_team_id (winner_team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Best-effort migration from the legacy normalized model.
-- Limitation:
-- 1. Legacy 18-phase data usually contains 4 bans per side. Slot 5 stays NULL unless old data had extra bans.
-- 2. Draft order is not preserved in the new flat table; only final per-slot bans and final lineups are migrated.
-- 3. If legacy rows contain duplicate or incomplete data, this script keeps best-effort values and leaves the row incomplete.

INSERT INTO esports_game_drafts (
    match_id,
    game_number,
    blue_team_id,
    red_team_id,
    winner_team_id,
    duration_seconds,
    draft_format_code,
    source,
    blue_ban_1_hero_id,
    blue_ban_2_hero_id,
    blue_ban_3_hero_id,
    blue_ban_4_hero_id,
    blue_ban_5_hero_id,
    red_ban_1_hero_id,
    red_ban_2_hero_id,
    red_ban_3_hero_id,
    red_ban_4_hero_id,
    red_ban_5_hero_id,
    blue_dsl_hero_id,
    blue_jgl_hero_id,
    blue_mid_hero_id,
    blue_adl_hero_id,
    blue_sup_hero_id,
    red_dsl_hero_id,
    red_jgl_hero_id,
    red_mid_hero_id,
    red_adl_hero_id,
    red_sup_hero_id,
    raw_draft_json,
    created_at,
    updated_at
)
WITH ordered_bans AS (
    SELECT
        action.game_id,
        action.team_side,
        action.hero_id,
        ROW_NUMBER() OVER (
            PARTITION BY action.game_id, action.team_side
            ORDER BY action.step_number ASC, action.id ASC
        ) AS slot_number
    FROM esports_match_draft_actions action
    WHERE action.action_type = 'BAN'
),
pivot_bans AS (
    SELECT
        game_id,
        MAX(CASE WHEN team_side = 'BLUE' AND slot_number = 1 THEN hero_id END) AS blue_ban_1_hero_id,
        MAX(CASE WHEN team_side = 'BLUE' AND slot_number = 2 THEN hero_id END) AS blue_ban_2_hero_id,
        MAX(CASE WHEN team_side = 'BLUE' AND slot_number = 3 THEN hero_id END) AS blue_ban_3_hero_id,
        MAX(CASE WHEN team_side = 'BLUE' AND slot_number = 4 THEN hero_id END) AS blue_ban_4_hero_id,
        MAX(CASE WHEN team_side = 'BLUE' AND slot_number = 5 THEN hero_id END) AS blue_ban_5_hero_id,
        MAX(CASE WHEN team_side = 'RED' AND slot_number = 1 THEN hero_id END) AS red_ban_1_hero_id,
        MAX(CASE WHEN team_side = 'RED' AND slot_number = 2 THEN hero_id END) AS red_ban_2_hero_id,
        MAX(CASE WHEN team_side = 'RED' AND slot_number = 3 THEN hero_id END) AS red_ban_3_hero_id,
        MAX(CASE WHEN team_side = 'RED' AND slot_number = 4 THEN hero_id END) AS red_ban_4_hero_id,
        MAX(CASE WHEN team_side = 'RED' AND slot_number = 5 THEN hero_id END) AS red_ban_5_hero_id
    FROM ordered_bans
    GROUP BY game_id
),
pivot_lineups AS (
    SELECT
        lineup.game_id,
        MAX(CASE WHEN lineup.team_side = 'BLUE' AND lineup.lane_role = 'DSL' THEN lineup.hero_id END) AS blue_dsl_hero_id,
        MAX(CASE WHEN lineup.team_side = 'BLUE' AND lineup.lane_role = 'JGL' THEN lineup.hero_id END) AS blue_jgl_hero_id,
        MAX(CASE WHEN lineup.team_side = 'BLUE' AND lineup.lane_role = 'MID' THEN lineup.hero_id END) AS blue_mid_hero_id,
        MAX(CASE WHEN lineup.team_side = 'BLUE' AND lineup.lane_role = 'ADL' THEN lineup.hero_id END) AS blue_adl_hero_id,
        MAX(CASE WHEN lineup.team_side = 'BLUE' AND lineup.lane_role = 'SUP' THEN lineup.hero_id END) AS blue_sup_hero_id,
        MAX(CASE WHEN lineup.team_side = 'RED' AND lineup.lane_role = 'DSL' THEN lineup.hero_id END) AS red_dsl_hero_id,
        MAX(CASE WHEN lineup.team_side = 'RED' AND lineup.lane_role = 'JGL' THEN lineup.hero_id END) AS red_jgl_hero_id,
        MAX(CASE WHEN lineup.team_side = 'RED' AND lineup.lane_role = 'MID' THEN lineup.hero_id END) AS red_mid_hero_id,
        MAX(CASE WHEN lineup.team_side = 'RED' AND lineup.lane_role = 'ADL' THEN lineup.hero_id END) AS red_adl_hero_id,
        MAX(CASE WHEN lineup.team_side = 'RED' AND lineup.lane_role = 'SUP' THEN lineup.hero_id END) AS red_sup_hero_id
    FROM esports_match_game_lineups lineup
    GROUP BY lineup.game_id
)
SELECT
    game.match_id,
    game.game_number,
    game.blue_team_id,
    game.red_team_id,
    game.winner_team_id,
    game.duration_seconds,
    COALESCE(format.code, 'AOV_STANDARD_18') AS draft_format_code,
    'legacy_migration' AS source,
    bans.blue_ban_1_hero_id,
    bans.blue_ban_2_hero_id,
    bans.blue_ban_3_hero_id,
    bans.blue_ban_4_hero_id,
    bans.blue_ban_5_hero_id,
    bans.red_ban_1_hero_id,
    bans.red_ban_2_hero_id,
    bans.red_ban_3_hero_id,
    bans.red_ban_4_hero_id,
    bans.red_ban_5_hero_id,
    lineups.blue_dsl_hero_id,
    lineups.blue_jgl_hero_id,
    lineups.blue_mid_hero_id,
    lineups.blue_adl_hero_id,
    lineups.blue_sup_hero_id,
    lineups.red_dsl_hero_id,
    lineups.red_jgl_hero_id,
    lineups.red_mid_hero_id,
    lineups.red_adl_hero_id,
    lineups.red_sup_hero_id,
    CONCAT(
        '{"legacyGameId":',
        game.id,
        ',"migrationSource":"esports_match_*"}'
    ) AS raw_draft_json,
    COALESCE(game.created_at, CURRENT_TIMESTAMP(6)),
    COALESCE(game.updated_at, game.created_at, CURRENT_TIMESTAMP(6))
FROM esports_match_games game
LEFT JOIN esports_draft_formats format
    ON format.id = game.draft_format_id
LEFT JOIN pivot_bans bans
    ON bans.game_id = game.id
LEFT JOIN pivot_lineups lineups
    ON lineups.game_id = game.id
WHERE NOT EXISTS (
    SELECT 1
    FROM esports_game_drafts existing
    WHERE existing.match_id = game.match_id
      AND existing.game_number = game.game_number
);

-- Verification queries
SELECT COUNT(*) AS legacy_game_count FROM esports_match_games;
SELECT COUNT(*) AS legacy_draft_action_count FROM esports_match_draft_actions;
SELECT COUNT(*) AS legacy_lineup_count FROM esports_match_game_lineups;
SELECT COUNT(*) AS new_game_draft_count FROM esports_game_drafts;

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
      TABLE_NAME LIKE 'esports_match_%'
      OR TABLE_NAME = 'esports_game_drafts'
  );

-- Deprecated-table cleanup proposal. Run only after full backup and runtime verification.
-- DROP TABLE esports_match_game_lineups;
-- DROP TABLE esports_match_draft_actions;
-- DROP TABLE esports_match_games;
