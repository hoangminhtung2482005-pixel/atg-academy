-- ============================================================================
-- ATG Academy Wiki Hero Migration
-- Multi-class heroes + attribute admin metadata
-- ============================================================================
-- Goal:
-- 1. Create hero_classes and hero_class_mapping
-- 2. Extend hero_attributes with admin-editable metadata
-- 3. Preserve legacy heroes.hero_class temporarily
-- 4. Backfill mappings from heroes.hero_class
-- 5. Verify every hero has at least one class
--
-- Notes:
-- - This file is the production-safe migration guide.
-- - Do not rely on spring.jpa.hibernate.ddl-auto=update for production rollout.
-- - Keep heroes.hero_class until every consumer has fully migrated.
-- ============================================================================

START TRANSACTION;

CREATE TABLE IF NOT EXISTS hero_classes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(50) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hero_class_mapping (
    hero_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    PRIMARY KEY (hero_id, class_id),
    CONSTRAINT fk_hero_class_mapping_hero
        FOREIGN KEY (hero_id) REFERENCES heroes(id) ON DELETE CASCADE,
    CONSTRAINT fk_hero_class_mapping_class
        FOREIGN KEY (class_id) REFERENCES hero_classes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE hero_attributes
    ADD COLUMN IF NOT EXISTS description TEXT NULL,
    ADD COLUMN IF NOT EXISTS icon_url VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS sort_order INT NULL;

INSERT INTO hero_classes (name, display_name) VALUES
('Đấu sĩ', 'Đấu sĩ'),
('Sát thủ', 'Sát thủ'),
('Pháp sư', 'Pháp sư'),
('Xạ thủ', 'Xạ thủ'),
('Đỡ đòn', 'Đỡ đòn'),
('Trợ thủ', 'Trợ thủ')
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name);

-- Backfill hero -> class mappings from the legacy single-value heroes.hero_class column.
INSERT IGNORE INTO hero_class_mapping (hero_id, class_id)
SELECT h.id, hc.id
FROM heroes h
JOIN hero_classes hc
    ON hc.name = h.hero_class
WHERE h.hero_class IS NOT NULL
  AND h.hero_class <> '';

COMMIT;

-- ============================================================================
-- Verification
-- ============================================================================

-- 1. Heroes without a mapped class after migration
SELECT h.id, h.name, h.slug, h.hero_class
FROM heroes h
LEFT JOIN hero_class_mapping hcm ON hcm.hero_id = h.id
WHERE hcm.hero_id IS NULL
ORDER BY h.name;

-- 2. Duplicate hero-class mappings (should return zero rows)
SELECT hero_id, class_id, COUNT(*) AS duplicate_count
FROM hero_class_mapping
GROUP BY hero_id, class_id
HAVING COUNT(*) > 1;

-- 3. Current hero class catalog
SELECT id, name, display_name
FROM hero_classes
ORDER BY id;

-- 4. Attribute usage count
SELECT
    a.id,
    a.name,
    a.sort_order,
    COUNT(ham.hero_id) AS hero_usage_count
FROM hero_attributes a
LEFT JOIN hero_attribute_mapping ham ON ham.attribute_id = a.id
GROUP BY a.id, a.name, a.sort_order
ORDER BY COALESCE(a.sort_order, 2147483647), a.name;

-- 5. Optional: preview heroes with all mapped classes
SELECT
    h.id,
    h.name,
    h.slug,
    h.hero_class AS legacy_primary_class,
    GROUP_CONCAT(COALESCE(hc.display_name, hc.name) ORDER BY hc.name SEPARATOR ', ') AS mapped_classes
FROM heroes h
LEFT JOIN hero_class_mapping hcm ON hcm.hero_id = h.id
LEFT JOIN hero_classes hc ON hc.id = hcm.class_id
GROUP BY h.id, h.name, h.slug, h.hero_class
ORDER BY h.name;

-- 6. Optional later cleanup, only after every consumer no longer depends on heroes.hero_class:
-- ALTER TABLE heroes DROP COLUMN hero_class;
