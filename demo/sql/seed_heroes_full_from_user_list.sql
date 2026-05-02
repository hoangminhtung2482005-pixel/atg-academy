-- ============================================================================
-- ATG Academy Wiki Hero Seed - SAFE UPSERT from user-provided full hero list
-- MySQL 8.0
--
-- Safety guarantees:
-- - Does NOT DROP/TRUNCATE/DELETE real Wiki tables.
-- - Does NOT recreate heroes or mapping tables.
-- - Inserts heroes.slug and updates only heroes.name, heroes.avatar_url,
--   heroes.hero_class on duplicate slug.
-- - Does NOT touch title, difficulty, description, lore, portrait_url, banner_url.
-- - Uses heroes.slug as the stable upsert key.
-- - Adds default lane-role mappings with INSERT IGNORE only; existing mappings
--   and manual admin edits are not removed or replaced.
--
-- Note: the list provided in the request contains 128 rows.
-- Avatar lookup order:
-- 1. /images/heroes/{slug}.png|jpg|jpeg|gif|webp
-- 2. /assets/heroes/{slug}.png|jpg|jpeg|gif|webp
-- 3. /img/heroes/{slug}.png|jpg|jpeg|gif|webp
-- 4. /images/heroes/* matched by slugified local filename
-- ============================================================================

-- Missing avatar files (using /images/heroes/default.png):
-- - Zephys -> expected slug asset for 'zephys'
-- - Flowborn (Mage) -> expected slug asset for 'flowborn-mage'
-- - Flowborn (Marksman) -> expected slug asset for 'flowborn-marksman'
-- - Arthur -> expected slug asset for 'arthur'
-- - Rourka -> expected slug asset for 'rourka'
-- - Thorne -> expected slug asset for 'thorne'
-- - Wonder Woman -> expected slug asset for 'wonder-woman'
-- - Lu Bu -> expected slug asset for 'lu-bu'
-- - Ormarr -> expected slug asset for 'ormarr'

DROP PROCEDURE IF EXISTS assert_seed_heroes_full_from_user_list_ready;
DELIMITER //
CREATE PROCEDURE assert_seed_heroes_full_from_user_list_ready()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'heroes'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing heroes table.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'heroes'
          AND COLUMN_NAME = 'slug'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing heroes.slug column. Run Phase 1 slug migration first.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'heroes'
          AND COLUMN_NAME = 'name'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing heroes.name column.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'heroes'
          AND COLUMN_NAME = 'avatar_url'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing heroes.avatar_url column.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'heroes'
          AND COLUMN_NAME = 'hero_class'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing heroes.hero_class column.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'heroes'
          AND COLUMN_NAME = 'slug'
          AND NON_UNIQUE = 0
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing unique index on heroes.slug. ON DUPLICATE KEY UPDATE requires slug to be unique.';
    END IF;

    IF EXISTS (SELECT 1 FROM heroes WHERE slug IS NULL OR slug = '') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Existing heroes contain NULL/empty slug. Backfill before seeding.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM (
            SELECT slug
            FROM heroes
            GROUP BY slug
            HAVING COUNT(*) > 1
        ) duplicate_slugs
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Existing heroes contain duplicate slugs. Resolve before seeding.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'hero_roles'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing hero_roles table.';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'hero_role_mapping'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing hero_role_mapping table.';
    END IF;
END//
DELIMITER ;

CALL assert_seed_heroes_full_from_user_list_ready();
DROP PROCEDURE IF EXISTS assert_seed_heroes_full_from_user_list_ready;

CREATE TEMPORARY TABLE IF NOT EXISTS tmp_seed_heroes_full_from_user_list (
    slug VARCHAR(140) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(255) NULL,
    hero_class VARCHAR(30) NOT NULL,
    lane_code VARCHAR(10) NOT NULL
) ENGINE=Memory DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_seed_heroes_full_from_user_list (slug, name, avatar_url, hero_class, lane_code) VALUES
('toro', 'Toro', '/images/heroes/Toro.jpg', 'Đỡ đòn', 'SUP'),
('hayate', 'Hayate', '/images/heroes/Hayate.jpg', 'Xạ thủ', 'ADL'),
('marja', 'Marja', '/images/heroes/Marja.jpg', 'Pháp sư', 'MID'),
('zata', 'Zata', '/images/heroes/Zata.jpg', 'Sát thủ', 'JGL'),
('sinestrea', 'Sinestrea', '/images/heroes/Sinestrea.jpg', 'Sát thủ', 'JGL'),
('y-bneth', 'Y''bneth', '/images/heroes/Y''bneth.jpg', 'Đỡ đòn', 'SUP'),
('billow', 'Billow', '/images/heroes/Billow.jpg', 'Sát thủ', 'JGL'),
('wisp', 'Wisp', '/images/heroes/Wisp.jpg', 'Xạ thủ', 'ADL'),
('murad', 'Murad', '/images/heroes/Murad.jpg', 'Sát thủ', 'JGL'),
('violet', 'Violet', '/images/heroes/Violet.jpg', 'Xạ thủ', 'ADL'),
('lorion', 'Lorion', '/images/heroes/Lorion.jpg', 'Pháp sư', 'MID'),
('ryoma', 'Ryoma', '/images/heroes/Ryoma.jpg', 'Đấu sĩ', 'DSL'),
('annette', 'Annette', '/images/heroes/Annette.jpg', 'Trợ thủ', 'SUP'),
('volkath', 'Volkath', '/images/heroes/Volkath.jpg', 'Đấu sĩ', 'DSL'),
('qi', 'Qi', '/images/heroes/Qi.jpg', 'Đấu sĩ', 'DSL'),
('enzo', 'Enzo', '/images/heroes/Enzo.jpg', 'Sát thủ', 'JGL'),
('moren', 'Moren', '/images/heroes/Moren.jpg', 'Xạ thủ', 'ADL'),
('gildur', 'Gildur', '/images/heroes/Gildur.jpg', 'Pháp sư', 'MID'),
('florentino', 'Florentino', '/images/heroes/Florentino.jpg', 'Đấu sĩ', 'DSL'),
('aya', 'Aya', '/images/heroes/Aya.jpg', 'Trợ thủ', 'SUP'),
('airi', 'Airi', '/images/heroes/Airi.jpg', 'Đấu sĩ', 'DSL'),
('dolia', 'Dolia', '/images/heroes/Dolia.jpg', 'Trợ thủ', 'SUP'),
('yue', 'Yue', '/images/heroes/Yue.jpg', 'Pháp sư', 'MID'),
('tachi', 'Tachi', '/images/heroes/Tachi.jpg', 'Đấu sĩ', 'DSL'),
('nakroth', 'Nakroth', '/images/heroes/Nakroth.jpg', 'Sát thủ', 'JGL'),
('zephys', 'Zephys', '/images/heroes/default.png', 'Đấu sĩ', 'DSL'),
('flowborn-mage', 'Flowborn (Mage)', '/images/heroes/default.png', 'Pháp sư', 'MID'),
('taara', 'Taara', '/images/heroes/Taara.jpg', 'Đỡ đòn', 'SUP'),
('fennik', 'Fennik', '/images/heroes/Fennik.jpg', 'Xạ thủ', 'ADL'),
('elsu', 'Elsu', '/images/heroes/Elsu.jpg', 'Xạ thủ', 'ADL'),
('stuart', 'Stuart', '/images/heroes/Stuart.jpg', 'Xạ thủ', 'ADL'),
('teemee', 'TeeMee', '/images/heroes/TeeMee.jpg', 'Trợ thủ', 'SUP'),
('azzen-ka', 'Azzen''Ka', '/images/heroes/Azzen''Ka.jpg', 'Pháp sư', 'MID'),
('ngo-khong', 'Ngộ Không', '/images/heroes/Ngộ Không.jpg', 'Sát thủ', 'JGL'),
('rouie', 'Rouie', '/images/heroes/Rouie.jpg', 'Trợ thủ', 'SUP'),
('skud', 'Skud', '/images/heroes/Skud.jpg', 'Đấu sĩ', 'DSL'),
('capheny', 'Capheny', '/images/heroes/Capheny.jpg', 'Xạ thủ', 'ADL'),
('liliana', 'Liliana', '/images/heroes/Liliana.jpg', 'Pháp sư', 'MID'),
('flowborn-marksman', 'Flowborn (Marksman)', '/images/heroes/default.png', 'Xạ thủ', 'ADL'),
('yena', 'Yena', '/images/heroes/Yena.jpg', 'Đấu sĩ', 'DSL'),
('goverra', 'Goverra', '/images/heroes/Goverra.jpg', 'Pháp sư', 'MID'),
('dyadia', 'Dyadia', '/images/heroes/Dyadia.jpg', 'Trợ thủ', 'SUP'),
('mina', 'Mina', '/images/heroes/Mina.png', 'Đỡ đòn', 'SUP'),
('cresht', 'Cresht', '/images/heroes/Cresht.jpg', 'Trợ thủ', 'SUP'),
('natalya', 'Natalya', '/images/heroes/Natalya.jpg', 'Pháp sư', 'MID'),
('aleister', 'Aleister', '/images/heroes/Aleister.jpg', 'Pháp sư', 'MID'),
('eland-orr', 'Eland''orr', '/images/heroes/Eland''orr.jpg', 'Xạ thủ', 'ADL'),
('grakk', 'Grakk', '/images/heroes/Grakk.png', 'Trợ thủ', 'SUP'),
('preyta', 'Preyta', '/images/heroes/Preyta.jpg', 'Pháp sư', 'MID'),
('edras', 'Edras', '/images/heroes/Edras.jpg', 'Đấu sĩ', 'DSL'),
('arthur', 'Arthur', '/images/heroes/default.png', 'Đấu sĩ', 'DSL'),
('tel-annas', 'Tel''Annas', '/images/heroes/Tel''Annas.jpg', 'Xạ thủ', 'ADL'),
('iggy', 'Iggy', '/images/heroes/Iggy.jpeg', 'Pháp sư', 'MID'),
('alice', 'Alice', '/images/heroes/Alice.png', 'Trợ thủ', 'SUP'),
('omen', 'Omen', '/images/heroes/Omen.jpg', 'Đấu sĩ', 'DSL'),
('bolt-baron', 'Bolt Baron', '/images/heroes/Bolt Baron.jpg', 'Đấu sĩ', 'DSL'),
('astrid', 'Astrid', '/images/heroes/Astrid.jpg', 'Đấu sĩ', 'DSL'),
('lumburr', 'Lumburr', '/images/heroes/Lumburr.jpg', 'Trợ thủ', 'SUP'),
('keera', 'Keera', '/images/heroes/Keera.jpg', 'Sát thủ', 'JGL'),
('trieu-van', 'Triệu Vân', '/images/heroes/Triệu Vân.jpg', 'Đấu sĩ', 'DSL'),
('aoi', 'Aoi', '/images/heroes/Aoi.jpeg', 'Sát thủ', 'JGL'),
('heino', 'Heino', '/images/heroes/Heino.jpg', 'Pháp sư', 'MID'),
('roxie', 'Roxie', '/images/heroes/Roxie.jpg', 'Đỡ đòn', 'SUP'),
('veera', 'Veera', '/images/heroes/Veera.jpg', 'Pháp sư', 'MID'),
('zuka', 'Zuka', '/images/heroes/Zuka.jpg', 'Đấu sĩ', 'DSL'),
('quillen', 'Quillen', '/images/heroes/Quillen.jpg', 'Sát thủ', 'JGL'),
('butterfly', 'Butterfly', '/images/heroes/Butterfly.jpg', 'Sát thủ', 'JGL'),
('mganga', 'Mganga', '/images/heroes/Mganga.jpg', 'Pháp sư', 'MID'),
('chaugnar', 'Chaugnar', '/images/heroes/Chaugnar.jpg', 'Đỡ đòn', 'SUP'),
('arum', 'Arum', '/images/heroes/Arum.jpg', 'Trợ thủ', 'SUP'),
('rourka', 'Rourka', '/images/heroes/default.png', 'Đấu sĩ', 'DSL'),
('helen', 'Helen', '/images/heroes/Helen.jpg', 'Trợ thủ', 'SUP'),
('zip', 'Zip', '/images/heroes/Zip.jpg', 'Trợ thủ', 'SUP'),
('thane', 'Thane', '/images/heroes/Thane.png', 'Đỡ đòn', 'SUP'),
('biron', 'Biron', '/images/heroes/Biron.jpg', 'Đấu sĩ', 'DSL'),
('thorne', 'Thorne', '/images/heroes/default.png', 'Xạ thủ', 'ADL'),
('krizzix', 'Krizzix', '/images/heroes/Krizzix.png', 'Trợ thủ', 'SUP'),
('valhein', 'Valhein', '/images/heroes/Valhein.jpg', 'Xạ thủ', 'ADL'),
('maloch', 'Maloch', '/images/heroes/Maloch.jpg', 'Đấu sĩ', 'DSL'),
('dirak', 'Dirak', '/images/heroes/Dirak.jpg', 'Pháp sư', 'MID'),
('kaine', 'Kaine', '/images/heroes/Kaine.jpg', 'Sát thủ', 'JGL'),
('ming', 'Ming', '/images/heroes/Ming.jpg', 'Trợ thủ', 'SUP'),
('jinna', 'Jinna', '/images/heroes/Jinna.jpg', 'Pháp sư', 'MID'),
('wiro', 'Wiro', '/images/heroes/Wiro.jpg', 'Đỡ đòn', 'SUP'),
('slimz', 'Slimz', '/images/heroes/Slimz.png', 'Xạ thủ', 'ADL'),
('richter', 'Richter', '/images/heroes/Richter.jpg', 'Đấu sĩ', 'DSL'),
('tulen', 'Tulen', '/images/heroes/Tulen.jpg', 'Pháp sư', 'MID'),
('yan', 'Yan', '/images/heroes/Yan.jpg', 'Đấu sĩ', 'DSL'),
('bijan', 'Bijan', '/images/heroes/Bijan.jpg', 'Đấu sĩ', 'DSL'),
('ignis', 'Ignis', '/images/heroes/Ignis.jpg', 'Pháp sư', 'MID'),
('max', 'Max', '/images/heroes/Max.jpg', 'Đỡ đòn', 'SUP'),
('laville', 'Laville', '/images/heroes/Laville.jpg', 'Xạ thủ', 'ADL'),
('veres', 'Veres', '/images/heroes/Veres.jpg', 'Đấu sĩ', 'DSL'),
('yorn', 'Yorn', '/images/heroes/Yorn.jpg', 'Xạ thủ', 'ADL'),
('lauriel', 'Lauriel', '/images/heroes/Lauriel.jpg', 'Pháp sư', 'MID'),
('ilumia', 'Ilumia', '/images/heroes/Ilumia.jpg', 'Pháp sư', 'MID'),
('raz', 'Raz', '/images/heroes/Raz.jpg', 'Sát thủ', 'JGL'),
('wonder-woman', 'Wonder Woman', '/images/heroes/default.png', 'Đấu sĩ', 'DSL'),
('xeniel', 'Xeniel', '/images/heroes/Xeniel.jpg', 'Đỡ đòn', 'SUP'),
('dieu-thuyen', 'Điêu Thuyền', '/images/heroes/Điêu Thuyền.jpg', 'Pháp sư', 'MID'),
('arduin', 'Arduin', '/images/heroes/Arduin.jpg', 'Đỡ đòn', 'SUP'),
('allain', 'Allain', '/images/heroes/Allain.jpg', 'Đấu sĩ', 'DSL'),
('erin', 'Erin', '/images/heroes/Erin.jpg', 'Xạ thủ', 'ADL'),
('baldum', 'Baldum', '/images/heroes/Baldum.jpg', 'Đỡ đòn', 'SUP'),
('krixi', 'Krixi', '/images/heroes/Krixi.png', 'Pháp sư', 'MID'),
('kil-groth', 'Kil''Groth', '/images/heroes/Kil''Groth.gif', 'Đấu sĩ', 'DSL'),
('errol', 'Errol', '/images/heroes/Errol.jpg', 'Đấu sĩ', 'DSL'),
('kriknak', 'Kriknak', '/images/heroes/Kriknak.png', 'Sát thủ', 'JGL'),
('sephera', 'Sephera', '/images/heroes/Sephera.jpg', 'Pháp sư', 'MID'),
('lu-bu', 'Lu Bu', '/images/heroes/default.png', 'Đấu sĩ', 'DSL'),
('teeri', 'Teeri', '/images/heroes/Teeri.jpg', 'Xạ thủ', 'ADL'),
('bonnie', 'Bonnie', '/images/heroes/Bonnie.jpg', 'Pháp sư', 'MID'),
('charlotte', 'Charlotte', '/images/heroes/Charlotte.jpg', 'Đấu sĩ', 'DSL'),
('kahlii', 'Kahlii', '/images/heroes/Kahlii.png', 'Pháp sư', 'MID'),
('bright', 'Bright', '/images/heroes/Bright.jpg', 'Sát thủ', 'JGL'),
('amily', 'Amily', '/images/heroes/Amily.jpg', 'Đấu sĩ', 'DSL'),
('ormarr', 'Ormarr', '/images/heroes/default.png', 'Đỡ đòn', 'SUP'),
('zill', 'Zill', '/images/heroes/Zill.jpg', 'Sát thủ', 'JGL'),
('omega', 'Omega', '/images/heroes/Omega.jpg', 'Đỡ đòn', 'SUP'),
('lindis', 'Lindis', '/images/heroes/Lindis.jpg', 'Xạ thủ', 'ADL'),
('ata', 'Ata', '/images/heroes/Ata.jpg', 'Đỡ đòn', 'SUP'),
('paine', 'Paine', '/images/heroes/Paine.jpg', 'Sát thủ', 'JGL'),
('dextra', 'Dextra', '/images/heroes/Dextra.jpg', 'Đấu sĩ', 'DSL'),
('d-arcy', 'D''Arcy', '/images/heroes/D''Arcy.jpg', 'Pháp sư', 'MID'),
('ishar', 'Ishar', '/images/heroes/Ishar.jpg', 'Pháp sư', 'MID'),
('celica', 'Celica', '/images/heroes/Celica.jpg', 'Xạ thủ', 'ADL'),
('the-flash', 'The Flash', '/images/heroes/The Flash.jpg', 'Sát thủ', 'JGL'),
('superman', 'Superman', '/images/heroes/Superman.jpg', 'Đấu sĩ', 'DSL')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    avatar_url = VALUES(avatar_url),
    hero_class = VALUES(hero_class),
    lane_code = VALUES(lane_code);

-- Insert fixed lane roles if missing.
INSERT INTO hero_roles (code, name) VALUES
('DSL', 'Đường Solo'),
('JGL', 'Đi rừng'),
('MID', 'Đường giữa'),
('ADL', 'Xạ thủ'),
('SUP', 'Hỗ trợ')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Upsert heroes by slug only. Do not touch other Wiki detail columns.
INSERT INTO heroes (slug, name, avatar_url, hero_class)
SELECT slug, name, avatar_url, hero_class
FROM tmp_seed_heroes_full_from_user_list
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    avatar_url = VALUES(avatar_url),
    hero_class = VALUES(hero_class);

-- Add conservative default role mappings.
-- This is intentionally additive: no existing role mapping is deleted/replaced.
INSERT IGNORE INTO hero_role_mapping (hero_id, role_id)
SELECT h.id, r.id
FROM tmp_seed_heroes_full_from_user_list seed
JOIN heroes h ON h.slug = seed.slug
JOIN hero_roles r ON r.code = seed.lane_code;

-- Extra verification for this seed source.
SELECT 'seed_source_rows' AS check_name, COUNT(*) AS actual, 128 AS expected
FROM tmp_seed_heroes_full_from_user_list;

SELECT 'seed_rows_present_in_heroes' AS check_name, COUNT(*) AS actual, 128 AS expected
FROM tmp_seed_heroes_full_from_user_list seed
JOIN heroes h ON h.slug = seed.slug;

SELECT 'seed_rows_with_avatar_url' AS check_name, COUNT(*) AS actual, 128 AS expected
FROM tmp_seed_heroes_full_from_user_list
WHERE avatar_url IS NOT NULL AND avatar_url <> '';

SELECT 'seed_rows_using_placeholder_avatar' AS check_name, COUNT(*) AS actual, 0 AS ideal
FROM tmp_seed_heroes_full_from_user_list
WHERE avatar_url = '/images/heroes/default.png';

SELECT 'seed_rows_with_default_lane_mapping' AS check_name, COUNT(DISTINCT seed.slug) AS actual, 128 AS expected
FROM tmp_seed_heroes_full_from_user_list seed
JOIN heroes h ON h.slug = seed.slug
JOIN hero_role_mapping hrm ON hrm.hero_id = h.id
JOIN hero_roles r ON r.id = hrm.role_id AND r.code = seed.lane_code;

SELECT h.id, h.slug, h.name
FROM heroes h
LEFT JOIN tmp_seed_heroes_full_from_user_list seed ON seed.slug = h.slug
WHERE seed.slug IS NULL
ORDER BY h.name;

-- Required verification queries.
SELECT COUNT(*) FROM heroes;

SELECT slug, COUNT(*) FROM heroes GROUP BY slug HAVING COUNT(*) > 1;

SELECT COUNT(*) FROM heroes WHERE slug IS NULL OR slug = '';

SELECT COUNT(*) FROM hero_role_mapping;