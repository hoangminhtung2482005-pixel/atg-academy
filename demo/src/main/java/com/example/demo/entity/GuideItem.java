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
        name = "huong_dan_vat_pham",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_huong_dan_vat_pham",
                        columnNames = {"huong_dan_id", "vat_pham_id"}
                )
        },
        indexes = {
                @Index(name = "idx_huong_dan_vat_pham_huong_dan", columnList = "huong_dan_id"),
                @Index(name = "idx_huong_dan_vat_pham_vat_pham", columnList = "vat_pham_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuideItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "huong_dan_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Guide huongDan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vat_pham_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Item vatPham;

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
        if (this.thuTu == null) {
            this.thuTu = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
