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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(
        name = "hero_skills",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_hero_skills_hero_skill_type",
                        columnNames = {"hero_id", "skill_type"}
                ),
                @UniqueConstraint(
                        name = "uk_hero_skills_hero_sort_order",
                        columnNames = {"hero_id", "sort_order"}
                )
        },
        indexes = {
                @Index(name = "idx_hero_skills_skill_type", columnList = "skill_type")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HeroSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hero_id", nullable = false)
    @ToString.Exclude
    private Hero hero;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_type", nullable = false, length = 30)
    private HeroSkillType skillType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 80)
    private String cooldown;

    @Column(length = 80)
    private String manaCost;

    private String iconUrl;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @PrePersist
    @PreUpdate
    private void defaultSortOrder() {
        if (sortOrder == null) {
            if (skillType == null) {
                sortOrder = 0;
                return;
            }
            sortOrder = switch (skillType) {
                case PASSIVE -> 0;
                case SKILL_1 -> 1;
                case SKILL_2 -> 2;
                case ULTIMATE -> 3;
            };
        }
    }
}
