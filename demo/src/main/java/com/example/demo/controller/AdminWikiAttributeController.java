package com.example.demo.controller;

import com.example.demo.dto.admin.AdminHeroAttributeResponse;
import com.example.demo.dto.admin.AdminHeroAttributeUpsertRequest;
import com.example.demo.service.AdminWikiHeroService;
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
@RequestMapping("/api/admin/wiki/attributes")
public class AdminWikiAttributeController {

    private final AdminWikiHeroService adminWikiHeroService;

    public AdminWikiAttributeController(AdminWikiHeroService adminWikiHeroService) {
        this.adminWikiHeroService = adminWikiHeroService;
    }

    @GetMapping
    public List<AdminHeroAttributeResponse> listAttributes() {
        return adminWikiHeroService.listAttributes();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminHeroAttributeResponse createAttribute(@RequestBody AdminHeroAttributeUpsertRequest request) {
        return adminWikiHeroService.createAttribute(request);
    }

    @PutMapping("/{id}")
    public AdminHeroAttributeResponse updateAttribute(@PathVariable Long id,
                                                      @RequestBody AdminHeroAttributeUpsertRequest request) {
        return adminWikiHeroService.updateAttribute(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttribute(@PathVariable Long id) {
        adminWikiHeroService.deleteAttribute(id);
    }
}
