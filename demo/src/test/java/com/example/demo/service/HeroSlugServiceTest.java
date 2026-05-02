package com.example.demo.service;

import com.example.demo.entity.Hero;
import com.example.demo.repository.HeroRepository;
import com.example.demo.util.SlugUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HeroSlugServiceTest {

    @Mock
    private HeroRepository heroRepository;

    private HeroSlugService service;

    @BeforeEach
    void setUp() {
        service = new HeroSlugService(heroRepository);
    }

    @Test
    void appendsNumericSuffixWhenBaseSlugExists() {
        Hero existing = new Hero();
        existing.setId(1L);

        when(heroRepository.findBySlug("florentino")).thenReturn(Optional.of(existing));
        when(heroRepository.findBySlug("florentino-2")).thenReturn(Optional.empty());

        assertThat(service.generateUniqueSlug("Florentino")).isEqualTo("florentino-2");
    }

    @Test
    void keepsSlugInsideDatabaseLimitWhenSuffixIsAdded() {
        String longName = "a".repeat(SlugUtils.MAX_SLUG_LENGTH + 10);
        String baseSlug = "a".repeat(SlugUtils.MAX_SLUG_LENGTH);
        Hero existing = new Hero();
        existing.setId(1L);

        when(heroRepository.findBySlug(baseSlug)).thenReturn(Optional.of(existing));
        when(heroRepository.findBySlug("a".repeat(SlugUtils.MAX_SLUG_LENGTH - 2) + "-2"))
                .thenReturn(Optional.empty());

        String slug = service.generateUniqueSlug(longName);

        assertThat(slug).hasSize(SlugUtils.MAX_SLUG_LENGTH);
        assertThat(slug).endsWith("-2");
    }
}
