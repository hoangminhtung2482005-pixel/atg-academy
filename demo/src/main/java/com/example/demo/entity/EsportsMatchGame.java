package com.example.demo.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "esports_match_games",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_esports_match_games_match_game_number",
                columnNames = {"match_id", "game_number"}
        ),
        indexes = {
                @Index(name = "idx_esports_match_games_match_id", columnList = "match_id"),
                @Index(name = "idx_esports_match_games_blue_team_id", columnList = "blue_team_id"),
                @Index(name = "idx_esports_match_games_red_team_id", columnList = "red_team_id"),
                @Index(name = "idx_esports_match_games_draft_format_id", columnList = "draft_format_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EsportsMatchGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private EsportsMatch match;

    @Column(name = "game_number", nullable = false)
    private Integer gameNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blue_team_id", nullable = false)
    private EsportsTeam blueTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "red_team_id", nullable = false)
    private EsportsTeam redTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_team_id")
    private EsportsTeam winnerTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draft_format_id")
    private EsportsDraftFormat draftFormat;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EsportsMatchDraftAction> draftActions = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EsportsMatchGameLineup> lineups = new ArrayList<>();

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
