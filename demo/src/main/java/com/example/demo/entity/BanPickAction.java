package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ban_pick_actions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ban_pick_actions_room_hero",
                columnNames = {"room_id", "hero_id"}
        ),
        indexes = {
                @Index(name = "idx_ban_pick_actions_room_phase", columnList = "room_id, phase_index"),
                @Index(name = "idx_ban_pick_actions_room_confirmed", columnList = "room_id, confirmed_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BanPickAction {

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
    @Column(nullable = false, length = 8)
    private BanPickTeamSide teamSide;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private BanPickActionType actionType;

    @Column(name = "hero_id", nullable = false)
    private Long heroId;

    @Column(name = "phase_index", nullable = false)
    private Integer phaseIndex;

    @Column(name = "confirmed_at", nullable = false)
    private LocalDateTime confirmedAt;
}
