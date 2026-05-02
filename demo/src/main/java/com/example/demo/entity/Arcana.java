package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
        name = "bang_ngoc",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bang_ngoc_slug", columnNames = "slug")
        },
        indexes = {
                @Index(name = "idx_bang_ngoc_slug", columnList = "slug")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Arcana {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slug", nullable = false, length = 160)
    private String slug;

    @Column(name = "ten", nullable = false, length = 160)
    private String ten;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "mau", nullable = false, length = 40)
    private String mau;

    @Column(name = "cap_do")
    private Integer capDo;

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
