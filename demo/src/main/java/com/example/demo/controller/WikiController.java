package com.example.demo.controller;

import com.example.demo.dto.wiki.HeroDetailDto;
import com.example.demo.dto.wiki.HeroSummaryDto;
import com.example.demo.service.WikiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wiki")
public class WikiController {

    private final WikiService wikiService;

    public WikiController(WikiService wikiService) {
        this.wikiService = wikiService;
    }

    @GetMapping("/heroes")
    public ResponseEntity<List<HeroSummaryDto>> getAllHeroes() {
        return ResponseEntity.ok(wikiService.getAllHeroes());
    }

    @GetMapping("/heroes/{slug}")
    public ResponseEntity<HeroDetailDto> getHeroBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(wikiService.getHeroBySlug(slug));
    }
}
