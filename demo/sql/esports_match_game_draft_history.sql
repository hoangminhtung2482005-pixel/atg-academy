-- Esports Match Game Draft History + Hard Phase Rule + Final Lineup
-- Safe to rerun on the existing Spring Boot database.
-- This script documents the schema even though the app currently uses spring.jpa.hibernate.ddl-auto=update.

SET @schema_name = DATABASE();

CREATE TABLE IF NOT EXISTS esports_draft_formats (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(160) NOT NULL,
    total_steps INT NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_esports_draft_formats_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS esports_draft_phase_rules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    format_id BIGINT NOT NULL,
    step_number INT NOT NULL,
    team_side VARCHAR(10) NOT NULL,
    action_type VARCHAR(10) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_esports_draft_phase_rules_format_step_number UNIQUE (format_id, step_number),
    CONSTRAINT fk_esports_draft_phase_rules_format
        FOREIGN KEY (format_id) REFERENCES esports_draft_formats (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_esports_draft_phase_rules_step_number
        CHECK (step_number BETWEEN 1 AND 18),
    CONSTRAINT chk_esports_draft_phase_rules_team_side
        CHECK (team_side IN ('BLUE', 'RED')),
    CONSTRAINT chk_esports_draft_phase_rules_action_type
        CHECK (action_type IN ('BAN', 'PICK')),
    INDEX idx_esports_draft_phase_rules_format_id (format_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO esports_draft_formats (
    code,
    name,
    total_steps,
    is_default,
    active,
    created_at,
    updated_at
) VALUES (
    'AOV_STANDARD_18',
    'AOV Standard 18 Phase',
    18,
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    total_steps = VALUES(total_steps),
    is_default = VALUES(is_default),
    active = VALUES(active),
    updated_at = CURRENT_TIMESTAMP(6);

SET @default_draft_format_id = (
    SELECT id
    FROM esports_draft_formats
    WHERE code = 'AOV_STANDARD_18'
    LIMIT 1
);

DELETE FROM esports_draft_phase_rules
WHERE format_id = @default_draft_format_id
  AND step_number NOT IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18);

INSERT INTO esports_draft_phase_rules (
    format_id,
    step_number,
    team_side,
    action_type,
    created_at
) VALUES
    (@default_draft_format_id, 1, 'BLUE', 'BAN', CURRENT_TIMESTAMP(6)),
    (@default_draft_format_id, 2, 'RED', 'BAN', CURRENT_TIMESTAMP(6)),
    (@default_draft_format_id, 3, 'BLUE', 'BAN', CURRENT_TIMESTAMP(6)),
    (@default_draft_format_id, 4, 'RED', 'BAN', CURRENT_TIMESTAMP(6)),
    (@default_draft_format_id, 5, 'BLUE', 'PICK', CURRENT_TIMESTAMP(6)),
    (@default_draft_format_id, 6, 'RED', 'PICK', CURRENT_TIMESTAMP(6)),
    (@default_draft_format_id, 7, 'RED', 'PICK', CURRENT_TIMESTAMP(6)),
    (@default_draft_format_id, 8, 'BLUE', 'PICK', CURRENT_TIMESTAMP(6)),
    (@default_draft_format_id, 9, 'BLUE', 'PICK', CURRENT_TIMESTAMP(6)),
    (@default_draft_format_id, 10, 'RED', 'PICK', CURRENT_TIMESTAMP(6)),
    (@default_draft_format_id, 11, 'RED', 'BAN', CURRENT_TIMESTAMP(6)),
    (@default_draft_format_id, 12, 'BLUE', 'BAN', CURRENT_TIMESTAMP(6)),
    (@default_draft_format_id, 13, 'RED', 'BAN', CURRENT_TIMESTAMP(6)),
    (@default_draft_format_id, 14, 'BLUE', 'BAN', CURRENT_TIMESTAMP(6)),
    (@default_draft_format_id, 15, 'RED', 'PICK', CURRENT_TIMESTAMP(6)),
    (@default_draft_format_id, 16, 'BLUE', 'PICK', CURRENT_TIMESTAMP(6)),
    (@default_draft_format_id, 17, 'BLUE', 'PICK', CURRENT_TIMESTAMP(6)),
    (@default_draft_format_id, 18, 'RED', 'PICK', CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE
    team_side = VALUES(team_side),
    action_type = VALUES(action_type);

CREATE TABLE IF NOT EXISTS esports_match_games (
    id BIGINT NOT NULL AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    game_number INT NOT NULL,
    blue_team_id BIGINT NOT NULL,
    red_team_id BIGINT NOT NULL,
    winner_team_id BIGINT NULL,
    draft_format_id BIGINT NULL,
    duration_seconds INT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_esports_match_games_match_game_number UNIQUE (match_id, game_number),
    CONSTRAINT fk_esports_match_games_match
        FOREIGN KEY (match_id) REFERENCES esports_matches (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_esports_match_games_blue_team
        FOREIGN KEY (blue_team_id) REFERENCES esports_teams (id),
    CONSTRAINT fk_esports_match_games_red_team
        FOREIGN KEY (red_team_id) REFERENCES esports_teams (id),
    CONSTRAINT fk_esports_match_games_winner_team
        FOREIGN KEY (winner_team_id) REFERENCES esports_teams (id),
    CONSTRAINT fk_esports_match_games_draft_format
        FOREIGN KEY (draft_format_id) REFERENCES esports_draft_formats (id),
    INDEX idx_esports_match_games_match_id (match_id),
    INDEX idx_esports_match_games_blue_team_id (blue_team_id),
    INDEX idx_esports_match_games_red_team_id (red_team_id),
    INDEX idx_esports_match_games_winner_team_id (winner_team_id),
    INDEX idx_esports_match_games_draft_format_id (draft_format_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @has_draft_format_id = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'esports_match_games'
      AND column_name = 'draft_format_id'
);
SET @add_draft_format_column_sql = IF(
    @has_draft_format_id = 0,
    'ALTER TABLE esports_match_games ADD COLUMN draft_format_id BIGINT NULL AFTER winner_team_id',
    'SELECT 1'
);
PREPARE stmt FROM @add_draft_format_column_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE esports_match_games
SET draft_format_id = @default_draft_format_id
WHERE draft_format_id IS NULL;

SET @has_draft_format_fk = (
    SELECT COUNT(*)
    FROM information_schema.table_constraints
    WHERE table_schema = @schema_name
      AND table_name = 'esports_match_games'
      AND constraint_name = 'fk_esports_match_games_draft_format'
);
SET @add_draft_format_fk_sql = IF(
    @has_draft_format_fk = 0,
    'ALTER TABLE esports_match_games ADD CONSTRAINT fk_esports_match_games_draft_format FOREIGN KEY (draft_format_id) REFERENCES esports_draft_formats (id)',
    'SELECT 1'
);
PREPARE stmt FROM @add_draft_format_fk_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_draft_format_index = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'esports_match_games'
      AND index_name = 'idx_esports_match_games_draft_format_id'
);
SET @add_draft_format_index_sql = IF(
    @has_draft_format_index = 0,
    'CREATE INDEX idx_esports_match_games_draft_format_id ON esports_match_games (draft_format_id)',
    'SELECT 1'
);
PREPARE stmt FROM @add_draft_format_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS esports_match_draft_actions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    game_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    hero_id BIGINT NOT NULL,
    action_type VARCHAR(10) NOT NULL,
    step_number INT NOT NULL,
    team_side VARCHAR(10) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_esports_match_draft_actions_game_step_number UNIQUE (game_id, step_number),
    CONSTRAINT uk_esports_match_draft_actions_game_hero_id UNIQUE (game_id, hero_id),
    CONSTRAINT fk_esports_match_draft_actions_game
        FOREIGN KEY (game_id) REFERENCES esports_match_games (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_esports_match_draft_actions_team
        FOREIGN KEY (team_id) REFERENCES esports_teams (id),
    CONSTRAINT fk_esports_match_draft_actions_hero
        FOREIGN KEY (hero_id) REFERENCES heroes (id),
    CONSTRAINT chk_esports_match_draft_actions_action_type
        CHECK (action_type IN ('BAN', 'PICK')),
    CONSTRAINT chk_esports_match_draft_actions_team_side
        CHECK (team_side IN ('BLUE', 'RED')),
    INDEX idx_esports_match_draft_actions_game_step (game_id, step_number),
    INDEX idx_esports_match_draft_actions_team_id (team_id),
    INDEX idx_esports_match_draft_actions_hero_id (hero_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS esports_match_game_lineups (
    id BIGINT NOT NULL AUTO_INCREMENT,
    game_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    team_side VARCHAR(10) NOT NULL,
    position_number INT NOT NULL,
    lane_role VARCHAR(10) NOT NULL,
    hero_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NULL DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_esports_match_game_lineups_game_side_position UNIQUE (game_id, team_side, position_number),
    CONSTRAINT uk_esports_match_game_lineups_game_side_lane UNIQUE (game_id, team_side, lane_role),
    CONSTRAINT uk_esports_match_game_lineups_game_hero UNIQUE (game_id, hero_id),
    CONSTRAINT fk_esports_match_game_lineups_game
        FOREIGN KEY (game_id) REFERENCES esports_match_games (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_esports_match_game_lineups_team
        FOREIGN KEY (team_id) REFERENCES esports_teams (id),
    CONSTRAINT fk_esports_match_game_lineups_hero
        FOREIGN KEY (hero_id) REFERENCES heroes (id),
    CONSTRAINT chk_esports_match_game_lineups_team_side
        CHECK (team_side IN ('BLUE', 'RED')),
    CONSTRAINT chk_esports_match_game_lineups_position
        CHECK (position_number BETWEEN 1 AND 5),
    CONSTRAINT chk_esports_match_game_lineups_lane_role
        CHECK (lane_role IN ('DSL', 'JGL', 'MID', 'ADL', 'SUP')),
    INDEX idx_esports_match_game_lineups_game_id (game_id),
    INDEX idx_esports_match_game_lineups_team_id (team_id),
    INDEX idx_esports_match_game_lineups_hero_id (hero_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
