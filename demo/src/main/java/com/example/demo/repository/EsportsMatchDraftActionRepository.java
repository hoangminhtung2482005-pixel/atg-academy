package com.example.demo.repository;

import com.example.demo.dto.esports.EsportsDraftTournamentAggregate;
import com.example.demo.dto.esports.EsportsHeroBanStatAggregate;
import com.example.demo.dto.esports.EsportsHeroPickStatAggregate;
import com.example.demo.entity.BanPickTeamSide;
import com.example.demo.entity.EsportsMatchDraftAction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EsportsMatchDraftActionRepository extends JpaRepository<EsportsMatchDraftAction, Long> {

    List<EsportsMatchDraftAction> findByGameIdOrderByStepNumberAsc(Long gameId);

    boolean existsByGameIdAndStepNumber(Long gameId, Integer stepNumber);

    boolean existsByGameIdAndStepNumberAndIdNot(Long gameId, Integer stepNumber, Long id);

    boolean existsByGameIdAndHeroId(Long gameId, Long heroId);

    boolean existsByGameIdAndHeroIdAndIdNot(Long gameId, Long heroId, Long id);

    @Query("""
            select new com.example.demo.dto.esports.EsportsHeroBanStatAggregate(
                hero.id,
                hero.name,
                hero.avatarUrl,
                count(action.id)
            )
            from EsportsMatchDraftAction action
            join action.hero hero
            join action.game game
            join game.match esportsMatch
            where action.actionType = com.example.demo.entity.BanPickActionType.BAN
              and (:teamSide is null or action.teamSide = :teamSide)
              and (:tournamentTier is null or esportsMatch.tier = :tournamentTier)
            group by hero.id, hero.name, hero.avatarUrl
            order by count(action.id) desc, hero.name asc
            """)
    List<EsportsHeroBanStatAggregate> findTopHeroBanStats(@Param("tournamentTier") String tournamentTier,
                                                          @Param("teamSide") BanPickTeamSide teamSide,
                                                          Pageable pageable);

    @Query("""
            select new com.example.demo.dto.esports.EsportsHeroPickStatAggregate(
                hero.id,
                hero.name,
                hero.avatarUrl,
                count(action.id),
                coalesce(sum(case
                    when game.winnerTeam is not null and action.team.id = game.winnerTeam.id then 1
                    else 0
                end), 0),
                coalesce(sum(case
                    when action.teamSide = com.example.demo.entity.BanPickTeamSide.BLUE then 1
                    else 0
                end), 0),
                coalesce(sum(case
                    when action.teamSide = com.example.demo.entity.BanPickTeamSide.BLUE
                        and game.winnerTeam is not null
                        and action.team.id = game.winnerTeam.id then 1
                    else 0
                end), 0),
                coalesce(sum(case
                    when action.teamSide = com.example.demo.entity.BanPickTeamSide.RED then 1
                    else 0
                end), 0),
                coalesce(sum(case
                    when action.teamSide = com.example.demo.entity.BanPickTeamSide.RED
                        and game.winnerTeam is not null
                        and action.team.id = game.winnerTeam.id then 1
                    else 0
                end), 0)
            )
            from EsportsMatchDraftAction action
            join action.hero hero
            join action.game game
            join game.match esportsMatch
            where action.actionType = com.example.demo.entity.BanPickActionType.PICK
              and (:tournamentTier is null or esportsMatch.tier = :tournamentTier)
            group by hero.id, hero.name, hero.avatarUrl
            order by count(action.id) desc, hero.name asc
            """)
    List<EsportsHeroPickStatAggregate> findHeroPickStats(@Param("tournamentTier") String tournamentTier);

    @Query("""
            select new com.example.demo.dto.esports.EsportsHeroBanStatAggregate(
                hero.id,
                hero.name,
                hero.avatarUrl,
                count(action.id)
            )
            from EsportsMatchDraftAction action
            join action.hero hero
            join action.game game
            join game.match esportsMatch
            where action.actionType = com.example.demo.entity.BanPickActionType.BAN
              and (:tournamentTier is null or esportsMatch.tier = :tournamentTier)
            group by hero.id, hero.name, hero.avatarUrl
            order by count(action.id) desc, hero.name asc
            """)
    List<EsportsHeroBanStatAggregate> findHeroBanStats(@Param("tournamentTier") String tournamentTier);

    @Query("""
            select new com.example.demo.dto.esports.EsportsDraftTournamentAggregate(
                coalesce(esportsMatch.tier, '1'),
                max(esportsMatch.matchDate)
            )
            from EsportsMatchDraftAction action
            join action.game game
            join game.match esportsMatch
            group by coalesce(esportsMatch.tier, '1')
            order by max(esportsMatch.matchDate) desc
            """)
    List<EsportsDraftTournamentAggregate> findDraftTournamentsOrderByLatestMatchDesc();
}
