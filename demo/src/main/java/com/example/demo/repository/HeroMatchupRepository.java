package com.example.demo.repository;

import com.example.demo.entity.HeroMatchup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HeroMatchupRepository extends JpaRepository<HeroMatchup, Long> {

    @Query("""
            SELECT DISTINCT matchup FROM HeroMatchup matchup
            JOIN FETCH matchup.targetHero targetHero
            LEFT JOIN FETCH targetHero.primaryRole
            LEFT JOIN FETCH targetHero.classes
            LEFT JOIN FETCH targetHero.roles
            LEFT JOIN FETCH targetHero.attributes
            WHERE matchup.hero.id = :heroId
            ORDER BY matchup.matchupType ASC, targetHero.name ASC
            """)
    List<HeroMatchup> findByHeroIdWithTargetHeroCatalog(@Param("heroId") Long heroId);
}
