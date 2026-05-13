package com.example.demo.repository;

import com.example.demo.entity.EsportsTournament;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EsportsTournamentRepository extends JpaRepository<EsportsTournament, Long> {

    @Override
    @EntityGraph(attributePaths = {"franchise"})
    List<EsportsTournament> findAll();

    @Override
    @EntityGraph(attributePaths = {"franchise"})
    Optional<EsportsTournament> findById(Long id);

    @EntityGraph(attributePaths = {"franchise"})
    Optional<EsportsTournament> findBySlugIgnoreCase(String slug);

    @EntityGraph(attributePaths = {"franchise"})
    Optional<EsportsTournament> findByNameIgnoreCase(String name);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, Long id);

    long countByFranchiseId(Long franchiseId);

    @EntityGraph(attributePaths = {"franchise"})
    @Query("""
            select tournament
            from EsportsTournament tournament
            left join tournament.franchise franchise
            where (:franchiseId is null or franchise.id = :franchiseId)
              and (:franchiseCode is null or upper(franchise.code) = upper(:franchiseCode))
            """)
    List<EsportsTournament> findAllForListing(@Param("franchiseId") Long franchiseId,
                                              @Param("franchiseCode") String franchiseCode);
}
