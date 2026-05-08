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
        name = "esports_match_draft_actions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_esports_match_draft_actions_game_step_number",
                        columnNames = {"game_id", "step_number"}
                ),
                @UniqueConstraint(
                        name = "uk_esports_match_draft_actions_game_hero_id",
                        columnNames = {"game_id", "hero_id"}
                )
        },
        indexes = {
                @Index(name = "idx_esports_match_draft_actions_game_step", columnList = "game_id, step_number"),
                @Index(name = "idx_esports_match_draft_actions_hero_id", columnList = "hero_id"),
                @Index(name = "idx_esports_match_draft_actions_team_id", columnList = "team_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EsportsMatchDraftAction {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hero_id", nullable = false)
    private Hero hero;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 10)
    private BanPickActionType actionType;

    @Column(name = "step_number", nullable = false)
    private Integer stepNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_side", nullable = false, length = 10)
    private BanPickTeamSide teamSide;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
