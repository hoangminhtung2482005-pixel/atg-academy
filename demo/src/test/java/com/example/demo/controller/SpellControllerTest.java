package com.example.demo.controller;

import com.example.demo.dto.wiki.SpellDto;
import com.example.demo.service.SpellService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpellControllerTest {

    @Test
    void getAllSpellsDelegatesToService() {
        SpellService spellService = mock(SpellService.class);
        SpellController controller = new SpellController(spellService);
        List<SpellDto> spells = List.of(
                new SpellDto("boc-pha", "Bá»™c phÃ¡", "/images/spells/boc-pha.png", null)
        );

        when(spellService.getAllSpells()).thenReturn(spells);

        assertThat(controller.getAllSpells().getBody()).isEqualTo(spells);
        verify(spellService).getAllSpells();
    }

    @Test
    void getSpellBySlugDelegatesToService() {
        SpellService spellService = mock(SpellService.class);
        SpellController controller = new SpellController(spellService);
        SpellDto spell = new SpellDto("toc-bien", "Tá»‘c biáº¿n", "/images/spells/toc-bien.png", null);

        when(spellService.getSpellBySlug("toc-bien")).thenReturn(spell);

        assertThat(controller.getSpellBySlug("toc-bien").getBody()).isEqualTo(spell);
        verify(spellService).getSpellBySlug("toc-bien");
    }
}
