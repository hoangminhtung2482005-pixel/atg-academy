-- Tier List community detail/comments/ratings support.
-- The app currently uses spring.jpa.hibernate.ddl-auto=update, so these are
-- production migration notes for environments where DDL is managed manually.

ALTER TABLE tier_lists
    ADD COLUMN description TEXT NULL;

ALTER TABLE tier_list_ratings
    ADD COLUMN updated_at DATETIME NULL;

UPDATE tier_list_ratings
SET updated_at = created_at
WHERE updated_at IS NULL;

CREATE TABLE tier_list_comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tier_list_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (id),
    INDEX idx_tier_list_comments_tier_list (tier_list_id),
    INDEX idx_tier_list_comments_user (user_id),
    CONSTRAINT fk_tier_list_comments_tier_list
        FOREIGN KEY (tier_list_id) REFERENCES tier_lists (id),
    CONSTRAINT fk_tier_list_comments_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE tier_list_admin_ratings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tier_list_id BIGINT NOT NULL,
    admin_user_id BIGINT NOT NULL,
    rating_value DOUBLE NOT NULL,
    note TEXT NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tier_list_admin_ratings_tier_list (tier_list_id),
    INDEX idx_tier_list_admin_ratings_admin (admin_user_id),
    CONSTRAINT fk_tier_list_admin_ratings_tier_list
        FOREIGN KEY (tier_list_id) REFERENCES tier_lists (id),
    CONSTRAINT fk_tier_list_admin_ratings_admin
        FOREIGN KEY (admin_user_id) REFERENCES users (id)
);

-- Optional backfill from the older tier_lists.admin_rating column.
-- Choose the admin user that should own the migrated admin ratings first.
-- Replace :admin_user_id with that users.id.
--
-- INSERT INTO tier_list_admin_ratings
--     (tier_list_id, admin_user_id, rating_value, created_at, updated_at)
-- SELECT id, :admin_user_id, admin_rating, updated_at, updated_at
-- FROM tier_lists
-- WHERE admin_rating IS NOT NULL
-- ON DUPLICATE KEY UPDATE
--     rating_value = VALUES(rating_value),
--     updated_at = VALUES(updated_at);
