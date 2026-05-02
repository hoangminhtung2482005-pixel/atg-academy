package com.example.demo.dto.admin;

import java.util.List;

public record AdminHeroAttributesUpdateRequest(
        List<String> attributes
) {
}
