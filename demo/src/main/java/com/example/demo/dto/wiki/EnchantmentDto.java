package com.example.demo.dto.wiki;

public record EnchantmentDto(
        String slug,
        String name,
        String branch,
        String branchName,
        Integer level,
        String iconUrl,
        String description,
        String branchIconUrl
) {
}
