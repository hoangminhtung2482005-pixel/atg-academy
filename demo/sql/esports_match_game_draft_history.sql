-- Esports Match Game Draft History
-- Safe to rerun on the existing Spring Boot database.
-- This script documents the schema even though the app currently uses spring.jpa.hibernate.ddl-auto=update.

CREATE TABLE IF NOT EXISTS esports_match_games (
    id BIGINT NOT NULL AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    game_number INT NOT NULL,
    blue_team_id BIGINT NOT NULL,
    red_team_id BIGINT NOT NULL,
    winner_team_id BIGINT NULL,
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
    INDEX idx_esports_match_games_match_id (match_id),
    INDEX idx_esports_match_games_blue_team_id (blue_team_id),
    INDEX idx_esports_match_games_red_team_id (red_team_id),
    INDEX idx_esports_match_games_winner_team_id (winner_team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
