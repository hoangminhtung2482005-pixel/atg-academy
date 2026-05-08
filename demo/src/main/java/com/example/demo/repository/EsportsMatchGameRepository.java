package com.example.demo.repository;

import com.example.demo.entity.EsportsMatchGame;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EsportsMatchGameRepository extends JpaRepository<EsportsMatchGame, Long> {

    List<EsportsMatchGame> findByMatchIdOrderByGameNumberAsc(Long matchId);

    List<EsportsMatchGame> findAllByDraftFormatIsNull();

    boolean existsByMatchIdAndGameNumber(Long matchId, Integer gameNumber);

    boolean existsByMatchIdAndGameNumberAndIdNot(Long matchId, Integer gameNumber, Long id);

    @Query("""
            select game
            from EsportsMatchGame game
            join fetch game.match esportsMatch
            join fetch game.blueTeam blueTeam
            join fetch game.redTeam redTeam
            left join fetch game.winnerTeam winnerTeam
            where (:tournamentTier is null or esportsMatch.tier = :tournamentTier)
              and (:teamCode is null
                    or upper(blueTeam.teamCode) = :teamCode
                    or upper(redTeam.teamCode) = :teamCode)
              and (:dateFrom is null or esportsMatch.matchDate >= :dateFrom)
              and (:dateTo is null or esportsMatch.matchDate <= :dateTo)
            order by esportsMatch.matchDate asc, game.gameNumber asc
            """)
    List<EsportsMatchGame> findAllForAnalytics(@Param("tournamentTier") String tournamentTier,
                                               @Param("teamCode") String teamCode,
                                               @Param("dateFrom") LocalDateTime dateFrom,
                                               @Param("dateTo") LocalDateTime dateTo);
}
