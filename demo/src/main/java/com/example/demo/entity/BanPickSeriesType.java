package com.example.demo.entity;

public enum BanPickSeriesType {
    BO1(1),
    BO3(3),
    BO5(5),
    BO7(7);

    private final int maxGames;

    BanPickSeriesType(int maxGames) {
        this.maxGames = maxGames;
    }

    public int getMaxGames() {
        return maxGames;
    }

    public static BanPickSeriesType defaultIfNull(BanPickSeriesType seriesType) {
        return seriesType != null ? seriesType : BO1;
    }
}
