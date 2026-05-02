package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tier_lists")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TierList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    /** Lưu toàn bộ cấu trúc bảng Tier List dưới dạng JSON string */
    @Column(columnDefinition = "TEXT")
    private String contentData;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Cờ đánh dấu đây là Tier List chính thức của Admin */
    @Column(nullable = false)
    private boolean isOfficial = false;

    /** Admin đánh giá endorsement (1-5 sao, nullable) */
    private Integer adminRating;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
