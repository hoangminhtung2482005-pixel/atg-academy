package com.example.demo.dto.banpick;

public record BanPickSeasonResetExecuteRequest(
        String type,
        String confirmationText,
        String note
) {
}
