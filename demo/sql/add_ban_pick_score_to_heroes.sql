-- Add ban_pick_score column for hero Ban/Pick evaluation.
-- Safe to rerun: only fills score for heroes where ban_pick_score is currently NULL.

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'heroes'
      AND COLUMN_NAME = 'ban_pick_score'
);
SET @ddl := IF(
    @column_exists = 0,
    'ALTER TABLE heroes ADD COLUMN ban_pick_score DECIMAL(5,2) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DROP TEMPORARY TABLE IF EXISTS tmp_hero_ban_pick_scores;
CREATE TEMPORARY TABLE tmp_hero_ban_pick_scores (
    slug VARCHAR(140) NOT NULL PRIMARY KEY,
    ban_pick_score DECIMAL(5,2) NOT NULL
) ENGINE=Memory;

INSERT INTO tmp_hero_ban_pick_scores (slug, ban_pick_score) VALUES
('hayate', 10.00),
('marja', 9.84),
('sinestrea', 9.92),
('toro', 9.76),
('zata', 9.69),
('y-bneth', 9.45),
('billow', 9.61),
('volkath', 9.53),
('violet', 9.37),
('wisp', 9.29),
('annette', 9.21),
('murad', 9.13),
('lorion', 9.06),
('gildur', 8.90),
('moren', 8.98),
('airi', 8.82),
('florentino', 8.74),
('ryoma', 8.66),
('enzo', 8.58),
('qi', 8.50),
('aya', 8.43),
('taara', 8.35),
('dolia', 8.19),
('yue', 8.27),
('zephys', 8.11),
('nakroth', 7.95),
('tachi', 8.03),
('teemee', 7.87),
('grakk', 7.80),
('stuart', 7.48),
('fennik', 7.64),
('rouie', 7.72),
('mina', 7.32),
('elsu', 7.24),
('azzen-ka', 6.93),
('skud', 7.09),
('yena', 7.40),
('roxie', 6.85),
('eland-orr', 7.01),
('bolt-baron', 7.17),
('liliana', 6.69),
('iggy', 6.61),
('trieu-van', 6.77),
('zanis', 6.77),
('astrid', 7.56),
('flowborn-marksman', 6.54),
('aoi', 6.38),
('capheny', 6.46),
('cresht', 6.22),
('omen', 6.14),
('goverra', 5.83),
('chaugnar', 6.30),
('helen', 5.91),
('edras', 5.98),
('dyadia', 5.59),
('zuka', 6.06),
('natalya', 5.43),
('tel-annas', 5.75),
('krizzix', 5.67),
('thorne', 5.12),
('arthur', 5.51),
('flowborn-mage', 5.35),
('butterfly', 5.28),
('heino', 5.04),
('thane', 5.20),
('mganga', 4.96),
('ngo-khong', 4.88),
('wukong', 4.88),
('tulen', 4.80),
('keera', 4.72),
('aleister', 4.65),
('slimz', 4.49),
('preyta', 4.41),
('max', 4.33),
('wonder-woman', 4.57),
('zip', 4.25),
('alice', 4.17),
('ming', 4.02),
('lumburr', 3.94),
('veres', 3.86),
('valhein', 4.09),
('maloch', 3.70),
('richter', 3.07),
('riktor', 3.07),
('laville', 3.54),
('veera', 3.39),
('ilumia', 3.62),
('quillen', 3.31),
('ignis', 3.15),
('wiro', 3.46),
('biron', 3.23),
('kriknak', 2.99),
('arum', 2.91),
('rourke', 2.83),
('rourka', 2.83),
('yan', 3.78),
('dieu-thuyen', 2.36),
('diaochan', 2.36),
('bright', 2.52),
('kaine', 2.60),
('dirak', 2.60),
('errol', 2.76),
('jinna', 2.44),
('bijan', 2.28),
('kil-groth', 2.20),
('ormarr', 1.73),
('charlotte', 1.65),
('paine', 1.97),
('raz', 2.05),
('yorn', 1.81),
('lauriel', 1.57),
('sephera', 1.50),
('xeniel', 1.89),
('arduin', 2.13),
('lu-bu', 1.34),
('erin', 1.42),
('kahlii', 1.26),
('allain', 1.18),
('krixi', 1.02),
('zill', 0.87),
('baldum', 1.10),
('ata', 0.94),
('teeri', 0.79),
('bonnie', 0.71),
('omega', 0.55),
('amily', 0.63),
('lindis', 0.47),
('dextra', 0.39),
('d-arcy', 0.31),
('ishar', 0.24),
('celica', 0.16),
('the-flash', 0.08),
('superman', 0.00);

UPDATE heroes h
JOIN tmp_hero_ban_pick_scores seed ON seed.slug = h.slug
SET h.ban_pick_score = seed.ban_pick_score
WHERE h.ban_pick_score IS NULL;

SELECT 'heroes_missing_ban_pick_score' AS check_name, COUNT(*) AS count_value
FROM heroes
WHERE ban_pick_score IS NULL;
