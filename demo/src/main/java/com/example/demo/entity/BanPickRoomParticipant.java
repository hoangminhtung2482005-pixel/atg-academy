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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ban_pick_room_participants",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ban_pick_room_participants_room_user",
                columnNames = {"room_id", "user_id"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BanPickRoomParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private BanPickRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BanPickParticipantRole role;

    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    private BanPickTeamSide teamSide;

    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    /**
     * Strategy pool: comma-separated hero IDs that this participant wants to prioritize.
     * Scoped to the current room only. Not a pick/ban lock.
     */
    @Column(name = "strategy_pool", columnDefinition = "TEXT")
    private String strategyPool;

    @PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
    }
}
