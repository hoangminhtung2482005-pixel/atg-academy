-- Phuong an A: heroes.primary_role_id is the primary lane role.
-- hero_role_mapping is retained and now represents sub roles only.

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'heroes'
      AND COLUMN_NAME = 'primary_role_id'
);
SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE heroes ADD COLUMN primary_role_id BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @index_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'heroes'
      AND INDEX_NAME = 'idx_heroes_primary_role_id'
);
SET @ddl := IF(
    @index_exists = 0,
    'CREATE INDEX idx_heroes_primary_role_id ON heroes(primary_role_id)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk_exists := (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'heroes'
      AND COLUMN_NAME = 'primary_role_id'
      AND REFERENCED_TABLE_NAME = 'hero_roles'
      AND REFERENCED_COLUMN_NAME = 'id'
);
SET @ddl := IF(
    @fk_exists = 0,
    'ALTER TABLE heroes ADD CONSTRAINT fk_heroes_primary_role FOREIGN KEY (primary_role_id) REFERENCES hero_roles(id)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DROP TEMPORARY TABLE IF EXISTS tmp_primary_role_backfill;
CREATE TEMPORARY TABLE tmp_primary_role_backfill (
    slug VARCHAR(120) NOT NULL PRIMARY KEY,
    lane_code VARCHAR(10) NOT NULL
) ENGINE=Memory;

INSERT INTO tmp_primary_role_backfill (slug, lane_code) VALUES
('toro', 'SUP'),
('hayate', 'ADL'),
('marja', 'MID'),
('zata', 'JGL'),
('sinestrea', 'JGL'),
('y-bneth', 'SUP'),
('billow', 'JGL'),
('wisp', 'ADL'),
('murad', 'JGL'),
('violet', 'ADL'),
('lorion', 'MID'),
('ryoma', 'DSL'),
('annette', 'SUP'),
('volkath', 'DSL'),
('qi', 'DSL'),
('enzo', 'JGL'),
('moren', 'ADL'),
('gildur', 'MID'),
('florentino', 'DSL'),
('aya', 'SUP'),
('airi', 'DSL'),
('dolia', 'SUP'),
('yue', 'MID'),
('tachi', 'DSL'),
('nakroth', 'JGL'),
('zephys', 'DSL'),
('flowborn-mage', 'MID'),
('taara', 'SUP'),
('fennik', 'ADL'),
('elsu', 'ADL'),
('stuart', 'ADL'),
('teemee', 'SUP'),
('azzen-ka', 'MID'),
('ngo-khong', 'JGL'),
('rouie', 'SUP'),
('skud', 'DSL'),
('capheny', 'ADL'),
('liliana', 'MID'),
('flowborn-marksman', 'ADL'),
('yena', 'DSL'),
('goverra', 'MID'),
('dyadia', 'SUP'),
('mina', 'SUP'),
('cresht', 'SUP'),
('natalya', 'MID'),
('aleister', 'MID'),
('eland-orr', 'ADL'),
('grakk', 'SUP'),
('preyta', 'MID'),
('edras', 'DSL'),
('arthur', 'DSL'),
('tel-annas', 'ADL'),
('iggy', 'MID'),
('alice', 'SUP'),
('omen', 'DSL'),
('bolt-baron', 'DSL'),
('astrid', 'DSL'),
('lumburr', 'SUP'),
('keera', 'JGL'),
('trieu-van', 'DSL'),
('aoi', 'JGL'),
('heino', 'MID'),
('roxie', 'SUP'),
('veera', 'MID'),
('zuka', 'DSL'),
('quillen', 'JGL'),
('butterfly', 'JGL'),
('mganga', 'MID'),
('chaugnar', 'SUP'),
('arum', 'SUP'),
('rourka', 'DSL'),
('helen', 'SUP'),
('zip', 'SUP'),
('thane', 'SUP'),
('biron', 'DSL'),
('thorne', 'ADL'),
('krizzix', 'SUP'),
('valhein', 'ADL'),
('maloch', 'DSL'),
('dirak', 'MID'),
('kaine', 'JGL'),
('ming', 'SUP'),
('jinna', 'MID'),
('wiro', 'SUP'),
('slimz', 'ADL'),
('richter', 'DSL'),
('tulen', 'MID'),
('yan', 'DSL'),
('bijan', 'DSL'),
('ignis', 'MID'),
('max', 'SUP'),
('laville', 'ADL'),
('veres', 'DSL'),
('yorn', 'ADL'),
('lauriel', 'MID'),
('ilumia', 'MID'),
('raz', 'JGL'),
('wonder-woman', 'DSL'),
('xeniel', 'SUP'),
('dieu-thuyen', 'MID'),
('arduin', 'SUP'),
('allain', 'DSL'),
('erin', 'ADL'),
('baldum', 'SUP'),
('krixi', 'MID'),
('kil-groth', 'DSL'),
('errol', 'DSL'),
('kriknak', 'JGL'),
('sephera', 'MID'),
('lu-bu', 'DSL'),
('teeri', 'ADL'),
('bonnie', 'MID'),
('charlotte', 'DSL'),
('kahlii', 'MID'),
('bright', 'JGL'),
('amily', 'DSL'),
('ormarr', 'SUP'),
('zill', 'JGL'),
('omega', 'SUP'),
('lindis', 'ADL'),
('ata', 'SUP'),
('paine', 'JGL'),
('dextra', 'DSL'),
('d-arcy', 'MID'),
('ishar', 'MID'),
('celica', 'ADL'),
('the-flash', 'JGL'),
('superman', 'DSL');

UPDATE heroes h
JOIN tmp_primary_role_backfill seed ON seed.slug = h.slug
JOIN hero_roles r ON r.code = seed.lane_code
SET h.primary_role_id = r.id
WHERE h.primary_role_id IS NULL;

UPDATE heroes h
JOIN hero_roles r ON r.code = 'JGL'
SET h.primary_role_id = r.id
WHERE h.slug = 'billow';

UPDATE heroes h
JOIN (
    SELECT hero_id, MIN(role_id) AS role_id
    FROM hero_role_mapping
    GROUP BY hero_id
) legacy ON legacy.hero_id = h.id
SET h.primary_role_id = legacy.role_id
WHERE h.primary_role_id IS NULL;

DELETE hrm
FROM hero_role_mapping hrm
JOIN heroes h ON h.id = hrm.hero_id
WHERE h.primary_role_id IS NOT NULL
  AND hrm.role_id = h.primary_role_id;

INSERT IGNORE INTO hero_role_mapping (hero_id, role_id)
SELECT h.id, r.id
FROM heroes h
JOIN hero_roles r ON r.code = 'DSL'
WHERE h.slug = 'billow'
  AND h.primary_role_id IS NOT NULL
  AND h.primary_role_id <> r.id;

SELECT 'heroes_without_primary_role' AS check_name, COUNT(*) AS count_value
FROM heroes
WHERE primary_role_id IS NULL;

SELECT 'sub_roles_matching_primary_role' AS check_name, COUNT(*) AS count_value
FROM hero_role_mapping hrm
JOIN heroes h ON h.id = hrm.hero_id
WHERE h.primary_role_id = hrm.role_id;

SELECT h.slug, h.name, primary_role.code AS primary_role, GROUP_CONCAT(sub_role.code ORDER BY sub_role.code) AS sub_roles
FROM heroes h
LEFT JOIN hero_roles primary_role ON primary_role.id = h.primary_role_id
LEFT JOIN hero_role_mapping hrm ON hrm.hero_id = h.id
LEFT JOIN hero_roles sub_role ON sub_role.id = hrm.role_id
WHERE h.slug = 'billow'
GROUP BY h.id, h.slug, h.name, primary_role.code;
