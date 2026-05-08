UPDATE spells
SET name = 'Bộc phá',
    slug = 'boc-pha',
    icon_url = '/images/spells/boc-pha.png',
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP(6)),
    updated_at = CURRENT_TIMESTAMP(6)
WHERE slug = 'boc-pha' OR name = 'Bộc phá' OR name = 'Bộc Phá';

UPDATE spells
SET name = 'Cấp cứu',
    slug = 'cap-cuu',
    icon_url = '/images/spells/cap-cuu.png',
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP(6)),
    updated_at = CURRENT_TIMESTAMP(6)
WHERE slug = 'cap-cuu' OR name = 'Cấp cứu' OR name = 'Cấp Cứu';

UPDATE spells
SET name = 'Gầm thét',
    slug = 'gam-thet',
    icon_url = '/images/spells/gam-thet.png',
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP(6)),
    updated_at = CURRENT_TIMESTAMP(6)
WHERE slug = 'gam-thet' OR name = 'Gầm thét' OR name = 'Gầm Thét';

UPDATE spells
SET name = 'Lá chắn sinh mệnh',
    slug = 'la-chan-sinh-menh',
    icon_url = '/images/spells/la-chan-sinh-menh.png',
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP(6)),
    updated_at = CURRENT_TIMESTAMP(6)
WHERE slug = 'la-chan-sinh-menh' OR name = 'Lá chắn sinh mệnh' OR name = 'Lá Chắn Sinh Mệnh';

UPDATE spells
SET name = 'Ngất ngư',
    slug = 'ngat-ngu',
    icon_url = '/images/spells/ngat-ngu.png',
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP(6)),
    updated_at = CURRENT_TIMESTAMP(6)
WHERE slug = 'ngat-ngu' OR name = 'Ngất ngư' OR name = 'Ngất Ngư';

UPDATE spells
SET name = 'Suy nhược',
    slug = 'suy-nhuoc',
    icon_url = '/images/spells/suy-nhuoc.png',
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP(6)),
    updated_at = CURRENT_TIMESTAMP(6)
WHERE slug = 'suy-nhuoc' OR name = 'Suy nhược' OR name = 'Suy Nhược';

UPDATE spells
SET name = 'Thanh tẩy',
    slug = 'thanh-tay',
    icon_url = '/images/spells/thanh-tay.png',
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP(6)),
    updated_at = CURRENT_TIMESTAMP(6)
WHERE slug = 'thanh-tay' OR name = 'Thanh tẩy' OR name = 'Thanh Tẩy';

UPDATE spells
SET name = 'Tốc biến',
    slug = 'toc-bien',
    icon_url = '/images/spells/toc-bien.png',
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP(6)),
    updated_at = CURRENT_TIMESTAMP(6)
WHERE slug = 'toc-bien' OR name = 'Tốc biến' OR name = 'Tốc Biến';

UPDATE spells
SET name = 'Tốc hành',
    slug = 'toc-hanh',
    icon_url = '/images/spells/toc-hanh.png',
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP(6)),
    updated_at = CURRENT_TIMESTAMP(6)
WHERE slug = 'toc-hanh' OR name = 'Tốc hành' OR name = 'Tốc Hành';

UPDATE spells
SET name = 'Trừng trị',
    slug = 'trung-tri',
    icon_url = '/images/spells/trung-tri.png',
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP(6)),
    updated_at = CURRENT_TIMESTAMP(6)
WHERE slug = 'trung-tri' OR name = 'Trừng trị' OR name = 'Trừng Trị';

UPDATE spells
SET name = 'Tự bạo bổn',
    slug = 'tu-bao-bon',
    icon_url = '/images/spells/tu-bao-bon.png',
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP(6)),
    updated_at = CURRENT_TIMESTAMP(6)
WHERE slug = 'tu-bao-bon' OR name = 'Tự bạo bổn' OR name = 'Tụ Bảo Bồn';

UPDATE spells
SET name = 'Viện binh liên hiệp',
    slug = 'vien-binh-lien-hiep',
    icon_url = '/images/spells/vien-binh-lien-hiep.png',
    created_at = COALESCE(created_at, CURRENT_TIMESTAMP(6)),
    updated_at = CURRENT_TIMESTAMP(6)
WHERE slug = 'vien-binh-lien-hiep' OR name = 'Viện binh liên hiệp' OR name = 'Viện Binh Liên Hiệp';

INSERT INTO spells (name, slug, icon_url, created_at, updated_at)
VALUES
    ('Bộc phá', 'boc-pha', '/images/spells/boc-pha.png', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('Cấp cứu', 'cap-cuu', '/images/spells/cap-cuu.png', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('Gầm thét', 'gam-thet', '/images/spells/gam-thet.png', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('Lá chắn sinh mệnh', 'la-chan-sinh-menh', '/images/spells/la-chan-sinh-menh.png', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('Ngất ngư', 'ngat-ngu', '/images/spells/ngat-ngu.png', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('Suy nhược', 'suy-nhuoc', '/images/spells/suy-nhuoc.png', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('Thanh tẩy', 'thanh-tay', '/images/spells/thanh-tay.png', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('Tốc biến', 'toc-bien', '/images/spells/toc-bien.png', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('Tốc hành', 'toc-hanh', '/images/spells/toc-hanh.png', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('Trừng trị', 'trung-tri', '/images/spells/trung-tri.png', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('Tự bạo bổn', 'tu-bao-bon', '/images/spells/tu-bao-bon.png', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('Viện binh liên hiệp', 'vien-binh-lien-hiep', '/images/spells/vien-binh-lien-hiep.png', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    icon_url = VALUES(icon_url),
    updated_at = CURRENT_TIMESTAMP(6);
