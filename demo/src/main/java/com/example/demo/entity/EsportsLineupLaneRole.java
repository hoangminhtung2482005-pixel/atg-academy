package com.example.demo.entity;

import java.util.Arrays;
import java.util.Optional;

public enum EsportsLineupLaneRole {
    DSL(1),
    JGL(2),
    MID(3),
    ADL(4),
    SUP(5);

    private final int positionNumber;

    EsportsLineupLaneRole(int positionNumber) {
        this.positionNumber = positionNumber;
    }

    public int getPositionNumber() {
        return positionNumber;
    }

    public boolean matchesPosition(int position) {
        return positionNumber == position;
    }

    public static Optional<EsportsLineupLaneRole> fromPosition(int position) {
        return Arrays.stream(values())
                .filter(role -> role.positionNumber == position)
                .findFirst();
    }
}
