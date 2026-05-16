package com.example.demo.entity;

import java.util.Arrays;

public enum UserStatus {
    ACTIVE,
    LOCKED;

    public static UserStatus from(String value) {
        String normalized = value == null ? "" : value.trim();
        return Arrays.stream(values())
                .filter(status -> status.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Trạng thái không hợp lệ"));
    }
}
