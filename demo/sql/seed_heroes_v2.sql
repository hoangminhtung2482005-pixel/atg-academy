-- ===========================================================================
-- ATG Academy Wiki Hero Seed v2 - SAFE UPSERT for Phase 1 schema (MySQL 8.0)
--
-- Source: sql/seed_heroes.sql legacy 127-row list.
-- Purpose: import/update all 127 heroes without dropping tables and without
-- overwriting Phase 1 detail columns: title, difficulty, description, lore,
-- portrait_url, banner_url.
--
-- Requirements before running:
-- 1. Back up the database.
-- 2. Run sql/wiki_phase1_hero_cleanup_notes.sql if Phase 1 columns/indexes are
--    not already present.
-- 3. Confirm heroes.slug has a unique index and no NULL/empty/duplicate values.
--
-- This script intentionally does not DROP/TRUNCATE/DELETE real Wiki tables.
-- It creates only a TEMPORARY staging table for this session.
-- ===========================================================================

DROP PROCEDURE IF EXISTS assert_seed_heroes_v2_ready;
DELIMITER //
CREATE PROCEDURE assert_seed_heroes_v2_ready()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'heroes' AND COLUMN_NAME = 'slug'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing heroes.slug. Run wiki_phase1_hero_cleanup_notes.sql first.';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'heroes' AND COLUMN_NAME = 'slug' AND NON_UNIQUE = 0
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing unique index on heroes.slug. Run Phase 1 slug migration first.';
    END IF;

    IF EXISTS (SELECT 1 FROM heroes WHERE slug IS NULL OR slug = '') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Existing heroes contain NULL/empty slug. Backfill before seeding.';
    END IF;

    IF EXISTS (
        SELECT 1 FROM (
            SELECT slug FROM heroes GROUP BY slug HAVING COUNT(*) > 1
        ) duplicate_slugs
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Existing heroes contain duplicate slugs. Resolve before seeding.';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hero_roles'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing hero_roles table.';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'hero_role_mapping'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Missing hero_role_mapping table.';
    END IF;
END//
DELIMITER ;

CALL assert_seed_heroes_v2_ready();
DROP PROCEDURE IF EXISTS assert_seed_heroes_v2_ready;

CREATE TEMPORARY TABLE tmp_seed_heroes_v2 (
    slug VARCHAR(140) NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(255) NULL,
    hero_class VARCHAR(30) NULL,
    lane_code VARCHAR(10) NULL
) ENGINE=Memory;

INSERT INTO tmp_seed_heroes_v2 (slug, name, avatar_url, hero_class, lane_code) VALUES
('toro', 'Toro', '/images/heroes/Toro.jpg', 'Đỡ đòn', 'SUP'),
('hayate', 'Hayate', '/images/heroes/Hayate.jpg', 'Xạ thủ', 'ADL'),
('marja', 'Marja', '/images/heroes/Marja.jpg', 'Pháp sư', 'MID'),
('zata', 'Zata', '/images/heroes/Zata.jpg', 'Sát thủ', 'JGL'),
('y-bneth', 'Y''bneth', '/images/heroes/Y''bneth.jpg', 'Đỡ đòn', 'SUP'),
('wisp', 'Wisp', '/images/heroes/Wisp.jpg', 'Xạ thủ', 'ADL'),
('billow', 'Billow', '/images/heroes/Billow.jpg', 'Sát thủ', 'JGL'),
('sinestrea', 'Sinestrea', '/images/heroes/Sinestrea.jpg', 'Sát thủ', 'JGL'),
('lorion', 'Lorion', '/images/heroes/Lorion.jpg', 'Pháp sư', 'MID'),
('ryoma', 'Ryoma', '/images/heroes/Ryoma.jpg', 'Đấu sĩ', 'DSL'),
('dolia', 'Dolia', '/images/heroes/Dolia.jpg', 'Trợ thủ', 'SUP'),
('qi', 'Qi', '/images/heroes/Qi.jpg', 'Đấu sĩ', 'DSL'),
('aya', 'Aya', '/images/heroes/Aya.jpg', 'Trợ thủ', 'SUP'),
('violet', 'Violet', '/images/heroes/Violet.jpg', 'Xạ thủ', 'ADL'),
('yue', 'Yue', '/images/heroes/Yue.jpg', 'Pháp sư', 'MID'),
('volkath', 'Volkath', '/images/heroes/Volkath.jpg', 'Đấu sĩ', 'DSL'),
('enzo', 'Enzo', '/images/heroes/Enzo.jpg', 'Sát thủ', 'JGL'),
('murad', 'Murad', '/images/heroes/Murad.jpg', 'Sát thủ', 'JGL'),
('gildur', 'Gildur', '/images/heroes/Gildur.jpg', 'Pháp sư', 'MID'),
('moren', 'Moren', '/images/heroes/Moren.jpg', 'Xạ thủ', 'ADL'),
('florentino', 'Florentino', '/images/heroes/Florentino.jpg', 'Đấu sĩ', 'DSL'),
('aleister', 'Aleister', '/images/heroes/Aleister.jpg', 'Pháp sư', 'MID'),
('annette', 'Annette', '/images/heroes/Annette.jpg', 'Trợ thủ', 'SUP'),
('zephys', 'Zephys', '/images/heroes/Zephys.jpg', 'Đấu sĩ', 'DSL'),
('elsu', 'Elsu', '/images/heroes/Elsu.jpg', 'Xạ thủ', 'ADL'),
('airi', 'Airi', '/images/heroes/Airi.jpg', 'Đấu sĩ', 'DSL'),
('nakroth', 'Nakroth', '/images/heroes/Nakroth.jpg', 'Sát thủ', 'JGL'),
('fennik', 'Fennik', '/images/heroes/Fennik.jpg', 'Xạ thủ', 'ADL'),
('liliana', 'Liliana', '/images/heroes/Liliana.jpg', 'Pháp sư', 'MID'),
('tachi', 'Tachi', '/images/heroes/Tachi.jpg', 'Đấu sĩ', 'DSL'),
('preyta', 'Preyta', '/images/heroes/Preyta.jpg', 'Pháp sư', 'MID'),
('taara', 'Taara', '/images/heroes/Taara.jpg', 'Đỡ đòn', 'SUP'),
('dyadia', 'Dyadia', '/images/heroes/Dyadia.jpg', 'Trợ thủ', 'SUP'),
('quillen', 'Quillen', '/images/heroes/Quillen.jpg', 'Sát thủ', 'JGL'),
('capheny', 'Capheny', '/images/heroes/Capheny.jpg', 'Xạ thủ', 'ADL'),
('iggy', 'Iggy', '/images/heroes/Iggy.jpeg', 'Pháp sư', 'MID'),
('veera', 'Veera', '/images/heroes/Veera.jpg', 'Pháp sư', 'MID'),
('stuart', 'Stuart', '/images/heroes/Stuart.jpg', 'Xạ thủ', 'ADL'),
('keera', 'Keera', '/images/heroes/Keera.jpg', 'Sát thủ', 'JGL'),
('skud', 'Skud', '/images/heroes/Skud.jpg', 'Đấu sĩ', 'DSL'),
('azzen-ka', 'Azzen''Ka', '/images/heroes/Azzen''Ka.jpg', 'Pháp sư', 'MID'),
('yena', 'Yena', '/images/heroes/Yena.jpg', 'Đấu sĩ', 'DSL'),
('mina', 'Mina', '/images/heroes/Mina.jpg', 'Đỡ đòn', 'SUP'),
('tel-annas', 'Tel''Annas', '/images/heroes/Tel''Annas.jpg', 'Xạ thủ', 'ADL'),
('goverra', 'Goverra', '/images/heroes/Goverra.jpg', 'Pháp sư', 'MID'),
('teemee', 'TeeMee', '/images/heroes/TeeMee.jpg', 'Trợ thủ', 'SUP'),
('aoi', 'Aoi', '/images/heroes/Aoi.jpeg', 'Sát thủ', 'JGL'),
('rourka', 'Rourka', '/images/heroes/Rourke.jpg', 'Đấu sĩ', 'DSL'),
('bolt-baron', 'Bolt Baron', '/images/heroes/Bolt Baron.jpg', 'Đấu sĩ', 'DSL'),
('rouie', 'Rouie', '/images/heroes/Rouie.jpg', 'Trợ thủ', 'SUP'),
('eland-orr', 'Eland''orr', '/images/heroes/Eland''orr.jpg', 'Xạ thủ', 'ADL'),
('lumburr', 'Lumburr', '/images/heroes/Lumburr.jpg', 'Trợ thủ', 'SUP'),
('heino', 'Heino', '/images/heroes/Heino.jpg', 'Pháp sư', 'MID'),
('arthur', 'Arthur', '/images/heroes/Athur.jpg', 'Đấu sĩ', 'DSL'),
('flowborn-marksman', 'Flowborn (Marksman)', '/images/heroes/Flowborn (ADL).jpg', 'Xạ thủ', 'ADL'),
('edras', 'Edras', '/images/heroes/Edras.jpg', 'Đấu sĩ', 'DSL'),
('grakk', 'Grakk', '/images/heroes/Grakk.png', 'Trợ thủ', 'SUP'),
('jinna', 'Jinna', '/images/heroes/Jinna.jpg', 'Pháp sư', 'MID'),
('mganga', 'Mganga', '/images/heroes/Mganga.jpg', 'Pháp sư', 'MID'),
('natalya', 'Natalya', '/images/heroes/Natalya.jpg', 'Pháp sư', 'MID'),
('kaine', 'Kaine', '/images/heroes/Kaine.jpg', 'Sát thủ', 'JGL'),
('omen', 'Omen', '/images/heroes/Omen.jpg', 'Đấu sĩ', 'DSL'),
('dirak', 'Dirak', '/images/heroes/Dirak.jpg', 'Pháp sư', 'MID'),
('butterfly', 'Butterfly', '/images/heroes/Butterfly.jpg', 'Sát thủ', 'JGL'),
('wukong', 'Wukong', '/images/heroes/Ngộ Không.jpg', 'Sát thủ', 'JGL'),
('diaochan', 'Diaochan', '/images/heroes/Điêu Thuyền.jpg', 'Pháp sư', 'MID'),
('xeniel', 'Xeniel', '/images/heroes/Xeniel.jpg', 'Đỡ đòn', 'SUP'),
('zanis', 'Zanis', '/images/heroes/Triệu Vân.jpg', 'Đấu sĩ', 'DSL'),
('ming', 'Ming', '/images/heroes/Ming.jpg', 'Trợ thủ', 'SUP'),
('chaugnar', 'Chaugnar', '/images/heroes/Chaugnar.jpg', 'Đỡ đòn', 'SUP'),
('astrid', 'Astrid', '/images/heroes/Astrid.jpg', 'Đấu sĩ', 'DSL'),
('biron', 'Biron', '/images/heroes/Biron.jpg', 'Đấu sĩ', 'DSL'),
('roxie', 'Roxie', '/images/heroes/Roxie.jpg', 'Đỡ đòn', 'SUP'),
('lauriel', 'Lauriel', '/images/heroes/Lauriel.jpg', 'Pháp sư', 'MID'),
('thorne', 'Thorne', '/images/heroes/Thorn.jpg', 'Xạ thủ', 'ADL'),
('zuka', 'Zuka', '/images/heroes/Zuka.jpg', 'Đấu sĩ', 'DSL'),
('tulen', 'Tulen', '/images/heroes/Tulen.jpg', 'Pháp sư', 'MID'),
('valhein', 'Valhein', '/images/heroes/Valhein.jpg', 'Xạ thủ', 'ADL'),
('zip', 'Zip', '/images/heroes/Zip.jpg', 'Trợ thủ', 'SUP'),
('max', 'Max', '/images/heroes/Max.jpg', 'Đỡ đòn', 'SUP'),
('sephera', 'Sephera', '/images/heroes/Sephera.jpg', 'Pháp sư', 'MID'),
('maloch', 'Maloch', '/images/heroes/Maloch.jpg', 'Đấu sĩ', 'DSL'),
('riktor', 'Riktor', '/images/heroes/Richter.jpg', 'Đấu sĩ', 'DSL'),
('thane', 'Thane', '/images/heroes/Thane.png', 'Đỡ đòn', 'SUP'),
('yan', 'Yan', '/images/heroes/Yan.jpg', 'Đấu sĩ', 'DSL'),
('yorn', 'Yorn', '/images/heroes/Yorn.jpg', 'Xạ thủ', 'ADL'),
('slimz', 'Slimz', '/images/heroes/Slimz.png', 'Xạ thủ', 'ADL'),
('laville', 'Laville', '/images/heroes/Laville.jpg', 'Xạ thủ', 'ADL'),
('ignis', 'Ignis', '/images/heroes/Ignis.jpg', 'Pháp sư', 'MID'),
('ilumia', 'Ilumia', '/images/heroes/Ilumia.jpg', 'Pháp sư', 'MID'),
('krizzix', 'Krizzix', '/images/heroes/Krizzix.png', 'Trợ thủ', 'SUP'),
('krixi', 'Krixi', '/images/heroes/Krixi.png', 'Pháp sư', 'MID'),
('raz', 'Raz', '/images/heroes/Raz.jpg', 'Sát thủ', 'JGL'),
('allain', 'Allain', '/images/heroes/Allain.jpg', 'Đấu sĩ', 'DSL'),
('cresht', 'Cresht', '/images/heroes/Cresht.jpg', 'Trợ thủ', 'SUP'),
('omega', 'Omega', '/images/heroes/Omega.jpg', 'Đỡ đòn', 'SUP'),
('kahli', 'Kahli', '/images/heroes/Kahlii.png', 'Pháp sư', 'MID'),
('arduin', 'Arduin', '/images/heroes/Arduin.jpg', 'Đỡ đòn', 'SUP'),
('erin', 'Erin', '/images/heroes/Erin.jpg', 'Xạ thủ', 'ADL'),
('kil-groth', 'Kil''Groth', '/images/heroes/Kil''Groth.gif', 'Đấu sĩ', 'DSL'),
('veres', 'Veres', '/images/heroes/Veres.jpg', 'Đấu sĩ', 'DSL'),
('arum', 'Arum', '/images/heroes/Arum.jpg', 'Trợ thủ', 'SUP'),
('bijan', 'Bijan', '/images/heroes/Bijan.jpg', 'Đấu sĩ', 'DSL'),
('bonnie', 'Bonnie', '/images/heroes/Bonnie.jpg', 'Pháp sư', 'MID'),
('teeri', 'Teeri', '/images/heroes/Teeri.jpg', 'Xạ thủ', 'ADL'),
('baldum', 'Baldum', '/images/heroes/Baldum.jpg', 'Đỡ đòn', 'SUP'),
('errol', 'Errol', '/images/heroes/Errol.jpg', 'Đấu sĩ', 'DSL'),
('helen', 'Helen', '/images/heroes/Helen.jpg', 'Trợ thủ', 'SUP'),
('amily', 'Amily', '/images/heroes/Amily.jpg', 'Đấu sĩ', 'DSL'),
('wonder-woman', 'Wonder Woman', '/images/heroes/Wonder Women.jpg', 'Đấu sĩ', 'DSL'),
('kriknak', 'Kriknak', '/images/heroes/Kriknak.png', 'Sát thủ', 'JGL'),
('lindis', 'Lindis', '/images/heroes/Lindis.jpg', 'Xạ thủ', 'ADL'),
('lu-bu', 'Lu Bu', '/images/heroes/Lữ Bố.jpg', 'Đấu sĩ', 'DSL'),
('wiro', 'Wiro', '/images/heroes/Wiro.jpg', 'Đỡ đòn', 'SUP'),
('zill', 'Zill', '/images/heroes/Zill.jpg', 'Sát thủ', 'JGL'),
('charlotte', 'Charlotte', '/images/heroes/Charlotte.jpg', 'Đấu sĩ', 'DSL'),
('ata', 'Ata', '/images/heroes/Ata.jpg', 'Đỡ đòn', 'SUP'),
('alice', 'Alice', '/images/heroes/Alice.png', 'Trợ thủ', 'SUP'),
('dextra', 'Dextra', '/images/heroes/Dextra.jpg', 'Đấu sĩ', 'DSL'),
('paine', 'Paine', '/images/heroes/Paine.jpg', 'Sát thủ', 'JGL'),
('ormarr', 'Ormarr', '/images/heroes/Omarr.jpg', 'Đỡ đòn', 'SUP'),
('bright', 'Bright', '/images/heroes/Bright.jpg', 'Sát thủ', 'JGL'),
('d-arcy', 'D''Arcy', '/images/heroes/D''Arcy.jpg', 'Pháp sư', 'MID'),
('celica', 'Celica', '/images/heroes/Celica.jpg', 'Xạ thủ', 'ADL'),
('the-flash', 'The Flash', '/images/heroes/The Flash.jpg', 'Sát thủ', 'JGL'),
('ishar', 'Ishar', '/images/heroes/Ishar.jpg', 'Pháp sư', 'MID'),
('superman', 'Superman', '/images/heroes/Superman.jpg', 'Đấu sĩ', 'DSL');

-- Seed fixed lane roles used by Hero.roles / laneRoles DTO.
INSERT INTO hero_roles (code, name) VALUES
('DSL', 'Đường Solo Lẻ'),
('JGL', 'Đi Rừng'),
('MID', 'Đường Giữa'),
('ADL', 'Xạ Thủ'),
('SUP', 'Hỗ Trợ')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Upsert heroes by slug. Do not touch Phase 1 detail fields.
INSERT INTO heroes (slug, name, avatar_url, hero_class)
SELECT slug, name, avatar_url, hero_class
FROM tmp_seed_heroes_v2
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    avatar_url = VALUES(avatar_url),
    hero_class = VALUES(hero_class);

-- Add conservative default lane mappings derived from hero_class.
-- Existing more precise mappings are preserved because INSERT IGNORE is additive.
INSERT IGNORE INTO hero_role_mapping (hero_id, role_id)
SELECT h.id, r.id
FROM tmp_seed_heroes_v2 seed
JOIN heroes h ON h.slug = seed.slug
JOIN hero_roles r ON r.code = seed.lane_code
WHERE seed.lane_code IS NOT NULL AND seed.lane_code <> '';

-- This source seed has no attribute data. Existing hero_attributes and
-- hero_attribute_mapping rows are intentionally left unchanged.

-- Validation: expected imported catalog size is 127 if the DB contains no extra heroes.
SELECT 'hero_count' AS check_name, COUNT(*) AS actual, 127 AS expected
FROM heroes;

SELECT 'seed_rows_present' AS check_name, COUNT(*) AS actual, 127 AS expected
FROM tmp_seed_heroes_v2 seed
JOIN heroes h ON h.slug = seed.slug;

SELECT slug, COUNT(*) AS duplicate_count
FROM heroes
GROUP BY slug
HAVING COUNT(*) > 1;

SELECT id, name
FROM heroes
WHERE slug IS NULL OR slug = '';

SELECT id, name
FROM heroes
WHERE hero_class IS NULL OR hero_class = '';

SELECT h.id, h.name, h.slug, h.hero_class, GROUP_CONCAT(r.code ORDER BY r.code) AS lane_codes
FROM heroes h
LEFT JOIN hero_role_mapping hrm ON hrm.hero_id = h.id
LEFT JOIN hero_roles r ON r.id = hrm.role_id
GROUP BY h.id, h.name, h.slug, h.hero_class
ORDER BY h.name;