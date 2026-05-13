package com.example.demo.repository;

import com.example.demo.entity.EsportsTournamentTeam;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EsportsTournamentTeamRepository extends JpaRepository<EsportsTournamentTeam, Long> {

    @EntityGraph(attributePaths = {"team", "tournament", "tournament.franchise"})
    List<EsportsTournamentTeam> findByTournamentId(Long tournamentId);

    @EntityGraph(attributePaths = {"team", "tournament", "tournament.franchise"})
    Optional<EsportsTournamentTeam> findByTournamentIdAndTeamId(Long tournamentId, Long teamId);

    boolean existsByTournamentIdAndTeamId(Long tournamentId, Long teamId);

    long countByTournamentId(Long tournamentId);

    void deleteByTournamentIdAndTeamId(Long tournamentId, Long teamId);
}
