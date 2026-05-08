package com.example.demo.service;

import com.example.demo.dto.wiki.SpellDto;
import com.example.demo.entity.Spell;
import com.example.demo.repository.SpellRepository;
import com.example.demo.util.SlugUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SpellService {

    private final SpellRepository spellRepository;

    public SpellService(SpellRepository spellRepository) {
        this.spellRepository = spellRepository;
    }

    @Transactional(readOnly = true)
    public List<SpellDto> getAllSpells() {
        return spellRepository.findAllByOrderByIdAsc()
                .stream()
                .map(SpellDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SpellDto getSpellBySlug(String rawSlug) {
        String slug = SlugUtils.toSlug(rawSlug);
        Spell spell = spellRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Spell not found"));
        return SpellDto.from(spell);
    }
}
