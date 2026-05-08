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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "esports_match_game_lineups",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_esports_match_game_lineups_game_side_position",
                        columnNames = {"game_id", "team_side", "position_number"}
                ),
                @UniqueConstraint(
                        name = "uk_esports_match_game_lineups_game_side_lane",
                        columnNames = {"game_id", "team_side", "lane_role"}
                ),
                @UniqueConstraint(
                        name = "uk_esports_match_game_lineups_game_hero",
                        columnNames = {"game_id", "hero_id"}
                )
        },
        indexes = {
                @Index(name = "idx_esports_match_game_lineups_game_id", columnList = "game_id"),
                @Index(name = "idx_esports_match_game_lineups_team_id", columnList = "team_id"),
                @Index(name = "idx_esports_match_game_lineups_hero_id", columnList = "hero_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EsportsMatchGameLineup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private EsportsMatchGame game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private EsportsTeam team;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_side", nullable = false, length = 10)
    private BanPickTeamSide teamSide;

    @Column(name = "position_number", nullable = false)
    private Integer positionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "lane_role", nullable = false, length = 10)
    private EsportsLineupLaneRole laneRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hero_id", nullable = false)
    private Hero hero;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
