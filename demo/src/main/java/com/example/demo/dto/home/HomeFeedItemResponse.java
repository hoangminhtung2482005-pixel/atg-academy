package com.example.demo.dto.home;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record HomeFeedItemResponse(
        String type,
        Long id,
        String title,
        String author,
        LocalDateTime createdAt,
        Double rating,
        Long ratingCount,
        List<Map<String, Object>> preview,
        String description,
        String image,
        Integer readTime,
        String category,
        String badgeLabel
) {

    public static HomeFeedItemResponse tier(Long id,
                                            String title,
                                            String author,
                                            LocalDateTime createdAt,
                                            Double rating,
                                            Long ratingCount,
                                            List<Map<String, Object>> preview) {
        return tier(id, title, author, createdAt, rating, ratingCount, preview, null);
    }

    public static HomeFeedItemResponse tier(Long id,
                                            String title,
                                            String author,
                                            LocalDateTime createdAt,
                                            Double rating,
                                            Long ratingCount,
                                            List<Map<String, Object>> preview,
                                            String badgeLabel) {
        return new HomeFeedItemResponse(
                "tier",
                id,
                title,
                author,
                createdAt,
                rating,
                ratingCount,
                preview,
                null,
                null,
                null,
                null,
                badgeLabel
        );
    }

    public static HomeFeedItemResponse guide(Long id,
                                             String title,
                                             String description,
                                             String image,
                                             LocalDateTime createdAt,
                                             Integer readTime,
                                             String category) {
        return new HomeFeedItemResponse(
                "guide",
                id,
                title,
                null,
                createdAt,
                null,
                null,
                null,
                description,
                image,
                readTime,
                category,
                null
        );
    }
}
