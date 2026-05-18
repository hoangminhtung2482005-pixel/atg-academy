package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ban_pick_rank_resets",
        uniqueConstraints = @UniqueConstraint(name = "uk_ban_pick_rank_resets_scheduled_date", columnNames = "scheduled_date")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BanPickRankResetLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "reset_type", nullable = false, length = 16)
    private BanPickSeasonResetType resetType;

    @Column(name = "scheduled_date", nullable = false, unique = true)
    private LocalDate scheduledDate;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @Column(name = "affected_players", nullable = false)
    private Integer affectedPlayers = 0;

    @Column(name = "base_rating", nullable = false)
    private Integer baseRating = BanPickSeasonResetType.BASE_RATING;

    @Column(name = "formula", nullable = false, length = 255)
    private String formula;

    @Column(name = "executed_by", length = 255)
    private String executedBy;

    @Column(columnDefinition = "TEXT")
    private String note;

    @PrePersist
    protected void onCreate() {
        if (executedAt == null) {
            executedAt = LocalDateTime.now();
        }
        if (affectedPlayers == null) {
            affectedPlayers = 0;
        }
        if (baseRating == null) {
            baseRating = BanPickSeasonResetType.BASE_RATING;
        }
    }
}
