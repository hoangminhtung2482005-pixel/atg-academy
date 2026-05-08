-- ===========================================================================
-- ATG Academy — Wiki Hero Database Schema Redesign
-- DDL + Seed Data for MySQL
--
-- Chạy file này SAU KHI đã restart Spring Boot (để JPA tạo bảng tự động),
-- HOẶC chạy trực tiếp nếu muốn tạo bảng thủ công.
--
-- Thứ tự: DDL → Seed Roles → Seed Attributes → Migrate heroes → Mappings
-- ===========================================================================

-- =====================
-- 1. DDL: Tạo bảng mới
-- =====================

-- Bảng Vị trí đi đường (5 giá trị cố định)
CREATE TABLE IF NOT EXISTS hero_roles (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10)  NOT NULL UNIQUE,
    name VARCHAR(50)  NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bảng Đặc điểm tướng (admin có thể thêm bớt)
CREATE TABLE IF NOT EXISTS hero_attributes (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bảng trung gian Hero ↔ Role (N-N)
-- Phuong an A: this mapping table is for sub roles only.
CREATE TABLE IF NOT EXISTS hero_role_mapping (
    hero_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (hero_id, role_id),
    FOREIGN KEY (hero_id) REFERENCES heroes(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES hero_roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bảng trung gian Hero ↔ Attribute (N-N)
CREATE TABLE IF NOT EXISTS hero_attribute_mapping (
    hero_id      BIGINT NOT NULL,
    attribute_id BIGINT NOT NULL,
    PRIMARY KEY (hero_id, attribute_id),
    FOREIGN KEY (hero_id)      REFERENCES heroes(id) ON DELETE CASCADE,
    FOREIGN KEY (attribute_id) REFERENCES hero_attributes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Thêm cột hero_class nếu chưa tồn tại
-- (Spring JPA ddl-auto=update sẽ tự thêm, nhưng chạy thủ công cũng an toàn)
ALTER TABLE heroes ADD COLUMN IF NOT EXISTS hero_class VARCHAR(30) DEFAULT NULL;
ALTER TABLE heroes ADD COLUMN IF NOT EXISTS primary_role_id BIGINT DEFAULT NULL;
ALTER TABLE heroes ADD COLUMN IF NOT EXISTS ban_pick_score DECIMAL(5,2) DEFAULT NULL;

-- =============================
-- 2. Seed: 5 Vị trí đi đường
-- =============================
INSERT INTO hero_roles (code, name) VALUES
('DSL', 'Đường Solo Lẻ'),
('JGL', 'Đi Rừng'),
('MID', 'Đường Giữa'),
('ADL', 'Xạ Thủ'),
('SUP', 'Hỗ Trợ')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- ==================================
-- 3. Seed: Đặc điểm tướng (mẫu)
-- ==================================
INSERT INTO hero_attributes (name) VALUES
('Cơ động'),
('Chống chịu'),
('Khống chế'),
('Cấu rỉa'),
('Hồi phục'),
('Giáp ảo'),
('Sát thương sốc cao'),
('Sát thương liên tục'),
('Đánh xa'),
('Hỗ trợ đồng đội'),
('Phản dame'),
('Xuyên giáp'),
('Né đòn'),
('Triệu hồi'),
('Kiểm soát vùng'),
('Combo burst')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- =========================================================================
-- 4. Migrate cột `roles` cũ → `hero_class` (nếu bạn đã có data cũ)
-- =========================================================================
-- Câu lệnh dưới đây copy giá trị từ cột `roles` (string cũ) sang `hero_class`
-- cho những hero chưa có hero_class:
UPDATE heroes SET hero_class = roles WHERE hero_class IS NULL AND roles IS NOT NULL;

-- =========================================================================
-- 5. Ví dụ mapping tướng ↔ vị trí đi đường
--    (Chạy SAU KHI bảng heroes đã có data)
-- =========================================================================
-- Ví dụ: Violet có thể đi ADL + JGL
-- INSERT INTO hero_role_mapping (hero_id, role_id)
-- SELECT h.id, r.id FROM heroes h, hero_roles r
-- WHERE h.name = 'Violet' AND r.code IN ('ADL', 'JGL')
-- ON DUPLICATE KEY UPDATE hero_id = hero_id;

-- Ví dụ: Butterfly đi JGL + DSL
-- INSERT INTO hero_role_mapping (hero_id, role_id)
-- SELECT h.id, r.id FROM heroes h, hero_roles r
-- WHERE h.name = 'Butterfly' AND r.code IN ('JGL', 'DSL')
-- ON DUPLICATE KEY UPDATE hero_id = hero_id;

-- Ví dụ: Violet có đặc điểm Cơ động + Sát thương sốc cao
-- INSERT INTO hero_attribute_mapping (hero_id, attribute_id)
-- SELECT h.id, a.id FROM heroes h, hero_attributes a
-- WHERE h.name = 'Violet' AND a.name IN ('Cơ động', 'Sát thương sốc cao')
-- ON DUPLICATE KEY UPDATE hero_id = hero_id;

-- =========================================================================
-- Kiểm tra dữ liệu:
-- =========================================================================
-- SELECT * FROM hero_roles;
-- SELECT * FROM hero_attributes;
-- SELECT h.id, h.name, h.hero_class, GROUP_CONCAT(r.name) AS lane_roles
-- FROM heroes h
-- LEFT JOIN hero_role_mapping hrm ON h.id = hrm.hero_id
-- LEFT JOIN hero_roles r ON hrm.role_id = r.id
-- GROUP BY h.id ORDER BY h.name LIMIT 10;
