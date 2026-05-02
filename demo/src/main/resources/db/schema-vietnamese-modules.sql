CREATE TABLE IF NOT EXISTS vat_pham (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slug VARCHAR(160) NOT NULL,
    ten VARCHAR(160) NOT NULL,
    mo_ta TEXT NULL,
    loai VARCHAR(80) NULL,
    gia_vang INT NULL,
    anh_url VARCHAR(500) NULL,
    chi_so_json JSON NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_vat_pham_slug UNIQUE (slug),
    INDEX idx_vat_pham_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bang_ngoc (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slug VARCHAR(160) NOT NULL,
    ten VARCHAR(160) NOT NULL,
    mo_ta TEXT NULL,
    mau VARCHAR(40) NOT NULL,
    cap_do INT NULL,
    anh_url VARCHAR(500) NULL,
    chi_so_json JSON NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_bang_ngoc_slug UNIQUE (slug),
    INDEX idx_bang_ngoc_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

CREATE TABLE IF NOT EXISTS huong_dan_vat_pham (
    id BIGINT NOT NULL AUTO_INCREMENT,
    huong_dan_id BIGINT NOT NULL,
    vat_pham_id BIGINT NOT NULL,
    thu_tu INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_huong_dan_vat_pham UNIQUE (huong_dan_id, vat_pham_id),
    CONSTRAINT fk_huong_dan_vat_pham_huong_dan
        FOREIGN KEY (huong_dan_id) REFERENCES guides (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_huong_dan_vat_pham_vat_pham
        FOREIGN KEY (vat_pham_id) REFERENCES vat_pham (id)
        ON DELETE CASCADE,
    INDEX idx_huong_dan_vat_pham_huong_dan (huong_dan_id),
    INDEX idx_huong_dan_vat_pham_vat_pham (vat_pham_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS huong_dan_ngoc (
    id BIGINT NOT NULL AUTO_INCREMENT,
    huong_dan_id BIGINT NOT NULL,
    bang_ngoc_id BIGINT NOT NULL,
    so_luong INT NOT NULL DEFAULT 1,
    thu_tu INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_huong_dan_ngoc UNIQUE (huong_dan_id, bang_ngoc_id),
    CONSTRAINT fk_huong_dan_ngoc_huong_dan
        FOREIGN KEY (huong_dan_id) REFERENCES guides (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_huong_dan_ngoc_bang_ngoc
        FOREIGN KEY (bang_ngoc_id) REFERENCES bang_ngoc (id)
        ON DELETE CASCADE,
    INDEX idx_huong_dan_ngoc_huong_dan (huong_dan_id),
    INDEX idx_huong_dan_ngoc_bang_ngoc (bang_ngoc_id)
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
