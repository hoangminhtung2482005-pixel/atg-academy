package com.example.demo.service;

import com.example.demo.dto.wiki.HeroDetailDto;
import com.example.demo.dto.wiki.HeroSummaryDto;
import com.example.demo.entity.Guide;
import com.example.demo.entity.Hero;
import com.example.demo.entity.HeroMatchup;
import com.example.demo.entity.HeroSkill;
import com.example.demo.repository.GuideRepository;
import com.example.demo.repository.HeroMatchupRepository;
import com.example.demo.repository.HeroRepository;
import com.example.demo.repository.HeroSkillRepository;
import com.example.demo.util.SlugUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class WikiService {

    private static final int RELATED_GUIDE_LIMIT = 6;

    private final HeroRepository heroRepository;
    private final HeroSkillRepository heroSkillRepository;
    private final HeroMatchupRepository heroMatchupRepository;
    private final GuideRepository guideRepository;

    public WikiService(HeroRepository heroRepository,
                       HeroSkillRepository heroSkillRepository,
                       HeroMatchupRepository heroMatchupRepository,
                       GuideRepository guideRepository) {
        this.heroRepository = heroRepository;
        this.heroSkillRepository = heroSkillRepository;
        this.heroMatchupRepository = heroMatchupRepository;
        this.guideRepository = guideRepository;
    }

    @Transactional(readOnly = true)
    public List<HeroSummaryDto> getAllHeroes() {
        return heroRepository.findAllWithRolesAndAttributes()
                .stream()
                .map(HeroSummaryDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public HeroDetailDto getHeroBySlug(String rawSlug) {
        String slug = SlugUtils.toSlug(rawSlug);
        Hero hero = heroRepository.findWithRolesAndAttributesBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Hero not found"));

        List<HeroSkill> skills = heroSkillRepository.findByHeroIdOrderBySortOrderAscIdAsc(hero.getId());
        List<HeroMatchup> matchups = heroMatchupRepository.findByHeroIdWithTargetHeroCatalog(hero.getId());
        List<Guide> relatedGuides = guideRepository.findPublishedByHeroId(
                hero.getId(),
                PageRequest.of(0, RELATED_GUIDE_LIMIT)
        );

        return HeroDetailDto.from(hero, skills, matchups, relatedGuides);
    }
}
