package com.example.demo.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class BanPickSeasonResetScheduler {

    private final BanPickSeasonResetService banPickSeasonResetService;

    public BanPickSeasonResetScheduler(BanPickSeasonResetService banPickSeasonResetService) {
        this.banPickSeasonResetService = banPickSeasonResetService;
    }

    @Scheduled(cron = "${banpick.season-reset.scheduler-cron:0 0 0 * * *}")
    public void runDailyResetCheck() {
        banPickSeasonResetService.runScheduledResetIfDue(LocalDate.now());
    }
}
