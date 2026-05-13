-- Legacy Arcana/Item tables (`arcanas`, `bang_ngoc`, `huong_dan_ngoc`,
-- `items`, `vat_pham`, `huong_dan_vat_pham`) were retired from runtime.
-- Use demo/sql/cleanup_legacy_unused_tables.sql for backup/FK review + drop.

CREATE TABLE IF NOT EXISTS phu_hieu (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slug VARCHAR(160) NOT NULL,
    ten VARCHAR(160) NOT NULL,
    mo_ta TEXT NULL,
    nhanh VARCHAR(80) NULL,
    cap_do INT NULL,
    anh_url VARCHAR(500) NULL,
    chi_so_json JSON NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_phu_hieu_slug UNIQUE (slug),
    INDEX idx_phu_hieu_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS huong_dan_phu_hieu (
    id BIGINT NOT NULL AUTO_INCREMENT,
    huong_dan_id BIGINT NOT NULL,
    phu_hieu_id BIGINT NOT NULL,
    thu_tu INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_huong_dan_phu_hieu UNIQUE (huong_dan_id, phu_hieu_id),
    CONSTRAINT fk_huong_dan_phu_hieu_huong_dan
        FOREIGN KEY (huong_dan_id) REFERENCES guides (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_huong_dan_phu_hieu_phu_hieu
        FOREIGN KEY (phu_hieu_id) REFERENCES phu_hieu (id)
        ON DELETE CASCADE,
    INDEX idx_huong_dan_phu_hieu_huong_dan (huong_dan_id),
    INDEX idx_huong_dan_phu_hieu_phu_hieu (phu_hieu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
