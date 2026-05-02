-- ATG Academy Wiki Phase 1 migration notes for MySQL 8.0.
-- This is intentionally not a Flyway migration yet.
--
-- Important order:
-- 1. Add slug as NULLABLE so existing rows do not fail.
-- 2. Backfill slug from hero name.
-- 3. Resolve duplicate slugs while slug is still nullable/non-unique.
-- 4. Add the unique slug index after data is clean.
-- 5. Make slug NOT NULL last.
--
-- Run once in a maintenance window. If a column/index already exists in your DB,
-- skip that statement manually before rerunning the remaining steps.

-- 1) Add nullable columns first so existing rows do not break.
ALTER TABLE heroes
    ADD COLUMN slug VARCHAR(140) NULL AFTER name,
    ADD COLUMN title VARCHAR(120) NULL AFTER slug,
    ADD COLUMN portrait_url VARCHAR(255) NULL AFTER avatar_url,
    ADD COLUMN banner_url VARCHAR(255) NULL AFTER portrait_url,
    ADD COLUMN difficulty VARCHAR(30) NULL AFTER hero_class,
    ADD COLUMN description TEXT NULL AFTER difficulty,
    ADD COLUMN lore TEXT NULL AFTER description;

-- 2) Backfill slug. MySQL has no built-in unaccent, so this helper covers common Vietnamese marks.
DROP FUNCTION IF EXISTS atg_slugify;
DELIMITER //
CREATE FUNCTION atg_slugify(input TEXT) RETURNS VARCHAR(140)
DETERMINISTIC
BEGIN
    DECLARE output TEXT;
    SET output = LOWER(TRIM(input));

    SET output = REPLACE(output, 'đ', 'd');
    SET output = REPLACE(output, 'à', 'a');
    SET output = REPLACE(output, 'á', 'a');
    SET output = REPLACE(output, 'ạ', 'a');
    SET output = REPLACE(output, 'ả', 'a');
    SET output = REPLACE(output, 'ã', 'a');
    SET output = REPLACE(output, 'â', 'a');
    SET output = REPLACE(output, 'ầ', 'a');
    SET output = REPLACE(output, 'ấ', 'a');
    SET output = REPLACE(output, 'ậ', 'a');
    SET output = REPLACE(output, 'ẩ', 'a');
    SET output = REPLACE(output, 'ẫ', 'a');
    SET output = REPLACE(output, 'ă', 'a');
    SET output = REPLACE(output, 'ằ', 'a');
    SET output = REPLACE(output, 'ắ', 'a');
    SET output = REPLACE(output, 'ặ', 'a');
    SET output = REPLACE(output, 'ẳ', 'a');
    SET output = REPLACE(output, 'ẵ', 'a');
    SET output = REPLACE(output, 'è', 'e');
    SET output = REPLACE(output, 'é', 'e');
    SET output = REPLACE(output, 'ẹ', 'e');
    SET output = REPLACE(output, 'ẻ', 'e');
    SET output = REPLACE(output, 'ẽ', 'e');
    SET output = REPLACE(output, 'ê', 'e');
    SET output = REPLACE(output, 'ề', 'e');
    SET output = REPLACE(output, 'ế', 'e');
    SET output = REPLACE(output, 'ệ', 'e');
    SET output = REPLACE(output, 'ể', 'e');
    SET output = REPLACE(output, 'ễ', 'e');
    SET output = REPLACE(output, 'ì', 'i');
    SET output = REPLACE(output, 'í', 'i');
    SET output = REPLACE(output, 'ị', 'i');
    SET output = REPLACE(output, 'ỉ', 'i');
    SET output = REPLACE(output, 'ĩ', 'i');
    SET output = REPLACE(output, 'ò', 'o');
    SET output = REPLACE(output, 'ó', 'o');
    SET output = REPLACE(output, 'ọ', 'o');
    SET output = REPLACE(output, 'ỏ', 'o');
    SET output = REPLACE(output, 'õ', 'o');
    SET output = REPLACE(output, 'ô', 'o');
    SET output = REPLACE(output, 'ồ', 'o');
    SET output = REPLACE(output, 'ố', 'o');
    SET output = REPLACE(output, 'ộ', 'o');
    SET output = REPLACE(output, 'ổ', 'o');
    SET output = REPLACE(output, 'ỗ', 'o');
    SET output = REPLACE(output, 'ơ', 'o');
    SET output = REPLACE(output, 'ờ', 'o');
    SET output = REPLACE(output, 'ớ', 'o');
    SET output = REPLACE(output, 'ợ', 'o');
    SET output = REPLACE(output, 'ở', 'o');
    SET output = REPLACE(output, 'ỡ', 'o');
    SET output = REPLACE(output, 'ù', 'u');
    SET output = REPLACE(output, 'ú', 'u');
    SET output = REPLACE(output, 'ụ', 'u');
    SET output = REPLACE(output, 'ủ', 'u');
    SET output = REPLACE(output, 'ũ', 'u');
    SET output = REPLACE(output, 'ư', 'u');
    SET output = REPLACE(output, 'ừ', 'u');
    SET output = REPLACE(output, 'ứ', 'u');
    SET output = REPLACE(output, 'ự', 'u');
    SET output = REPLACE(output, 'ử', 'u');
    SET output = REPLACE(output, 'ữ', 'u');
    SET output = REPLACE(output, 'ỳ', 'y');
    SET output = REPLACE(output, 'ý', 'y');
    SET output = REPLACE(output, 'ỵ', 'y');
    SET output = REPLACE(output, 'ỷ', 'y');
    SET output = REPLACE(output, 'ỹ', 'y');

    SET output = REGEXP_REPLACE(output, '[^a-z0-9]+', '-');
    SET output = REGEXP_REPLACE(output, '^-+|-+$', '');
    RETURN LEFT(COALESCE(NULLIF(output, ''), 'hero'), 140);
END//
DELIMITER ;

UPDATE heroes
SET slug = atg_slugify(name)
WHERE slug IS NULL OR slug = '';

DROP FUNCTION IF EXISTS atg_slugify;

-- 3) Resolve duplicate slugs using -2, -3, ... suffixes.
-- Keep the final value inside VARCHAR(140) before appending the suffix.
UPDATE heroes h
JOIN (
    SELECT id, slug, ROW_NUMBER() OVER (PARTITION BY slug ORDER BY id) AS rn
    FROM heroes
    WHERE slug IS NOT NULL AND slug <> ''
) ranked ON ranked.id = h.id
SET h.slug = CONCAT(
        LEFT(h.slug, GREATEST(1, 140 - CHAR_LENGTH(CONCAT('-', ranked.rn)))),
        '-',
        ranked.rn
    )
WHERE ranked.rn > 1;

-- Optional sanity checks. Both result sets should be empty before continuing.
SELECT slug, COUNT(*) AS duplicate_count
FROM heroes
GROUP BY slug
HAVING COUNT(*) > 1;

SELECT id, name
FROM heroes
WHERE slug IS NULL OR slug = '';

-- 4) Add the unique index before changing slug to NOT NULL.
-- The unique index is also the lookup index for /api/wiki/heroes/{slug};
-- do not add a second non-unique slug index.
CREATE UNIQUE INDEX uk_heroes_slug ON heroes(slug);

-- 5) Make slug NOT NULL only after every row is backfilled and unique.
ALTER TABLE heroes MODIFY slug VARCHAR(140) NOT NULL;
CREATE INDEX idx_heroes_name ON heroes(name);

-- 6) Hero skills.
CREATE TABLE IF NOT EXISTS hero_skills (
    id BIGINT NOT NULL AUTO_INCREMENT,
    hero_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    skill_type VARCHAR(30) NOT NULL,
    description TEXT NULL,
    cooldown VARCHAR(80) NULL,
    mana_cost VARCHAR(80) NULL,
    icon_url VARCHAR(255) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_hero_skills_hero FOREIGN KEY (hero_id) REFERENCES heroes(id),
    CONSTRAINT uk_hero_skills_hero_skill_type UNIQUE (hero_id, skill_type),
    -- This unique key also serves as the hero_skills(hero_id, sort_order) lookup index.
    CONSTRAINT uk_hero_skills_hero_sort_order UNIQUE (hero_id, sort_order),
    INDEX idx_hero_skills_skill_type (skill_type)
);

-- 7) Hero matchups.
CREATE TABLE IF NOT EXISTS hero_matchups (
    id BIGINT NOT NULL AUTO_INCREMENT,
    hero_id BIGINT NOT NULL,
    target_hero_id BIGINT NOT NULL,
    matchup_type VARCHAR(30) NOT NULL,
    difficulty VARCHAR(30) NOT NULL,
    notes TEXT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_hero_matchups_hero FOREIGN KEY (hero_id) REFERENCES heroes(id),
    CONSTRAINT fk_hero_matchups_target_hero FOREIGN KEY (target_hero_id) REFERENCES heroes(id),
    CONSTRAINT uk_hero_matchups_pair_type UNIQUE (hero_id, target_hero_id, matchup_type),
    CONSTRAINT chk_hero_matchups_not_self CHECK (hero_id <> target_hero_id),
    INDEX idx_hero_matchups_hero_type (hero_id, matchup_type),
    INDEX idx_hero_matchups_target_hero_id (target_hero_id),
    INDEX idx_hero_matchups_type (matchup_type)
);

-- 8) Related guide lookup used by hero detail pages.
CREATE INDEX idx_guides_hero_status_published
    ON guides(hero_id, status, published_at);
