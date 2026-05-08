package com.example.demo.controller;

import com.example.demo.dto.wiki.SpellDto;
import com.example.demo.service.SpellService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/spells")
public class SpellController {

    private final SpellService spellService;

    public SpellController(SpellService spellService) {
        this.spellService = spellService;
    }

    @GetMapping
    public ResponseEntity<List<SpellDto>> getAllSpells() {
        return ResponseEntity.ok(spellService.getAllSpells());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<SpellDto> getSpellBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(spellService.getSpellBySlug(slug));
    }
}
