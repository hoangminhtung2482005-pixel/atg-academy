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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "huong_dan_ngoc",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_huong_dan_ngoc",
                        columnNames = {"huong_dan_id", "bang_ngoc_id"}
                )
        },
        indexes = {
                @Index(name = "idx_huong_dan_ngoc_huong_dan", columnList = "huong_dan_id"),
                @Index(name = "idx_huong_dan_ngoc_bang_ngoc", columnList = "bang_ngoc_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuideArcana {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "huong_dan_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Guide huongDan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bang_ngoc_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Arcana bangNgoc;

    @Column(name = "so_luong", nullable = false)
    private Integer soLuong = 1;

    @Column(name = "thu_tu", nullable = false)
    private Integer thuTu = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.soLuong == null) {
            this.soLuong = 1;
        }
        if (this.thuTu == null) {
            this.thuTu = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
