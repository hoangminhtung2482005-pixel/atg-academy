package com.example.demo.repository;

import com.example.demo.entity.EsportsMatch;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EsportsMatchRepository extends JpaRepository<EsportsMatch, Long> {

    /** Lấy tất cả trận đấu sắp xếp theo ngày cũ → mới */
    @EntityGraph(attributePaths = {"tournament", "tournament.franchise"})
    List<EsportsMatch> findAllByOrderByMatchDateAsc();

    /** Lấy tất cả trận đấu sắp xếp theo ngày mới → cũ (cho Admin) */
    @EntityGraph(attributePaths = {"tournament", "tournament.franchise"})
    List<EsportsMatch> findAllByOrderByMatchDateDesc();

    @EntityGraph(attributePaths = {"tournament", "tournament.franchise"})
    List<EsportsMatch> findAllByOrderByMatchDateDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"tournament", "tournament.franchise"})
    Optional<EsportsMatch> findById(Long id);

    @Query("""
            select count(esportsMatch)
            from EsportsMatch esportsMatch
            where esportsMatch.tournament.id = :tournamentId
            """)
    long countByTournamentId(@Param("tournamentId") Long tournamentId);
}
