package com.example.demo.dto.esports;

public record EsportsResetDataRequest(
        String confirmationText,
        boolean backupBeforeReset
) {
}
