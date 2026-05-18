package com.example.demo.service;

import com.example.demo.entity.BanPickSeasonResetType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BanPickSeasonResetScheduleTest {

    private final BanPickSeasonResetSchedule schedule = new BanPickSeasonResetSchedule();

    @Test
    void determineResetTypeMapsFixedServerSchedule() {
        assertThat(schedule.determineResetType(LocalDate.of(2026, 2, 1))).isEqualTo(BanPickSeasonResetType.SOFT);
        assertThat(schedule.determineResetType(LocalDate.of(2026, 4, 1))).isEqualTo(BanPickSeasonResetType.SOFT);
        assertThat(schedule.determineResetType(LocalDate.of(2026, 6, 1))).isEqualTo(BanPickSeasonResetType.HARD);
        assertThat(schedule.determineResetType(LocalDate.of(2026, 8, 1))).isEqualTo(BanPickSeasonResetType.SOFT);
        assertThat(schedule.determineResetType(LocalDate.of(2026, 10, 1))).isEqualTo(BanPickSeasonResetType.SOFT);
        assertThat(schedule.determineResetType(LocalDate.of(2026, 12, 1))).isEqualTo(BanPickSeasonResetType.HARD);
    }

    @Test
    void determineResetTypeReturnsNoneOutsideConfiguredDaysAndKeepsHardPriority() {
        assertThat(schedule.determineResetType(LocalDate.of(2026, 6, 2))).isEqualTo(BanPickSeasonResetType.NONE);
        assertThat(schedule.determineResetType(LocalDate.of(2026, 12, 2))).isEqualTo(BanPickSeasonResetType.NONE);
        assertThat(schedule.determineResetType(LocalDate.of(2026, 1, 1))).isEqualTo(BanPickSeasonResetType.NONE);
        assertThat(schedule.determineResetType(LocalDate.of(2026, 3, 1))).isEqualTo(BanPickSeasonResetType.NONE);
        assertThat(schedule.determineResetType(LocalDate.of(2026, 6, 1))).isNotEqualTo(BanPickSeasonResetType.SOFT);
        assertThat(schedule.determineResetType(LocalDate.of(2026, 12, 1))).isNotEqualTo(BanPickSeasonResetType.SOFT);
    }
}
