package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_saved_tier_lists",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_saved_tier_lists_user_tier",
                columnNames = {"user_id", "tier_list_id"}
        ),
        indexes = {
                @Index(name = "idx_user_saved_tier_lists_user", columnList = "user_id"),
                @Index(name = "idx_user_saved_tier_lists_tier_list", columnList = "tier_list_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSavedTierList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_list_id", nullable = false)
    private TierList tierList;

    @Column(nullable = false, updatable = false)
    private LocalDateTime savedAt;

    @PrePersist
    protected void onCreate() {
        if (this.savedAt == null) {
            this.savedAt = LocalDateTime.now();
        }
    }
}
