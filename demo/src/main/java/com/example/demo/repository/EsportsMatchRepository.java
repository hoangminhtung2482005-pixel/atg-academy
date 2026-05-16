package com.example.demo.repository;

import com.example.demo.entity.EsportsMatch;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EsportsMatchRepository extends JpaRepository<EsportsMatch, Long> {

    /** Lấy tất cả trận đấu sắp xếp theo ngày cũ → mới */
    @EntityGraph(attributePaths = {"tournament", "tournament.franchise"})
    List<EsportsMatch> findAllByOrderByMatchDateAscIdAsc();

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update EsportsMatch esportsMatch
            set esportsMatch.tier = :tier
            where esportsMatch.tournament.id = :tournamentId
              and (esportsMatch.tier is null or esportsMatch.tier <> :tier)
            """)
    int syncTierSnapshotByTournamentId(@Param("tournamentId") Long tournamentId,
                                       @Param("tier") String tier);
}
