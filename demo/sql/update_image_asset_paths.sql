-- Image path migration for ATG Academy static asset reorganization
-- Generated during images folder refactor.

-- Hero avatars: preserve the existing filename and extension, change only the folder.
UPDATE heroes
SET avatar_url = REPLACE(avatar_url, '/images/', '/images/heroes/')
WHERE avatar_url LIKE '/images/%'
  AND avatar_url NOT LIKE '/images/heroes/%';

-- Team logos: preserve the existing filename and extension, change only the folder.
UPDATE esports_teams
SET logo_url = REPLACE(logo_url, '/images/', '/images/teams/')
WHERE logo_url LIKE '/images/%'
  AND logo_url NOT LIKE '/images/teams/%';

-- League logos are referenced from static assets. No persisted league-logo field was found in the Java entities.

-- Optional validation queries:
-- SELECT slug, avatar_url FROM heroes WHERE avatar_url LIKE '/images/%' AND avatar_url NOT LIKE '/images/heroes/%';
-- SELECT team_code, logo_url FROM esports_teams WHERE logo_url LIKE '/images/%' AND logo_url NOT LIKE '/images/teams/%';