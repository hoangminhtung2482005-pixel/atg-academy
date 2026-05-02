package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "guides",
        indexes = {
                @Index(name = "idx_guides_hero_status_published", columnList = "hero_id, status, published_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Guide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String coverImageUrl;

    @Column(length = 30)
    private String status = "PUBLISHED";

    private String category;

    private String lane;

    @Column(columnDefinition = "TEXT")
    private String excerpt;

    private Integer viewCount = 0;

    private Integer readingTimeMinutes = 5;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hero_id")
    private Hero hero;

    /** Lưu toàn bộ nội dung giáo án dưới dạng JSON string */
    @Column(columnDefinition = "TEXT")
    private String contentData;

    @OneToMany(mappedBy = "huongDan", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<GuideItem> vatPhams = new HashSet<>();

    @OneToMany(mappedBy = "huongDan", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<GuideArcana> bangNgocs = new HashSet<>();

    @OneToMany(mappedBy = "huongDan", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<GuideEnchantment> phuHieus = new HashSet<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime publishedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null || this.status.isBlank()) {
            this.status = "PUBLISHED";
        }
        if (this.viewCount == null) {
            this.viewCount = 0;
        }
        if (this.readingTimeMinutes == null || this.readingTimeMinutes < 1) {
            this.readingTimeMinutes = 5;
        }
        if ("PUBLISHED".equalsIgnoreCase(this.status) && this.publishedAt == null) {
            this.publishedAt = this.createdAt;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.viewCount == null) {
            this.viewCount = 0;
        }
        if (this.readingTimeMinutes == null || this.readingTimeMinutes < 1) {
            this.readingTimeMinutes = 5;
        }
        if ("PUBLISHED".equalsIgnoreCase(this.status) && this.publishedAt == null) {
            this.publishedAt = this.updatedAt;
        }
    }
}
