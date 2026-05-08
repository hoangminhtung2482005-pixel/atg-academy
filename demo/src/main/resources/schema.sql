SET @schema_name = DATABASE();

SET @bo_tro_exists = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = @schema_name
      AND table_name = 'bo_tro'
);

SET @spells_exists = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = @schema_name
      AND table_name = 'spells'
);

SET @rename_table_sql = IF(
    @bo_tro_exists = 1 AND @spells_exists = 0,
    'RENAME TABLE bo_tro TO spells',
    'SELECT 1'
);
PREPARE stmt FROM @rename_table_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS spells (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(160) NOT NULL,
    slug VARCHAR(160) NOT NULL,
    icon_url VARCHAR(500) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_spells_slug UNIQUE (slug),
    INDEX idx_spells_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @has_name = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'spells'
      AND column_name = 'name'
);
SET @has_ten = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'spells'
      AND column_name = 'ten'
);
SET @rename_name_sql = IF(
    @has_name = 0 AND @has_ten = 1,
    'ALTER TABLE spells CHANGE COLUMN ten name VARCHAR(160) NOT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @rename_name_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_name_sql = IF(
    @has_name = 0 AND @has_ten = 0,
    'ALTER TABLE spells ADD COLUMN name VARCHAR(160) NULL AFTER id',
    'SELECT 1'
);
PREPARE stmt FROM @add_name_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_icon_url = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'spells'
      AND column_name = 'icon_url'
);
SET @has_anh_url = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'spells'
      AND column_name = 'anh_url'
);
SET @rename_icon_sql = IF(
    @has_icon_url = 0 AND @has_anh_url = 1,
    'ALTER TABLE spells CHANGE COLUMN anh_url icon_url VARCHAR(500) NULL',
    'SELECT 1'
);
PREPARE stmt FROM @rename_icon_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_icon_sql = IF(
    @has_icon_url = 0 AND @has_anh_url = 0,
    'ALTER TABLE spells ADD COLUMN icon_url VARCHAR(500) NULL AFTER slug',
    'SELECT 1'
);
PREPARE stmt FROM @add_icon_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_slug = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'spells'
      AND column_name = 'slug'
);
SET @add_slug_sql = IF(
    @has_slug = 0,
    'ALTER TABLE spells ADD COLUMN slug VARCHAR(160) NULL AFTER name',
    'SELECT 1'
);
PREPARE stmt FROM @add_slug_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_created_at = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'spells'
      AND column_name = 'created_at'
);
SET @add_created_at_sql = IF(
    @has_created_at = 0,
    'ALTER TABLE spells ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) AFTER icon_url',
    'SELECT 1'
);
PREPARE stmt FROM @add_created_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_updated_at = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = @schema_name
      AND table_name = 'spells'
      AND column_name = 'updated_at'
);
SET @add_updated_at_sql = IF(
    @has_updated_at = 0,
    'ALTER TABLE spells ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) AFTER created_at',
    'SELECT 1'
);
PREPARE stmt FROM @add_updated_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_unique_slug = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'spells'
      AND index_name = 'uk_spells_slug'
);
SET @add_unique_slug_sql = IF(
    @has_unique_slug = 0,
    'ALTER TABLE spells ADD CONSTRAINT uk_spells_slug UNIQUE (slug)',
    'SELECT 1'
);
PREPARE stmt FROM @add_unique_slug_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_slug_index = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'spells'
      AND index_name = 'idx_spells_slug'
);
SET @add_slug_index_sql = IF(
    @has_slug_index = 0,
    'CREATE INDEX idx_spells_slug ON spells (slug)',
    'SELECT 1'
);
PREPARE stmt FROM @add_slug_index_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
