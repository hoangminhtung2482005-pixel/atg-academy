package com.example.demo.dto.banpick;

import com.example.demo.entity.BanPickMatchMode;
import com.example.demo.entity.BanPickSeriesType;

public record BanPickCreateRoomRequest(
        BanPickSeriesType seriesType,
        BanPickMatchMode mode
) {
    public BanPickCreateRoomRequest(BanPickSeriesType seriesType) {
        this(seriesType, null);
    }
}
