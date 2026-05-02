package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tier_list_ratings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tier_list_id", "user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TierListRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_list_id", nullable = false)
    private TierList tierList;

    /** Email Google của người đánh giá */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /** Số sao đánh giá (1-5) */
    @Column(nullable = false)
    private Integer stars;

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

    public Integer getRatingValue() {
        return stars;
    }

    public void setRatingValue(Integer ratingValue) {
        this.stars = ratingValue;
    }
}
