package com.example.demo.service;

import com.example.demo.dto.esports.EsportsHeroBanStatResponse;
import com.example.demo.dto.esports.EsportsHeroPickStatAggregate;
import com.example.demo.dto.esports.EsportsHeroStatResponse;
import com.example.demo.dto.esports.EsportsTournamentOptionResponse;
import com.example.demo.entity.BanPickTeamSide;
import com.example.demo.repository.EsportsMatchDraftActionRepository;
import com.example.demo.util.EsportsTournamentCatalog;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

@Service
@Transactional(readOnly = true)
public class EsportsDataService {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;

    private final EsportsMatchDraftActionRepository esportsMatchDraftActionRepository;

    public EsportsDataService(EsportsMatchDraftActionRepository esportsMatchDraftActionRepository) {
        this.esportsMatchDraftActionRepository = esportsMatchDraftActionRepository;
    }

    public List<EsportsTournamentOptionResponse> getAvailableTournaments() {
        Map<String, EsportsTournamentOptionResponse> uniqueTournaments = new LinkedHashMap<>();

        esportsMatchDraftActionRepository.findDraftTournamentsOrderByLatestMatchDesc()
                .forEach(tournament -> {
                    String tournamentTier = tournament.tournamentTier();
                    String tournamentName = EsportsTournamentCatalog.resolveTournamentName(tournamentTier);
                    uniqueTournaments.putIfAbsent(
                            tournamentName,
                            new EsportsTournamentOptionResponse(tournamentName, tournamentTier)
                    );
                });

        return uniqueTournaments.values().stream().toList();
    }

    public List<EsportsHeroBanStatResponse> getTopBannedHeroes(String tournamentName, Integer limit) {
        return getTopHeroBanStats(tournamentName, limit, null);
    }

    public List<EsportsHeroBanStatResponse> getTopBlueBannedHeroes(String tournamentName, Integer limit) {
        return getTopHeroBanStats(tournamentName, limit, BanPickTeamSide.BLUE);
    }

    public List<EsportsHeroStatResponse> getHeroStats(String tournamentName) {
        String tournamentTier = resolveTournamentTier(tournamentName);

        Map<Long, HeroStatAccumulator> statsByHeroId = new LinkedHashMap<>();

        esportsMatchDraftActionRepository.findHeroPickStats(tournamentTier)
                .forEach(item -> statsByHeroId
                        .computeIfAbsent(item.heroId(), ignored -> new HeroStatAccumulator(item.heroId()))
                        .applyPickStats(item));

        esportsMatchDraftActionRepository.findHeroBanStats(tournamentTier)
                .forEach(item -> statsByHeroId
                        .computeIfAbsent(item.heroId(), ignored -> new HeroStatAccumulator(item.heroId()))
                        .applyBanStats(item.heroName(), item.heroAvatarUrl(), item.banCount()));

        return statsByHeroId.values().stream()
                .map(HeroStatAccumulator::toResponse)
                .sorted(Comparator
                        .comparing(EsportsHeroStatResponse::presenceCount, Comparator.reverseOrder())
                        .thenComparing(EsportsHeroStatResponse::pickCount, Comparator.reverseOrder())
                        .thenComparing(EsportsHeroStatResponse::banCount, Comparator.reverseOrder())
                        .thenComparing(response -> safeText(response.heroName()))
                        .thenComparing(response -> response.heroId() == null ? Long.MAX_VALUE : response.heroId()))
                .toList();
    }

    private List<EsportsHeroBanStatResponse> getTopHeroBanStats(String tournamentName,
                                                                Integer limit,
                                                                BanPickTeamSide teamSide) {
        String tournamentTier = resolveTournamentTier(tournamentName);
        String resolvedTournamentName = tournamentTier != null
                ? EsportsTournamentCatalog.resolveTournamentName(tournamentTier)
                : null;

        return esportsMatchDraftActionRepository.findTopHeroBanStats(
                        tournamentTier,
                        teamSide,
                        PageRequest.of(0, sanitizeLimit(limit))
                ).stream()
                .map(item -> new EsportsHeroBanStatResponse(
                        item.heroId(),
                        item.heroName(),
                        item.heroAvatarUrl(),
                        item.banCount(),
                        resolvedTournamentName
                ))
                .toList();
    }

    private String resolveTournamentTier(String tournamentName) {
        if (!StringUtils.hasText(tournamentName)) {
            return null;
        }

        String resolvedTier = EsportsTournamentCatalog.resolveTournamentTier(tournamentName);
        if (resolvedTier == null) {
            throw new IllegalArgumentException("tournamentName khong hop le.");
        }
        return resolvedTier;
    }

    private int sanitizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    private static long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private static double calculateWinRate(long wins, long total) {
        if (total <= 0L) {
            return 0D;
        }
        return (wins * 100.0D) / total;
    }

    private static final class HeroStatAccumulator {

        private final Long heroId;
        private String heroName;
        private String heroAvatarUrl;
        private long pickCount;
        private long pickWins;
        private long bluePickCount;
        private long blueWins;
        private long redPickCount;
        private long redWins;
        private long banCount;

        private HeroStatAccumulator(Long heroId) {
            this.heroId = heroId;
        }

        private void applyPickStats(EsportsHeroPickStatAggregate item) {
            updateHeroMeta(item.heroName(), item.heroAvatarUrl());
            pickCount = safeLong(item.pickCount());
            pickWins = safeLong(item.pickWins());
            bluePickCount = safeLong(item.bluePickCount());
            blueWins = safeLong(item.blueWins());
            redPickCount = safeLong(item.redPickCount());
            redWins = safeLong(item.redWins());
        }

        private void applyBanStats(String nextHeroName, String nextHeroAvatarUrl, Long nextBanCount) {
            updateHeroMeta(nextHeroName, nextHeroAvatarUrl);
            banCount = safeLong(nextBanCount);
        }

        private void updateHeroMeta(String nextHeroName, String nextHeroAvatarUrl) {
            if (!StringUtils.hasText(heroName) && StringUtils.hasText(nextHeroName)) {
                heroName = nextHeroName;
            }
            if (!StringUtils.hasText(heroAvatarUrl) && StringUtils.hasText(nextHeroAvatarUrl)) {
                heroAvatarUrl = nextHeroAvatarUrl;
            }
        }

        private EsportsHeroStatResponse toResponse() {
            long pickLosses = Math.max(0L, pickCount - pickWins);
            long blueLosses = Math.max(0L, bluePickCount - blueWins);
            long redLosses = Math.max(0L, redPickCount - redWins);

            return new EsportsHeroStatResponse(
                    heroId,
                    heroName,
                    heroAvatarUrl,
                    pickCount,
                    pickWins,
                    pickLosses,
                    calculateWinRate(pickWins, pickCount),
                    bluePickCount,
                    blueWins,
                    blueLosses,
                    calculateWinRate(blueWins, bluePickCount),
                    redPickCount,
                    redWins,
                    redLosses,
                    calculateWinRate(redWins, redPickCount),
                    banCount,
                    pickCount + banCount
            );
        }
    }
}
