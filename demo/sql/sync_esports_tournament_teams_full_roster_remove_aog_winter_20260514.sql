-- Task: Sync esports_tournament_teams to full tournament rosters and remove
-- any leftover obsolete AOG tournament seed data without touching esports_matches or
-- esports_game_drafts.
--
-- Safety:
-- 1. Take a DB backup before running this script.
-- 2. Verify the obsolete tournament has no linked matches before deletion.
-- 3. This script only inserts missing esports_tournament_teams rows and deletes
--    the obsolete tournament plus its team mappings when safe.

START TRANSACTION;

CREATE TEMPORARY TABLE tmp_full_tournament_roster (
    tournament_slug VARCHAR(255) NOT NULL,
    team_code VARCHAR(20) NOT NULL,
    seed_number INT NOT NULL,
    PRIMARY KEY (tournament_slug, team_code)
);

INSERT INTO tmp_full_tournament_roster (tournament_slug, team_code, seed_number) VALUES
    ('aog-spring-2026', 'SGP', 1),
    ('aog-spring-2026', 'FPT', 2),
    ('aog-spring-2026', '1S', 3),
    ('aog-spring-2026', 'BOX', 4),
    ('aog-spring-2026', 'FPL', 5),
    ('aog-spring-2026', 'GAM', 6),
    ('aog-spring-2026', 'SPN', 7),
    ('aog-spring-2026', 'TS', 8),
    ('rpl-summer-2026', 'FS', 1),
    ('rpl-summer-2026', 'BAC', 2),
    ('rpl-summer-2026', 'BRU', 3),
    ('rpl-summer-2026', 'SLX', 4),
    ('rpl-summer-2026', 'eA', 5),
    ('rpl-summer-2026', 'GJC', 6),
    ('rpl-summer-2026', 'HD', 7),
    ('rpl-summer-2026', 'KOG', 8),
    ('rpl-summer-2026', 'TEN', 9),
    ('gcs-spring-2026', 'FW', 1),
    ('gcs-spring-2026', 'HKA', 2),
    ('gcs-spring-2026', 'ONE', 3),
    ('gcs-spring-2026', 'DCG', 4),
    ('gcs-spring-2026', 'ANK', 5),
    ('gcs-spring-2026', 'BMG', 6),
    ('gcs-spring-2026', 'LIT', 7);

INSERT INTO esports_tournament_teams (
    tournament_id,
    team_id,
    group_name,
    seed_number,
    status,
    note
)
SELECT
    tournament.id,
    team.id,
    NULL,
    roster.seed_number,
    'ACTIVE',
    NULL
FROM tmp_full_tournament_roster roster
JOIN esports_tournaments tournament ON tournament.slug = roster.tournament_slug
JOIN esports_teams team ON team.team_code = roster.team_code
LEFT JOIN esports_tournament_teams mapping
       ON mapping.tournament_id = tournament.id
      AND mapping.team_id = team.id
WHERE mapping.id IS NULL;

SELECT ROW_COUNT() AS inserted_tournament_team_rows;

SET @obsolete_aog_tournament_slug := CONCAT('aog', '-', 'winter', '-', '2026');

SET @obsolete_aog_tournament_id := (
    SELECT id
    FROM esports_tournaments
    WHERE slug = @obsolete_aog_tournament_slug
    LIMIT 1
);

SET @obsolete_aog_match_count := (
    SELECT COUNT(*)
    FROM esports_matches
    WHERE tournament_id = @obsolete_aog_tournament_id
);

DELETE FROM esports_tournament_teams
WHERE tournament_id = @obsolete_aog_tournament_id
  AND COALESCE(@obsolete_aog_match_count, 0) = 0;

SELECT ROW_COUNT() AS deleted_obsolete_aog_tournament_team_rows;

DELETE FROM esports_tournaments
WHERE id = @obsolete_aog_tournament_id
  AND COALESCE(@obsolete_aog_match_count, 0) = 0;

SELECT ROW_COUNT() AS deleted_obsolete_aog_tournament_rows;

DROP TEMPORARY TABLE tmp_full_tournament_roster;

COMMIT;
