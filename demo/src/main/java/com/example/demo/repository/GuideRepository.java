package com.example.demo.repository;

import com.example.demo.entity.Guide;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuideRepository extends JpaRepository<Guide, Long> {

    List<Guide> findByStatusIgnoreCaseOrderByCreatedAtDesc(String status, Pageable pageable);

    @Query("""
            SELECT guide FROM Guide guide
            WHERE guide.hero.id = :heroId
            AND UPPER(COALESCE(guide.status, 'PUBLISHED')) = 'PUBLISHED'
            ORDER BY guide.publishedAt DESC, guide.createdAt DESC
            """)
    List<Guide> findPublishedByHeroId(@Param("heroId") Long heroId, Pageable pageable);

    @Query("""
            SELECT COUNT(guide) FROM Guide guide
            WHERE guide.author.id = :authorId
            AND UPPER(COALESCE(guide.status, 'PUBLISHED')) = 'PUBLISHED'
            """)
    long countPublishedByAuthorId(@Param("authorId") Long authorId);

    @Query("""
            SELECT guide FROM Guide guide
            WHERE guide.author.id = :authorId
            AND UPPER(COALESCE(guide.status, 'PUBLISHED')) = 'PUBLISHED'
            ORDER BY COALESCE(guide.publishedAt, guide.createdAt) DESC, guide.createdAt DESC
            """)
    List<Guide> findPublishedByAuthorIdOrderByLatest(@Param("authorId") Long authorId);
}
