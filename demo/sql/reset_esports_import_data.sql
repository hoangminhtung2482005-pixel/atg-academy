-- Safe manual reset for imported esports series data.
-- Scope:
--   - DELETE esports_game_drafts
--   - DELETE esports_matches
--   - KEEP player_stats because it belongs to Ban/Pick leaderboard, not derived from esports_matches
--   - KEEP teams/tournaments/heroes/users/content tables
--   - RESET only derived ranking fields on esports_teams after imported matches are removed

SELECT COUNT(*) AS before_game_drafts FROM esports_game_drafts;
SELECT COUNT(*) AS before_matches FROM esports_matches;
SELECT COUNT(*) AS before_player_stats FROM player_stats;
SELECT COUNT(*) AS before_teams FROM esports_teams;
SELECT COUNT(*) AS before_tournaments FROM esports_tournaments;
SELECT COUNT(*) AS before_heroes FROM heroes;

START TRANSACTION;

DELETE FROM esports_game_drafts;
DELETE FROM esports_matches;

UPDATE esports_teams
SET score = 1200,
    match_wins = 0,
    match_losses = 0,
    game_wins = 0,
    game_losses = 0;

COMMIT;

SELECT COUNT(*) AS remaining_game_drafts FROM esports_game_drafts;
SELECT COUNT(*) AS remaining_matches FROM esports_matches;
SELECT COUNT(*) AS remaining_player_stats FROM player_stats;
SELECT COUNT(*) AS remaining_teams FROM esports_teams;
SELECT COUNT(*) AS remaining_tournaments FROM esports_tournaments;
SELECT COUNT(*) AS remaining_heroes FROM heroes;
