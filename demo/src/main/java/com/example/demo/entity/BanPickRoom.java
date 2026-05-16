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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ban_pick_rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BanPickRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String roomCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BanPickRoomStatus status = BanPickRoomStatus.WAITING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BanPickPhaseType phaseType = BanPickPhaseType.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private BanPickSeriesType seriesType = BanPickSeriesType.BO1;

    @Column(nullable = false)
    private Integer currentGameNumber = 1;

    @Column(columnDefinition = "TEXT")
    private String blueUsedPicksByGame;

    @Column(columnDefinition = "TEXT")
    private String redUsedPicksByGame;

    @Column(columnDefinition = "TEXT")
    private String blueSeriesUsedHeroIds;

    @Column(columnDefinition = "TEXT")
    private String redSeriesUsedHeroIds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_user_id", nullable = false)
    private User hostUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_user_id")
    private User guestUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blue_user_id")
    private User blueUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "red_user_id")
    private User redUser;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean hostReady = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean guestReady = false;

    @Column(nullable = false)
    private Integer currentPhaseIndex = 0;

    @Column(nullable = false)
    private Integer currentPhaseSelectedCount = 0;

    @Column(nullable = false)
    private Integer phaseDurationSeconds = 60;

    private LocalDateTime timerStartedAt;

    private LocalDateTime phaseDeadlineAt;

    private LocalDateTime lineupDeadlineAt;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean blueLineupConfirmed = false;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean redLineupConfirmed = false;

    @Column(columnDefinition = "TEXT")
    private String bluePickOrder;

    @Column(columnDefinition = "TEXT")
    private String redPickOrder;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Transient
    private DraftHistoryEndReason finishedEndReason;

    @Transient
    private String dodgedUserEmail;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = BanPickRoomStatus.WAITING;
        if (this.phaseType == null) this.phaseType = BanPickPhaseType.DRAFT;
        if (this.seriesType == null) this.seriesType = BanPickSeriesType.BO1;
        if (this.currentGameNumber == null) this.currentGameNumber = 1;
        if (this.hostReady == null) this.hostReady = false;
        if (this.guestReady == null) this.guestReady = false;
        if (this.blueLineupConfirmed == null) this.blueLineupConfirmed = false;
        if (this.redLineupConfirmed == null) this.redLineupConfirmed = false;
        if (this.currentPhaseIndex == null) this.currentPhaseIndex = 0;
        if (this.currentPhaseSelectedCount == null) this.currentPhaseSelectedCount = 0;
        if (this.phaseDurationSeconds == null) this.phaseDurationSeconds = 60;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
