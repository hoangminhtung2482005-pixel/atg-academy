package com.example.demo.dto.banpick;

import java.util.List;

public record BanPickProfileResponse(
        BanPickUserSummary user,
        PlayerStatsResponse stats,
        BanPickPlayerCardResponse playerCard,
        List<DraftHistoryResponse> history
) {
}
