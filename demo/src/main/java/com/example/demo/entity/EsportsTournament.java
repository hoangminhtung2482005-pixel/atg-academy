package com.example.demo.entity;

import com.example.demo.util.EsportsTierSupport;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "esports_tournaments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EsportsTournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "franchise_id", nullable = false)
    private EsportsFranchise franchise;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "season_year")
    private Integer seasonYear;

    @Column(name = "split_name", length = 100)
    private String splitName;

    @Column(name = "tier_level", length = 10)
    private String tierLevel;

    @Column(name = "aer_tier", nullable = false)
    private Integer aerTier = 1;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(length = 50)
    private String status = "UPCOMING";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null || status.isBlank()) {
            status = "UPCOMING";
        }
        normalizeAerTier();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        normalizeAerTier();
    }

    private void normalizeAerTier() {
        if (aerTier == null) {
            aerTier = 1;
        }
        if (!EsportsTierSupport.isValidAerTier(aerTier)) {
            throw new IllegalStateException("aerTier chi hop le 0, 1 hoac 2.");
        }
    }
}
