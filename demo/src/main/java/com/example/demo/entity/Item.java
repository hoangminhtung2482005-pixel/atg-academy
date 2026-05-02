package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "vat_pham",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_vat_pham_slug", columnNames = "slug")
        },
        indexes = {
                @Index(name = "idx_vat_pham_slug", columnList = "slug")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slug", nullable = false, length = 160)
    private String slug;

    @Column(name = "ten", nullable = false, length = 160)
    private String ten;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "loai", length = 80)
    private String loai;

    @Column(name = "gia_vang")
    private Integer giaVang;

    @Column(name = "anh_url", length = 500)
    private String anhUrl;

    @Column(name = "chi_so_json", columnDefinition = "JSON")
    private String chiSoJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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
