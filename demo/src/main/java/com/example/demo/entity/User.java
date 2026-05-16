package com.example.demo.entity;

import com.example.demo.support.PlayerCardDefaults;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(length = 80)
    private String displayName;

    private String avatarUrl;

    @Column(nullable = false)
    private String role;

    @Column(length = 20)
    private String level = "Normal";

    @Column(length = 40)
    private String playerBadgeCode = PlayerCardDefaults.DEFAULT_BADGE_CODE;

    @Column(length = 80)
    private String playerBadgeName = PlayerCardDefaults.DEFAULT_BADGE_NAME;

    @Column(length = 500)
    private String playerBadgeIconUrl;

    @Column(length = 120)
    private String playerTitle = PlayerCardDefaults.DEFAULT_TITLE;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String note;

    private LocalDateTime banPickCooldownUntil;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = UserStatus.ACTIVE;
        }
        if (this.role == null || this.role.isBlank()) {
            this.role = UserRole.USER.getStorageValue();
        }
        if (this.level == null || this.level.isBlank()) {
            this.level = "Normal";
        }
        applyPlayerCardDefaults();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = UserStatus.ACTIVE;
        }
        if (this.level == null || this.level.isBlank()) {
            this.level = "Normal";
        }
        if (this.playerBadgeIconUrl != null && this.playerBadgeIconUrl.isBlank()) {
            this.playerBadgeIconUrl = null;
        }
    }

    public String resolveDisplayName() {
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName.trim();
        }
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        return "User";
    }

    public String resolveLevel() {
        if (level != null && !level.trim().isEmpty()) {
            return level.trim();
        }
        return "Normal";
    }

    public String resolvePlayerBadgeCode() {
        if (playerBadgeCode != null && !playerBadgeCode.trim().isEmpty()) {
            return playerBadgeCode.trim();
        }
        return PlayerCardDefaults.DEFAULT_BADGE_CODE;
    }

    public String resolvePlayerBadgeName() {
        if (playerBadgeName != null && !playerBadgeName.trim().isEmpty()) {
            return playerBadgeName.trim();
        }
        return PlayerCardDefaults.resolvePreset(playerBadgeCode).name();
    }

    public String resolvePlayerBadgeIconUrl() {
        if (playerBadgeIconUrl != null && !playerBadgeIconUrl.trim().isEmpty()) {
            return playerBadgeIconUrl.trim();
        }
        return PlayerCardDefaults.resolvePreset(playerBadgeCode).iconUrl();
    }

    public String resolvePlayerTitle() {
        if (playerTitle != null && !playerTitle.trim().isEmpty()) {
            return playerTitle.trim();
        }
        return PlayerCardDefaults.DEFAULT_TITLE;
    }

    private void applyPlayerCardDefaults() {
        if (this.playerBadgeCode == null || this.playerBadgeCode.isBlank()) {
            this.playerBadgeCode = PlayerCardDefaults.DEFAULT_BADGE_CODE;
        }
        if (this.playerBadgeName == null || this.playerBadgeName.isBlank()) {
            this.playerBadgeName = PlayerCardDefaults.resolvePreset(this.playerBadgeCode).name();
        }
        if (this.playerTitle == null || this.playerTitle.isBlank()) {
            this.playerTitle = PlayerCardDefaults.DEFAULT_TITLE;
        }
        if (this.playerBadgeIconUrl != null && this.playerBadgeIconUrl.isBlank()) {
            this.playerBadgeIconUrl = null;
        }
    }
}
