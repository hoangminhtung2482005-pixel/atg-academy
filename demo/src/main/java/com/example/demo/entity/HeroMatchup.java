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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(
        name = "hero_matchups",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_hero_matchups_pair_type",
                        columnNames = {"hero_id", "target_hero_id", "matchup_type"}
                )
        },
        indexes = {
                @Index(name = "idx_hero_matchups_hero_type", columnList = "hero_id, matchup_type"),
                @Index(name = "idx_hero_matchups_target_hero_id", columnList = "target_hero_id"),
                @Index(name = "idx_hero_matchups_type", columnList = "matchup_type")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HeroMatchup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hero_id", nullable = false)
    @ToString.Exclude
    private Hero hero;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_hero_id", nullable = false)
    @ToString.Exclude
    private Hero targetHero;

    @Enumerated(EnumType.STRING)
    @Column(name = "matchup_type", nullable = false, length = 30)
    private HeroMatchupType matchupType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private HeroMatchupDifficulty difficulty;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
