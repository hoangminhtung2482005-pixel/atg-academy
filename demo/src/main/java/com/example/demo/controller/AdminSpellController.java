package com.example.demo.controller;

import com.example.demo.dto.wiki.SpellDto;
import com.example.demo.service.SpellService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/spells")
public class AdminSpellController {

    private final SpellService spellService;

    public AdminSpellController(SpellService spellService) {
        this.spellService = spellService;
    }

    @GetMapping
    public List<SpellDto> listSpells() {
        return spellService.getAllSpells();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpellDto createSpell(@RequestBody SpellDto request) {
        return spellService.createSpell(request);
    }

    @PutMapping("/{slug}")
    public SpellDto updateSpell(@PathVariable String slug, @RequestBody SpellDto request) {
        return spellService.updateSpell(slug, request);
    }

    @DeleteMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSpell(@PathVariable String slug) {
        spellService.deleteSpell(slug);
    }
}
