package com.example.demo.entity;

public enum BanPickMatchMode {
    SIMULATION,
    RANKED;

    public static BanPickMatchMode defaultIfNull(BanPickMatchMode mode) {
        return mode != null ? mode : SIMULATION;
    }
}
