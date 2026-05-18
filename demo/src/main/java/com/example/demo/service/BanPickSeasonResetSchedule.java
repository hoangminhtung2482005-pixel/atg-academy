package com.example.demo.service;

import com.example.demo.entity.BanPickSeasonResetType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class BanPickSeasonResetSchedule {

    public BanPickSeasonResetType determineResetType(LocalDate date) {
        return determineResetType(
                date,
                BanPickRatingDefaults.SEASON_SOFT_RESET_MONTHS,
                BanPickRatingDefaults.SEASON_HARD_RESET_MONTHS,
                BanPickRatingDefaults.SEASON_HARD_PRIORITY_OVER_SOFT
        );
    }

    public BanPickSeasonResetType determineResetType(LocalDate date,
                                                     List<Integer> softResetMonths,
                                                     List<Integer> hardResetMonths,
                                                     boolean hardPriorityOverSoft) {
        if (date == null || date.getDayOfMonth() != 1) {
            return BanPickSeasonResetType.NONE;
        }

        int monthValue = date.getMonthValue();
        boolean softMatched = softResetMonths != null && softResetMonths.contains(monthValue);
        boolean hardMatched = hardResetMonths != null && hardResetMonths.contains(monthValue);

        if (softMatched && hardMatched) {
            return hardPriorityOverSoft ? BanPickSeasonResetType.HARD : BanPickSeasonResetType.SOFT;
        }
        if (hardMatched) {
            return BanPickSeasonResetType.HARD;
        }
        if (softMatched) {
            return BanPickSeasonResetType.SOFT;
        }
        return BanPickSeasonResetType.NONE;
    }

    public Optional<ScheduledReset> findNextReset(LocalDate fromDate,
                                                  List<Integer> softResetMonths,
                                                  List<Integer> hardResetMonths,
                                                  boolean hardPriorityOverSoft) {
        LocalDate startDate = fromDate != null ? fromDate : LocalDate.now();
        for (int offset = 0; offset <= 370; offset += 1) {
            LocalDate candidate = startDate.plusDays(offset);
            BanPickSeasonResetType type = determineResetType(candidate, softResetMonths, hardResetMonths, hardPriorityOverSoft);
            if (type.isExecutable()) {
                return Optional.of(new ScheduledReset(candidate, type));
            }
        }
        return Optional.empty();
    }

    public record ScheduledReset(
            LocalDate scheduledDate,
            BanPickSeasonResetType resetType
    ) {
    }
}
