package com.example.demo.entity;

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

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String note;

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
}
