package com.example.demo.entity;

import java.util.Arrays;

public enum UserRole {
    ADMIN("Admin"),
    STAFF("Staff"),
    USER("User"),
    CUSTOM("Custom");

    private final String storageValue;

    UserRole(String storageValue) {
        this.storageValue = storageValue;
    }

    public String getStorageValue() {
        return storageValue;
    }

    public static UserRole from(String value) {
        String normalized = value == null ? "" : value.trim();
        return Arrays.stream(values())
                .filter(role -> role.name().equalsIgnoreCase(normalized)
                        || role.storageValue.equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Vai tro khong hop le"));
    }
}
