package com.example.demo.controller;

import com.example.demo.dto.wiki.EnchantmentDto;
import com.example.demo.service.EnchantmentService;
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
@RequestMapping("/api/admin/enchantments")
public class AdminEnchantmentController {

    private final EnchantmentService enchantmentService;

    public AdminEnchantmentController(EnchantmentService enchantmentService) {
        this.enchantmentService = enchantmentService;
    }

    @GetMapping
    public List<EnchantmentDto> listEnchantments() {
        return enchantmentService.getAllEnchantments();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnchantmentDto createEnchantment(@RequestBody EnchantmentDto request) {
        return enchantmentService.createEnchantment(request);
    }

    @PutMapping("/{slug}")
    public EnchantmentDto updateEnchantment(@PathVariable String slug, @RequestBody EnchantmentDto request) {
        return enchantmentService.updateEnchantment(slug, request);
    }

    @DeleteMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEnchantment(@PathVariable String slug) {
        enchantmentService.deleteEnchantment(slug);
    }
}
