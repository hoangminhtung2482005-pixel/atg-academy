package com.example.demo.controller;

import com.example.demo.dto.admin.AdminHeroAttributeResponse;
import com.example.demo.dto.admin.AdminHeroAttributeUpsertRequest;
import com.example.demo.service.HeroAttributeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/attributes")
public class HeroAttributeController {

    private final HeroAttributeService heroAttributeService;

    public HeroAttributeController(HeroAttributeService heroAttributeService) {
        this.heroAttributeService = heroAttributeService;
    }

    @GetMapping
    public List<AdminHeroAttributeResponse> findAll() {
        return heroAttributeService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminHeroAttributeResponse create(@RequestBody AdminHeroAttributeUpsertRequest request) {
        return heroAttributeService.create(request);
    }

    @PatchMapping("/{id}")
    public AdminHeroAttributeResponse update(@PathVariable Long id,
                                             @RequestBody AdminHeroAttributeUpsertRequest request) {
        return heroAttributeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        heroAttributeService.delete(id);
    }
}
