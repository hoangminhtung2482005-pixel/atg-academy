package com.example.demo.dto.admin;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminUserPageResponse(
        List<AdminUserResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static AdminUserPageResponse from(Page<AdminUserResponse> page) {
        return new AdminUserPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
