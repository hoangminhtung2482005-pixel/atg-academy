package com.example.demo.dto.user;

import java.util.List;

public record UserContentSummaryResponse(
        long guideCount,
        long tierListCount,
        List<UserContentItemResponse> guides,
        List<UserContentItemResponse> tierLists
) {
}
