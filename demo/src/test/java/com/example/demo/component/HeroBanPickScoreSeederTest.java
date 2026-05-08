package com.example.demo.component;

import com.example.demo.entity.Hero;
import com.example.demo.repository.HeroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeroBanPickScoreSeederTest {

    @Mock
    private HeroRepository heroRepository;

    @Test
    @SuppressWarnings("unchecked")
    void seedsNullScoresWithoutOverwritingExistingValuesAndSupportsAliases() throws Exception {
        Hero hayate = hero(1L, "Hayate", "hayate", null);
        Hero zanis = hero(2L, "Zanis", "zanis", null);
        Hero superman = hero(3L, "Superman", "superman", null);
        Hero marja = hero(4L, "Marja", "marja", new BigDecimal("1.11"));

        when(heroRepository.findAllByOrderByNameAsc()).thenReturn(List.of(hayate, marja, superman, zanis));
        when(heroRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        HeroBanPickScoreSeeder seeder = new HeroBanPickScoreSeeder(heroRepository);
        seeder.run();

        assertThat(hayate.getBanPickScore()).isEqualByComparingTo("10");
        assertThat(zanis.getBanPickScore()).isEqualByComparingTo("6.77");
        assertThat(superman.getBanPickScore()).isEqualByComparingTo("0");
        assertThat(marja.getBanPickScore()).isEqualByComparingTo("1.11");

        ArgumentCaptor<List<Hero>> captor = ArgumentCaptor.forClass(List.class);
        verify(heroRepository).saveAll(captor.capture());
        assertThat(captor.getValue())
                .extracting(Hero::getName)
                .containsExactlyInAnyOrder("Hayate", "Zanis", "Superman");
    }

    private Hero hero(Long id, String name, String slug, BigDecimal score) {
        Hero hero = new Hero();
        hero.setId(id);
        hero.setName(name);
        hero.setSlug(slug);
        hero.setBanPickScore(score);
        return hero;
    }
}
