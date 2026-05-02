package com.example.demo.repository;

import com.example.demo.entity.HeroSkill;
import com.example.demo.entity.HeroSkillType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HeroSkillRepository extends JpaRepository<HeroSkill, Long> {

    List<HeroSkill> findByHeroIdOrderBySortOrderAscIdAsc(Long heroId);

    boolean existsByHeroIdAndSkillType(Long heroId, HeroSkillType skillType);

    boolean existsByHeroIdAndSkillTypeAndIdNot(Long heroId, HeroSkillType skillType, Long id);

    boolean existsByHeroIdAndSortOrder(Long heroId, Integer sortOrder);

    boolean existsByHeroIdAndSortOrderAndIdNot(Long heroId, Integer sortOrder, Long id);
}
