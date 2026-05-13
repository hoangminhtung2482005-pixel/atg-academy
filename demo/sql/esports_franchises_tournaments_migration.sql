-- WARNING:
-- Review on staging and take a backup before applying on a real database.
-- This script creates franchise/tournament catalog tables and an optional
-- tournament link on esports_matches.
-- It does NOT delete existing esports data.

CREATE TABLE IF NOT EXISTS esports_franchises (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    tier_level VARCHAR(10) NOT NULL,
    region VARCHAR(255) NULL,
    description TEXT NULL,
    logo_url VARCHAR(500) NULL,
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_esports_franchises_code (code),
    KEY idx_esports_franchises_active_order (active, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS esports_tournaments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    franchise_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    season_year INT NULL,
    split_name VARCHAR(100) NULL,
    tier_level VARCHAR(10) NULL,
    aer_tier INT NOT NULL DEFAULT 1,
    start_date DATE NULL,
    end_date DATE NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'UPCOMING',
    description TEXT NULL,
    logo_url VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_esports_tournaments_slug (slug),
    KEY idx_esports_tournaments_franchise_id (franchise_id),
    CONSTRAINT fk_esports_tournaments_franchise
        FOREIGN KEY (franchise_id) REFERENCES esports_franchises (id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS esports_tournament_teams (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tournament_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    group_name VARCHAR(100) NULL,
    seed_number INT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    note TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_esports_tournament_team (tournament_id, team_id),
    KEY idx_esports_tournament_teams_tournament (tournament_id),
    KEY idx_esports_tournament_teams_team (team_id),
    CONSTRAINT fk_esports_tournament_teams_tournament
        FOREIGN KEY (tournament_id) REFERENCES esports_tournaments (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_esports_tournament_teams_team
        FOREIGN KEY (team_id) REFERENCES esports_teams (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Optional link from esports_matches -> esports_tournaments.
-- Legacy rows can keep tournament_id = NULL and continue to fall back to tier.
SET @has_matches_tournament_column := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'esports_matches'
      AND COLUMN_NAME = 'tournament_id'
);
SET @sql := IF(
    @has_matches_tournament_column = 0,
    'ALTER TABLE esports_matches ADD COLUMN tournament_id BIGINT NULL',
    'SELECT ''esports_matches.tournament_id already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_matches_tournament_index := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'esports_matches'
      AND COLUMN_NAME = 'tournament_id'
);
SET @sql := IF(
    @has_matches_tournament_index = 0,
    'ALTER TABLE esports_matches ADD INDEX idx_esports_matches_tournament_id (tournament_id)',
    'SELECT ''esports_matches.tournament_id index already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_matches_tournament_fk := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'esports_matches'
      AND COLUMN_NAME = 'tournament_id'
      AND REFERENCED_TABLE_NAME = 'esports_tournaments'
      AND REFERENCED_COLUMN_NAME = 'id'
);
SET @sql := IF(
    @has_matches_tournament_fk = 0,
    'ALTER TABLE esports_matches ADD CONSTRAINT fk_esports_matches_tournament FOREIGN KEY (tournament_id) REFERENCES esports_tournaments (id) ON DELETE SET NULL',
    'SELECT ''esports_matches.tournament_id foreign key already exists'''
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE esports_franchises
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

ALTER TABLE esports_tournaments
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

ALTER TABLE esports_tournament_teams
    MODIFY COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);

INSERT INTO esports_franchises (
    code,
    name,
    tier_level,
    region,
    description,
    logo_url,
    display_order,
    active
) VALUES
    ('RPL', 'RoV Pro League', 'T1', 'Thailand', 'RoV Pro League', '/images/leagues/RPL_logo.png', 10, TRUE),
    ('AOG', 'Arena Of Glory', 'T1', 'Vietnam', 'Arena Of Glory', '/images/leagues/AOG_logo.png', 20, TRUE),
    ('GCS', 'Garena Challenger Series', 'T1', 'Taiwan/Hong Kong/Macau', 'Garena Challenger Series', '/images/leagues/GCS_logo.png', 30, TRUE),
    ('APL', 'AoV Premier League', 'T0', 'International', 'AoV Premier League', NULL, 40, TRUE),
    ('AIC', 'AoV International Championship', 'T0', 'International', 'AoV International Championship', NULL, 50, TRUE)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    tier_level = VALUES(tier_level),
    region = VALUES(region),
    description = VALUES(description),
    logo_url = VALUES(logo_url),
    display_order = VALUES(display_order),
    active = VALUES(active);

INSERT INTO esports_tournaments (
    franchise_id,
    name,
    slug,
    season_year,
    split_name,
    tier_level,
    aer_tier,
    status,
    description
) VALUES
    ((SELECT id FROM esports_franchises WHERE code = 'AOG'), 'AOG Spring 2026', 'aog-spring-2026', 2026, 'Spring', 'T1', 1, 'UPCOMING', 'Seed tournament'),
    ((SELECT id FROM esports_franchises WHERE code = 'AOG'), 'AOG Winter 2026', 'aog-winter-2026', 2026, 'Winter', 'T1', 1, 'UPCOMING', 'Seed tournament'),
    ((SELECT id FROM esports_franchises WHERE code = 'RPL'), 'RPL Summer 2026', 'rpl-summer-2026', 2026, 'Summer', 'T1', 1, 'UPCOMING', 'Seed tournament'),
    ((SELECT id FROM esports_franchises WHERE code = 'GCS'), 'GCS Spring 2026', 'gcs-spring-2026', 2026, 'Spring', 'T1', 1, 'UPCOMING', 'Seed tournament')
ON DUPLICATE KEY UPDATE
    franchise_id = VALUES(franchise_id),
    name = VALUES(name),
    season_year = VALUES(season_year),
    split_name = VALUES(split_name),
    tier_level = VALUES(tier_level),
    aer_tier = VALUES(aer_tier),
    status = VALUES(status),
    description = VALUES(description);

UPDATE esports_tournaments
SET aer_tier = 1
WHERE aer_tier IS NULL
   OR aer_tier <= 0;

ALTER TABLE esports_tournaments
    MODIFY COLUMN aer_tier INT NOT NULL DEFAULT 1;

-- Starter tournament rosters. Inserts are skipped when the source team does not exist.
INSERT IGNORE INTO esports_tournament_teams (tournament_id, team_id, seed_number, status)
SELECT tournament.id, team.id, 1, 'ACTIVE'
FROM esports_tournaments tournament
JOIN esports_teams team ON team.team_code = 'SGP'
WHERE tournament.slug = 'aog-spring-2026';

INSERT IGNORE INTO esports_tournament_teams (tournament_id, team_id, seed_number, status)
SELECT tournament.id, team.id, 2, 'ACTIVE'
FROM esports_tournaments tournament
JOIN esports_teams team ON team.team_code = 'FPT'
WHERE tournament.slug = 'aog-spring-2026';

INSERT IGNORE INTO esports_tournament_teams (tournament_id, team_id, seed_number, status)
SELECT tournament.id, team.id, 3, 'ACTIVE'
FROM esports_tournaments tournament
JOIN esports_teams team ON team.team_code = '1S'
WHERE tournament.slug = 'aog-spring-2026';

INSERT IGNORE INTO esports_tournament_teams (tournament_id, team_id, seed_number, status)
SELECT tournament.id, team.id, 4, 'ACTIVE'
FROM esports_tournaments tournament
JOIN esports_teams team ON team.team_code = 'BOX'
WHERE tournament.slug = 'aog-spring-2026';

INSERT IGNORE INTO esports_tournament_teams (tournament_id, team_id, seed_number, status)
SELECT tournament.id, team.id, 1, 'ACTIVE'
FROM esports_tournaments tournament
JOIN esports_teams team ON team.team_code = 'SGP'
WHERE tournament.slug = 'aog-winter-2026';

INSERT IGNORE INTO esports_tournament_teams (tournament_id, team_id, seed_number, status)
SELECT tournament.id, team.id, 2, 'ACTIVE'
FROM esports_tournaments tournament
JOIN esports_teams team ON team.team_code = 'GAM'
WHERE tournament.slug = 'aog-winter-2026';

INSERT IGNORE INTO esports_tournament_teams (tournament_id, team_id, seed_number, status)
SELECT tournament.id, team.id, 3, 'ACTIVE'
FROM esports_tournaments tournament
JOIN esports_teams team ON team.team_code = 'TS'
WHERE tournament.slug = 'aog-winter-2026';

INSERT IGNORE INTO esports_tournament_teams (tournament_id, team_id, seed_number, status)
SELECT tournament.id, team.id, 4, 'ACTIVE'
FROM esports_tournaments tournament
JOIN esports_teams team ON team.team_code = 'SPN'
WHERE tournament.slug = 'aog-winter-2026';

INSERT IGNORE INTO esports_tournament_teams (tournament_id, team_id, seed_number, status)
SELECT tournament.id, team.id, 1, 'ACTIVE'
FROM esports_tournaments tournament
JOIN esports_teams team ON team.team_code = 'FS'
WHERE tournament.slug = 'rpl-summer-2026';

INSERT IGNORE INTO esports_tournament_teams (tournament_id, team_id, seed_number, status)
SELECT tournament.id, team.id, 2, 'ACTIVE'
FROM esports_tournaments tournament
JOIN esports_teams team ON team.team_code = 'BAC'
WHERE tournament.slug = 'rpl-summer-2026';

INSERT IGNORE INTO esports_tournament_teams (tournament_id, team_id, seed_number, status)
SELECT tournament.id, team.id, 3, 'ACTIVE'
FROM esports_tournaments tournament
JOIN esports_teams team ON team.team_code = 'BRU'
WHERE tournament.slug = 'rpl-summer-2026';

INSERT IGNORE INTO esports_tournament_teams (tournament_id, team_id, seed_number, status)
SELECT tournament.id, team.id, 4, 'ACTIVE'
FROM esports_tournaments tournament
JOIN esports_teams team ON team.team_code = 'SLX'
WHERE tournament.slug = 'rpl-summer-2026';

INSERT IGNORE INTO esports_tournament_teams (tournament_id, team_id, seed_number, status)
SELECT tournament.id, team.id, 1, 'ACTIVE'
FROM esports_tournaments tournament
JOIN esports_teams team ON team.team_code = 'FW'
WHERE tournament.slug = 'gcs-spring-2026';

INSERT IGNORE INTO esports_tournament_teams (tournament_id, team_id, seed_number, status)
SELECT tournament.id, team.id, 2, 'ACTIVE'
FROM esports_tournaments tournament
JOIN esports_teams team ON team.team_code = 'HKA'
WHERE tournament.slug = 'gcs-spring-2026';

INSERT IGNORE INTO esports_tournament_teams (tournament_id, team_id, seed_number, status)
SELECT tournament.id, team.id, 3, 'ACTIVE'
FROM esports_tournaments tournament
JOIN esports_teams team ON team.team_code = 'ONE'
WHERE tournament.slug = 'gcs-spring-2026';

INSERT IGNORE INTO esports_tournament_teams (tournament_id, team_id, seed_number, status)
SELECT tournament.id, team.id, 4, 'ACTIVE'
FROM esports_tournaments tournament
JOIN esports_teams team ON team.team_code = 'DCG'
WHERE tournament.slug = 'gcs-spring-2026';

-- Verification queries
SELECT id, code, name, tier_level, active
FROM esports_franchises
ORDER BY display_order ASC, id ASC;

SELECT tournament.id,
       franchise.code AS franchise_code,
       tournament.name,
       tournament.slug,
       tournament.season_year,
       tournament.split_name,
       tournament.tier_level,
       tournament.aer_tier,
       tournament.status
FROM esports_tournaments tournament
JOIN esports_franchises franchise ON franchise.id = tournament.franchise_id
ORDER BY franchise.display_order ASC, tournament.season_year DESC, tournament.name ASC;

SELECT tournament.slug,
       COUNT(mapping.id) AS team_count
FROM esports_tournaments tournament
LEFT JOIN esports_tournament_teams mapping ON mapping.tournament_id = tournament.id
GROUP BY tournament.id, tournament.slug
ORDER BY tournament.slug ASC;

SELECT COUNT(*) AS matches_without_tournament_link
FROM esports_matches
WHERE tournament_id IS NULL;
