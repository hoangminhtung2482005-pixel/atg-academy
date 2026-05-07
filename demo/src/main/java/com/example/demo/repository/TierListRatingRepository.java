package com.example.demo.repository;

import com.example.demo.entity.TierListRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TierListRatingRepository extends JpaRepository<TierListRating, Long> {

    interface RecentCommunityTierListRatingSummary {
        Long getTierListId();

        Double getAverageRating();

        Long getRatingCount();

        Long getFiveStarCount();
    }

    /** TÃ¬m rating cá»§a má»™t user cho má»™t tier list cá»¥ thá»ƒ */
    Optional<TierListRating> findByTierListIdAndUserId(Long tierListId, String userId);

    /** Láº¥y táº¥t cáº£ rating cá»§a má»™t tier list */
    List<TierListRating> findByTierListId(Long tierListId);

    /** TÃ­nh trung bÃ¬nh sao cá»§a má»™t tier list */
    @Query("SELECT AVG(r.stars) FROM TierListRating r WHERE r.tierList.id = :tierListId")
    Double getAverageRating(@Param("tierListId") Long tierListId);

    /** Äáº¿m tá»•ng lÆ°á»£t Ä‘Ã¡nh giÃ¡ */
    long countByTierListId(Long tierListId);

    @Query("""
            SELECT r.tierList.id AS tierListId,
                   AVG(r.stars) AS averageRating,
                   COUNT(r.id) AS ratingCount,
                   SUM(CASE WHEN r.stars = 5 THEN 1 ELSE 0 END) AS fiveStarCount
            FROM TierListRating r
            WHERE r.tierList.isOfficial = false
              AND COALESCE(r.updatedAt, r.createdAt) >= :cutoff
            GROUP BY r.tierList.id, r.tierList.createdAt
            ORDER BY AVG(r.stars) DESC,
                     COUNT(r.id) DESC,
                     r.tierList.createdAt DESC,
                     r.tierList.id DESC
            """)
    List<RecentCommunityTierListRatingSummary> findRecentCommunityTierListRatingSummaries(@Param("cutoff") LocalDateTime cutoff);

    void deleteByTierListId(Long tierListId);
}
