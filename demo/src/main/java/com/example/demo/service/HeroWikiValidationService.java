package com.example.demo.service;

import com.example.demo.entity.Hero;
import com.example.demo.entity.HeroMatchup;
import com.example.demo.entity.HeroSkill;
import com.example.demo.entity.HeroSkillType;
import com.example.demo.repository.HeroSkillRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Service
public class HeroWikiValidationService {

    private final HeroSkillRepository heroSkillRepository;

    public HeroWikiValidationService(HeroSkillRepository heroSkillRepository) {
        this.heroSkillRepository = heroSkillRepository;
    }

    public void validateSkillForSave(HeroSkill skill) {
        if (skill == null) {
            throw badRequest("Hero skill is required");
        }
        Long heroId = heroId(skill.getHero());
        if (heroId == null) {
            throw badRequest("Hero skill must reference a saved hero");
        }
        if (skill.getSkillType() == null) {
            throw badRequest("Hero skill type is required");
        }

        if (skill.getSortOrder() == null) {
            skill.setSortOrder(defaultSortOrder(skill.getSkillType()));
        }

        boolean duplicateSkillType = skill.getId() == null
                ? heroSkillRepository.existsByHeroIdAndSkillType(heroId, skill.getSkillType())
                : heroSkillRepository.existsByHeroIdAndSkillTypeAndIdNot(heroId, skill.getSkillType(), skill.getId());
        if (duplicateSkillType) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hero already has this skill type");
        }

        boolean duplicateSortOrder = skill.getId() == null
                ? heroSkillRepository.existsByHeroIdAndSortOrder(heroId, skill.getSortOrder())
                : heroSkillRepository.existsByHeroIdAndSortOrderAndIdNot(heroId, skill.getSortOrder(), skill.getId());
        if (duplicateSortOrder) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hero already has a skill with this sort order");
        }
    }

    public void validateMatchupForSave(HeroMatchup matchup) {
        if (matchup == null) {
            throw badRequest("Hero matchup is required");
        }
        Long heroId = heroId(matchup.getHero());
        Long targetHeroId = heroId(matchup.getTargetHero());
        if (heroId == null || targetHeroId == null) {
            throw badRequest("Hero matchup must reference saved heroes");
        }
        if (Objects.equals(heroId, targetHeroId)) {
            throw badRequest("Hero matchup cannot target the same hero");
        }
        if (matchup.getMatchupType() == null) {
            throw badRequest("Hero matchup type is required");
        }
        if (matchup.getDifficulty() == null) {
            throw badRequest("Hero matchup difficulty is required");
        }
    }

    private Long heroId(Hero hero) {
        return hero != null ? hero.getId() : null;
    }

    private int defaultSortOrder(HeroSkillType skillType) {
        return switch (skillType) {
            case PASSIVE -> 0;
            case SKILL_1 -> 1;
            case SKILL_2 -> 2;
            case ULTIMATE -> 3;
        };
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
