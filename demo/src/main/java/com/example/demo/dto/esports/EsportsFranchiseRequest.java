package com.example.demo.dto.esports;

public record EsportsFranchiseRequest(
        String code,
        String name,
        String tierLevel,
        String region,
        String description,
        String logoUrl,
        Integer displayOrder,
        Boolean active
) {
}
