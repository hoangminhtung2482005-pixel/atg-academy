package com.example.demo.service;

import com.example.demo.dto.wiki.SpellDto;
import com.example.demo.repository.GuideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpellServiceTest {

    @Mock
    private WikiJsonStorageService wikiJsonStorageService;

    @Mock
    private GuideRepository guideRepository;

    private SpellService spellService;

    @BeforeEach
    void setUp() {
        spellService = new SpellService(wikiJsonStorageService, guideRepository, new ObjectMapper());
    }

    @Test
    void getAllSpellsReturnsStoredDtos() {
        when(wikiJsonStorageService.readSpells()).thenReturn(List.of(
                new SpellDto("boc-pha", "Bá»™c phÃ¡", "/images/spells/boc-pha.png", null),
                new SpellDto("toc-bien", "Tá»‘c biáº¿n", "/images/spells/toc-bien.png", null)
        ));

        List<SpellDto> result = spellService.getAllSpells();

        assertThat(result)
                .extracting(SpellDto::slug, SpellDto::name, SpellDto::iconUrl)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("boc-pha", "Bá»™c phÃ¡", "/images/spells/boc-pha.png"),
                        org.assertj.core.groups.Tuple.tuple("toc-bien", "Tá»‘c biáº¿n", "/images/spells/toc-bien.png")
                );
    }

    @Test
    void getSpellBySlugNormalizesSlugBeforeLookup() {
        when(wikiJsonStorageService.readSpells()).thenReturn(List.of(
                new SpellDto("toc-bien", "Tá»‘c biáº¿n", "/images/spells/toc-bien.png", null)
        ));

        SpellDto result = spellService.getSpellBySlug("Toc Bien");

        assertThat(result.slug()).isEqualTo("toc-bien");
        assertThat(result.name()).isEqualTo("Tá»‘c biáº¿n");
        verify(wikiJsonStorageService).readSpells();
    }

    @Test
    void getSpellBySlugThrowsNotFoundWhenMissing() {
        when(wikiJsonStorageService.readSpells()).thenReturn(List.of());

        assertThatThrownBy(() -> spellService.getSpellBySlug("missing-spell"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }
}
