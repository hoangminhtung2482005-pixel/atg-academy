package com.example.demo.security;

public record GoogleUserPrincipal(
        String email,
        String name,
        String picture,
        String role
) {

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isStaff() {
        return "STAFF".equalsIgnoreCase(role);
    }
}
