package com.example.demo.dto.banpick;

public record BanPickPlayerCardResponse(
        String avatarUrl,
        String displayName,
        Integer elo,
        String rankCode,
        String rankLabel,
        String badgeCode,
        String badgeName,
        String badgeIconUrl,
        String title
) {
}
