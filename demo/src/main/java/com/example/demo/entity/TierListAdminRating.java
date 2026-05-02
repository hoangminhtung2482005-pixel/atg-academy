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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tier_list_admin_ratings",
        uniqueConstraints = @UniqueConstraint(columnNames = "tier_list_id"),
        indexes = {
                @Index(name = "idx_tier_list_admin_ratings_admin", columnList = "admin_user_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TierListAdminRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_list_id", nullable = false)
    private TierList tierList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id", nullable = false)
    private User adminUser;

    @Column(nullable = false)
    private Double ratingValue;

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
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
