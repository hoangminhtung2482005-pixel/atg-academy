package com.example.demo.entity;

import com.example.demo.util.SlugUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(
        name = "heroes",
        indexes = {
                @Index(name = "idx_heroes_name", columnList = "name")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Hero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = SlugUtils.MAX_SLUG_LENGTH)
    private String slug;

    @Column(length = 120)
    private String title;

    private String avatarUrl;

    private String portraitUrl;

    private String bannerUrl;

    /**
     * Legacy single-value class field kept temporarily for backward compatibility.
     * Prefer {@link #classes} for all new reads and writes.
     */
    @Column(name = "hero_class", length = 30)
    private String heroClass;

    @Column(length = 30)
    private String difficulty;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String lore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_role_id")
    @ToString.Exclude
    private HeroRole primaryRole;

    /**
     * Sub roles. Primary lane role is stored in {@code heroes.primary_role_id}.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "hero_role_mapping",
            joinColumns = @JoinColumn(name = "hero_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @ToString.Exclude
    private Set<HeroRole> roles = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "hero_class_mapping",
            joinColumns = @JoinColumn(name = "hero_id"),
            inverseJoinColumns = @JoinColumn(name = "class_id")
    )
    @ToString.Exclude
    private Set<HeroClass> classes = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "hero_attribute_mapping",
            joinColumns = @JoinColumn(name = "hero_id"),
            inverseJoinColumns = @JoinColumn(name = "attribute_id")
    )
    @ToString.Exclude
    private Set<HeroAttribute> attributes = new HashSet<>();

    @OneToMany(mappedBy = "hero", fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @ToString.Exclude
    private List<HeroSkill> skills = new ArrayList<>();

    @OneToMany(mappedBy = "hero", fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<HeroMatchup> matchups = new HashSet<>();

    @PrePersist
    @PreUpdate
    private void normalizeSlug() {
        if (slug == null || slug.isBlank()) {
            slug = SlugUtils.toSlug(name);
        } else {
            slug = SlugUtils.toSlug(slug);
        }
    }

    @Transient
    public String getRolesAsString() {
        List<HeroRole> laneRoles = new ArrayList<>();
        if (primaryRole != null) {
            laneRoles.add(primaryRole);
        }
        if (roles != null) {
            roles.stream()
                    .filter(Objects::nonNull)
                    .filter(role -> primaryRole == null || !Objects.equals(primaryRole.getId(), role.getId()))
                    .sorted((left, right) -> left.getCode().compareToIgnoreCase(right.getCode()))
                    .forEach(laneRoles::add);
        }

        if (laneRoles.isEmpty()) {
            if (classes != null && !classes.isEmpty()) {
                return classes.stream()
                        .map(HeroClass::resolveDisplayName)
                        .sorted()
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
            }
            return heroClass != null ? heroClass : "";
        }
        return laneRoles.stream()
                .map(HeroRole::getName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    @Transient
    public Set<HeroRole> getSubRoles() {
        return roles;
    }

    public void setSubRoles(Set<HeroRole> subRoles) {
        this.roles = subRoles != null ? subRoles : new HashSet<>();
    }
}
