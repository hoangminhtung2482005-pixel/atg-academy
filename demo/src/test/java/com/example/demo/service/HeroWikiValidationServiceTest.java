package com.example.demo.service;

import com.example.demo.entity.Hero;
import com.example.demo.entity.HeroMatchup;
import com.example.demo.entity.HeroMatchupDifficulty;
import com.example.demo.entity.HeroMatchupType;
import com.example.demo.entity.HeroSkill;
import com.example.demo.entity.HeroSkillType;
import com.example.demo.repository.HeroSkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeroWikiValidationServiceTest {

    @Mock
    private HeroSkillRepository heroSkillRepository;

    private HeroWikiValidationService service;

    @BeforeEach
    void setUp() {
        service = new HeroWikiValidationService(heroSkillRepository);
    }

    @Test
    void rejectsDuplicateHeroSkillType() {
        Hero hero = hero(10L);
        HeroSkill skill = new HeroSkill();
        skill.setHero(hero);
        skill.setSkillType(HeroSkillType.SKILL_1);

        when(heroSkillRepository.existsByHeroIdAndSkillType(10L, HeroSkillType.SKILL_1))
                .thenReturn(true);

        assertThatThrownBy(() -> service.validateSkillForSave(skill))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void defaultsHeroSkillSortOrderFromSkillType() {
        HeroSkill skill = new HeroSkill();
        skill.setHero(hero(10L));
        skill.setSkillType(HeroSkillType.ULTIMATE);
        skill.setSortOrder(null);

        service.validateSkillForSave(skill);

        assertThat(skill.getSortOrder()).isEqualTo(3);
    }

    @Test
    void rejectsSelfMatchup() {
        Hero hero = hero(10L);
        HeroMatchup matchup = new HeroMatchup();
        matchup.setHero(hero);
        matchup.setTargetHero(hero);
        matchup.setMatchupType(HeroMatchupType.COUNTER);
        matchup.setDifficulty(HeroMatchupDifficulty.HARD);

        assertThatThrownBy(() -> service.validateMatchupForSave(matchup))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private Hero hero(Long id) {
        Hero hero = new Hero();
        hero.setId(id);
        hero.setName("Hero " + id);
        return hero;
    }
}
