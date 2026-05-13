package com.example.demo.repository;

import com.example.demo.dto.esports.EsportsDraftTournamentAggregate;
import com.example.demo.dto.esports.EsportsDraftTournamentScopeAggregate;
import com.example.demo.entity.EsportsGameDraft;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EsportsGameDraftRepository extends JpaRepository<EsportsGameDraft, Long> {

    @EntityGraph(attributePaths = {"match", "blueTeam", "redTeam", "winnerTeam"})
    List<EsportsGameDraft> findByMatchIdOrderByGameNumberAsc(Long matchId);

    @EntityGraph(attributePaths = {"match", "blueTeam", "redTeam", "winnerTeam"})
    List<EsportsGameDraft> findByMatchId(Long matchId);

    @Override
    @EntityGraph(attributePaths = {"match", "blueTeam", "redTeam", "winnerTeam"})
    Optional<EsportsGameDraft> findById(Long id);

    boolean existsByMatchIdAndGameNumber(Long matchId, Integer gameNumber);

    boolean existsByMatchIdAndGameNumberAndIdNot(Long matchId, Integer gameNumber, Long id);

    void deleteByMatchId(Long matchId);

    @Query("""
            select draft
            from EsportsGameDraft draft
            join fetch draft.match esportsMatch
            join fetch draft.blueTeam blueTeam
            join fetch draft.redTeam redTeam
            left join fetch draft.winnerTeam winnerTeam
            where (:tournamentTier is null or esportsMatch.tier = :tournamentTier)
              and (:teamCode is null
                    or upper(blueTeam.teamCode) = :teamCode
                    or upper(redTeam.teamCode) = :teamCode)
              and (:dateFrom is null or esportsMatch.matchDate >= :dateFrom)
              and (:dateTo is null or esportsMatch.matchDate <= :dateTo)
            order by esportsMatch.matchDate asc, draft.gameNumber asc
            """)
    List<EsportsGameDraft> findAllForAnalytics(@Param("tournamentTier") String tournamentTier,
                                               @Param("teamCode") String teamCode,
                                               @Param("dateFrom") LocalDateTime dateFrom,
                                               @Param("dateTo") LocalDateTime dateTo);

    @Query("""
            select draft
            from EsportsGameDraft draft
            join fetch draft.match esportsMatch
            join fetch draft.blueTeam blueTeam
            join fetch draft.redTeam redTeam
            left join fetch draft.winnerTeam winnerTeam
            where (:tournamentTier is null or esportsMatch.tier = :tournamentTier)
              and (:matchId is null or esportsMatch.id = :matchId)
              and (:dateFrom is null or esportsMatch.matchDate >= :dateFrom)
              and (:dateTo is null or esportsMatch.matchDate <= :dateTo)
            order by esportsMatch.matchDate asc, esportsMatch.id asc, draft.gameNumber asc
            """)
    List<EsportsGameDraft> findAllForExport(@Param("tournamentTier") String tournamentTier,
                                            @Param("matchId") Long matchId,
                                            @Param("dateFrom") LocalDateTime dateFrom,
                                            @Param("dateTo") LocalDateTime dateTo);

    @Query("""
            select new com.example.demo.dto.esports.EsportsDraftTournamentAggregate(
                coalesce(esportsMatch.tier, '1'),
                max(esportsMatch.matchDate)
            )
            from EsportsGameDraft draft
            join draft.match esportsMatch
            group by coalesce(esportsMatch.tier, '1')
            order by max(esportsMatch.matchDate) desc
            """)
    List<EsportsDraftTournamentAggregate> findDraftTournamentsOrderByLatestMatchDesc();

    @Query("""
            select draft
            from EsportsGameDraft draft
            join fetch draft.match esportsMatch
            join fetch draft.blueTeam blueTeam
            join fetch draft.redTeam redTeam
            left join fetch draft.winnerTeam winnerTeam
            left join fetch esportsMatch.tournament tournament
            left join fetch tournament.franchise franchise
            where ((:tournamentId is not null and tournament.id = :tournamentId)
                    or (:tournamentId is null and (:tournamentTier is null or esportsMatch.tier = :tournamentTier)))
              and (:teamCode is null
                    or upper(blueTeam.teamCode) = :teamCode
                    or upper(redTeam.teamCode) = :teamCode)
              and (:dateFrom is null or esportsMatch.matchDate >= :dateFrom)
              and (:dateTo is null or esportsMatch.matchDate <= :dateTo)
            order by esportsMatch.matchDate asc, draft.gameNumber asc
            """)
    List<EsportsGameDraft> findAllForAnalyticsScope(@Param("tournamentId") Long tournamentId,
                                                    @Param("tournamentTier") String tournamentTier,
                                                    @Param("teamCode") String teamCode,
                                                    @Param("dateFrom") LocalDateTime dateFrom,
                                                    @Param("dateTo") LocalDateTime dateTo);

    @Query("""
            select draft
            from EsportsGameDraft draft
            join fetch draft.match esportsMatch
            join fetch draft.blueTeam blueTeam
            join fetch draft.redTeam redTeam
            left join fetch draft.winnerTeam winnerTeam
            left join fetch esportsMatch.tournament tournament
            left join fetch tournament.franchise franchise
            where ((:tournamentId is not null and tournament.id = :tournamentId)
                    or (:tournamentId is null and (:tournamentTier is null or esportsMatch.tier = :tournamentTier)))
              and (:matchId is null or esportsMatch.id = :matchId)
              and (:dateFrom is null or esportsMatch.matchDate >= :dateFrom)
              and (:dateTo is null or esportsMatch.matchDate <= :dateTo)
            order by esportsMatch.matchDate asc, esportsMatch.id asc, draft.gameNumber asc
            """)
    List<EsportsGameDraft> findAllForExportScope(@Param("tournamentId") Long tournamentId,
                                                 @Param("tournamentTier") String tournamentTier,
                                                 @Param("matchId") Long matchId,
                                                 @Param("dateFrom") LocalDateTime dateFrom,
                                                 @Param("dateTo") LocalDateTime dateTo);

    @Query("""
            select new com.example.demo.dto.esports.EsportsDraftTournamentScopeAggregate(
                tournament.id,
                tournament.name,
                case
                    when tournament.id is not null then str(coalesce(tournament.aerTier, 1))
                    else coalesce(esportsMatch.tier, '1')
                end,
                franchise.code,
                max(esportsMatch.matchDate)
            )
            from EsportsGameDraft draft
            join draft.match esportsMatch
            left join esportsMatch.tournament tournament
            left join tournament.franchise franchise
            group by tournament.id,
                     tournament.name,
                     case
                         when tournament.id is not null then str(coalesce(tournament.aerTier, 1))
                         else coalesce(esportsMatch.tier, '1')
                     end,
                     franchise.code
            order by max(esportsMatch.matchDate) desc
            """)
    List<EsportsDraftTournamentScopeAggregate> findDraftTournamentScopesOrderByLatestMatchDesc();
}
