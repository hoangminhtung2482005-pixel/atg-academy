package com.example.demo.dto.banpick;

import com.example.demo.entity.BanPickSeasonResetType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BanPickSeasonResetExecuteResponse(
        boolean executed,
        Long resetLogId,
        BanPickSeasonResetType resetType,
        LocalDate scheduledDate,
        LocalDateTime executedAt,
        String executedBy,
        String note,
        BanPickSeasonResetPreviewResponse preview
) {
}
