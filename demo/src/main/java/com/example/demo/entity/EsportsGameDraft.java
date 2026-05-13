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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "esports_game_drafts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_esports_game_drafts_match_game",
                columnNames = {"match_id", "game_number"}
        ),
        indexes = {
                @Index(name = "idx_esports_game_drafts_match_id", columnList = "match_id"),
                @Index(name = "idx_esports_game_drafts_blue_team_id", columnList = "blue_team_id"),
                @Index(name = "idx_esports_game_drafts_red_team_id", columnList = "red_team_id"),
                @Index(name = "idx_esports_game_drafts_winner_team_id", columnList = "winner_team_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EsportsGameDraft {

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

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "draft_format_code", length = 50)
    private String draftFormatCode;

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "blue_ban_1_hero_id")
    private Long blueBan1HeroId;

    @Column(name = "blue_ban_2_hero_id")
    private Long blueBan2HeroId;

    @Column(name = "blue_ban_3_hero_id")
    private Long blueBan3HeroId;

    @Column(name = "blue_ban_4_hero_id")
    private Long blueBan4HeroId;

    @Column(name = "blue_ban_5_hero_id")
    private Long blueBan5HeroId;

    @Column(name = "red_ban_1_hero_id")
    private Long redBan1HeroId;

    @Column(name = "red_ban_2_hero_id")
    private Long redBan2HeroId;

    @Column(name = "red_ban_3_hero_id")
    private Long redBan3HeroId;

    @Column(name = "red_ban_4_hero_id")
    private Long redBan4HeroId;

    @Column(name = "red_ban_5_hero_id")
    private Long redBan5HeroId;

    @Column(name = "blue_dsl_hero_id")
    private Long blueDslHeroId;

    @Column(name = "blue_jgl_hero_id")
    private Long blueJglHeroId;

    @Column(name = "blue_mid_hero_id")
    private Long blueMidHeroId;

    @Column(name = "blue_adl_hero_id")
    private Long blueAdlHeroId;

    @Column(name = "blue_sup_hero_id")
    private Long blueSupHeroId;

    @Column(name = "red_dsl_hero_id")
    private Long redDslHeroId;

    @Column(name = "red_jgl_hero_id")
    private Long redJglHeroId;

    @Column(name = "red_mid_hero_id")
    private Long redMidHeroId;

    @Column(name = "red_adl_hero_id")
    private Long redAdlHeroId;

    @Column(name = "red_sup_hero_id")
    private Long redSupHeroId;

    @Column(name = "raw_draft_json", columnDefinition = "LONGTEXT")
    private String rawDraftJson;

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
