package com.example.demo.controller;

import com.example.demo.dto.wiki.EnchantmentDto;
import com.example.demo.service.EnchantmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/enchantments")
public class EnchantmentController {

    private final EnchantmentService enchantmentService;

    public EnchantmentController(EnchantmentService enchantmentService) {
        this.enchantmentService = enchantmentService;
    }

    @GetMapping
    public ResponseEntity<List<EnchantmentDto>> getAllEnchantments() {
        return ResponseEntity.ok(enchantmentService.getAllEnchantments());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<EnchantmentDto> getEnchantmentBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(enchantmentService.getEnchantmentBySlug(slug));
    }
}
