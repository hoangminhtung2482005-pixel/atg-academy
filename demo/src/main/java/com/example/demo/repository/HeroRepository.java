package com.example.demo.repository;

import com.example.demo.entity.Hero;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HeroRepository extends JpaRepository<Hero, Long> {

    Optional<Hero> findFirstByNameIgnoreCase(String name);

    Optional<Hero> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Hero> findAllByOrderByNameAsc();

    /**
     * Lấy toàn bộ hero kèm roles và attributes trong 1 query duy nhất.
     * Dùng LEFT JOIN FETCH để tránh N+1 query.
     * DISTINCT để loại bỏ bản ghi trùng do Cartesian product của 2 JOIN.
     */
    @Query("""
            SELECT DISTINCT h FROM Hero h
            LEFT JOIN FETCH h.classes
            LEFT JOIN FETCH h.roles
            LEFT JOIN FETCH h.attributes
            ORDER BY h.name ASC
            """)
    List<Hero> findAllWithRolesAndAttributes();

    @EntityGraph(attributePaths = {"classes", "roles", "attributes"})
    Optional<Hero> findWithRolesAndAttributesBySlug(String slug);

    @Query("""
            SELECT DISTINCT h FROM Hero h
            LEFT JOIN FETCH h.classes
            LEFT JOIN FETCH h.roles
            LEFT JOIN FETCH h.attributes
            WHERE h.id = :id
            """)
    Optional<Hero> findByIdWithRolesAndAttributes(@Param("id") Long id);
}
