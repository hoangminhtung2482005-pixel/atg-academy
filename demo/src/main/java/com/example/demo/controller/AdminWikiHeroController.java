package com.example.demo.controller;

import com.example.demo.dto.admin.AdminHeroAttributesUpdateRequest;
import com.example.demo.dto.admin.AdminHeroBasicUpdateRequest;
import com.example.demo.dto.admin.AdminHeroDetailResponse;
import com.example.demo.dto.admin.AdminHeroResponse;
import com.example.demo.dto.admin.AdminHeroRolesUpdateRequest;
import com.example.demo.service.AdminWikiHeroService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/wiki/heroes")
public class AdminWikiHeroController {

    private final AdminWikiHeroService adminWikiHeroService;

    public AdminWikiHeroController(AdminWikiHeroService adminWikiHeroService) {
        this.adminWikiHeroService = adminWikiHeroService;
    }

    @GetMapping
    public List<AdminHeroResponse> listHeroes() {
        return adminWikiHeroService.listHeroes();
    }

    @GetMapping("/{id}")
    public AdminHeroDetailResponse getHero(@PathVariable Long id) {
        return adminWikiHeroService.getHero(id);
    }

    @PutMapping("/{id}")
    public AdminHeroDetailResponse updateHeroBasicInfo(@PathVariable Long id,
                                                       @RequestBody AdminHeroBasicUpdateRequest request) {
        return adminWikiHeroService.updateHeroBasicInfo(id, request);
    }

    @PutMapping("/{id}/roles")
    public AdminHeroDetailResponse updateHeroRoles(@PathVariable Long id,
                                                  @RequestBody AdminHeroRolesUpdateRequest request) {
        return adminWikiHeroService.updateHeroRoles(id, request);
    }

    @PutMapping("/{id}/attributes")
    public AdminHeroDetailResponse updateHeroAttributes(@PathVariable Long id,
                                                       @RequestBody AdminHeroAttributesUpdateRequest request) {
        return adminWikiHeroService.updateHeroAttributes(id, request);
    }
}
