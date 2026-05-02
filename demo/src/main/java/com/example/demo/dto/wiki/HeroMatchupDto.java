package com.example.demo.dto.wiki;

import com.example.demo.entity.HeroMatchup;
import com.example.demo.entity.HeroMatchupDifficulty;
import com.example.demo.entity.HeroMatchupType;

public record HeroMatchupDto(
        Long id,
        HeroMatchupType matchupType,
        HeroMatchupDifficulty difficulty,
        String notes,
        HeroSummaryDto targetHero
) {
    public static HeroMatchupDto from(HeroMatchup matchup) {
        if (matchup == null) {
            return null;
        }

        return new HeroMatchupDto(
                matchup.getId(),
                matchup.getMatchupType(),
                matchup.getDifficulty(),
                matchup.getNotes(),
                HeroSummaryDto.from(matchup.getTargetHero())
        );
    }
}
