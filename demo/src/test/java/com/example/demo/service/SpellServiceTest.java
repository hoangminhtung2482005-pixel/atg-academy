package com.example.demo.service;

import com.example.demo.dto.wiki.SpellDto;
import com.example.demo.entity.Spell;
import com.example.demo.repository.SpellRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpellServiceTest {

    @Mock
    private SpellRepository spellRepository;

    private SpellService spellService;

    @BeforeEach
    void setUp() {
        spellService = new SpellService(spellRepository);
    }

    @Test
    void getAllSpellsReturnsOrderedDtos() {
        Spell bocPha = spell(1L, "Bộc phá", "boc-pha", "/images/spells/boc-pha.png");
        Spell tocBien = spell(2L, "Tốc biến", "toc-bien", "/images/spells/toc-bien.png");

        when(spellRepository.findAllByOrderByIdAsc()).thenReturn(List.of(bocPha, tocBien));

        List<SpellDto> result = spellService.getAllSpells();

        assertThat(result)
                .extracting(SpellDto::id, SpellDto::name, SpellDto::slug, SpellDto::iconUrl)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, "Bộc phá", "boc-pha", "/images/spells/boc-pha.png"),
                        org.assertj.core.groups.Tuple.tuple(2L, "Tốc biến", "toc-bien", "/images/spells/toc-bien.png")
                );
    }

    @Test
    void getSpellBySlugNormalizesSlugBeforeLookup() {
        Spell spell = spell(8L, "Tốc biến", "toc-bien", "/images/spells/toc-bien.png");

        when(spellRepository.findBySlug("toc-bien")).thenReturn(Optional.of(spell));

        SpellDto result = spellService.getSpellBySlug("Toc Bien");

        assertThat(result.slug()).isEqualTo("toc-bien");
        assertThat(result.name()).isEqualTo("Tốc biến");
        verify(spellRepository).findBySlug("toc-bien");
    }

    @Test
    void getSpellBySlugThrowsNotFoundWhenMissing() {
        when(spellRepository.findBySlug("missing-spell")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> spellService.getSpellBySlug("missing-spell"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    private Spell spell(Long id, String name, String slug, String iconUrl) {
        Spell spell = new Spell();
        spell.setId(id);
        spell.setName(name);
        spell.setSlug(slug);
        spell.setIconUrl(iconUrl);
        spell.setCreatedAt(LocalDateTime.of(2026, 5, 8, 10, 0));
        spell.setUpdatedAt(LocalDateTime.of(2026, 5, 8, 10, 0));
        return spell;
    }
}
