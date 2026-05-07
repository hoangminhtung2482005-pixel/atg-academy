package com.example.demo.repository;

import com.example.demo.entity.TierListAdminRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TierListAdminRatingRepository extends JpaRepository<TierListAdminRating, Long> {

    interface RecentCommunityTierListAdminRatingSummary {
        Long getTierListId();

        Double getAdminRating();
    }

    Optional<TierListAdminRating> findByTierListId(Long tierListId);

    @Query("""
            SELECT r.tierList.id AS tierListId,
                   r.ratingValue AS adminRating
            FROM TierListAdminRating r
            WHERE r.tierList.isOfficial = false
              AND COALESCE(r.updatedAt, r.createdAt) >= :cutoff
            ORDER BY r.ratingValue DESC,
                     r.tierList.createdAt DESC,
                     r.tierList.id DESC
            """)
    List<RecentCommunityTierListAdminRatingSummary> findRecentCommunityTierListAdminRatingSummaries(@Param("cutoff") LocalDateTime cutoff);

    void deleteByTierListId(Long tierListId);
}
