package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "draft_histories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DraftHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String roomCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BanPickMatchMode mode = BanPickMatchMode.SIMULATION;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blue_user_id", nullable = false)
    private User blueUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "red_user_id", nullable = false)
    private User redUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_user_id")
    private User winnerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dodged_user_id")
    private User dodgedUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_reason", nullable = false, length = 32)
    private DraftHistoryEndReason endReason = DraftHistoryEndReason.NORMAL;

    @Column(columnDefinition = "TEXT")
    private String bluePicks;

    @Column(columnDefinition = "TEXT")
    private String redPicks;

    @Column(columnDefinition = "TEXT")
    private String blueBans;

    @Column(columnDefinition = "TEXT")
    private String redBans;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime resultRecordedAt;

    @Column(name = "win_rating_delta", nullable = false)
    private Integer winRatingDelta = 30;

    @Column(name = "loss_rating_delta", nullable = false)
    private Integer lossRatingDelta = -20;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (mode == null) {
            mode = BanPickMatchMode.SIMULATION;
        }
        if (winRatingDelta == null) {
            winRatingDelta = 30;
        }
        if (lossRatingDelta == null) {
            lossRatingDelta = -20;
        }
        if (endReason == null) {
            endReason = DraftHistoryEndReason.NORMAL;
        }
    }
}
