package com.example.demo.repository;

import com.example.demo.entity.TierListRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TierListRatingRepository extends JpaRepository<TierListRating, Long> {

    /** Tìm rating của một user cho một tier list cụ thể */
    Optional<TierListRating> findByTierListIdAndUserId(Long tierListId, String userId);

    /** Lấy tất cả rating của một tier list */
    List<TierListRating> findByTierListId(Long tierListId);

    /** Tính trung bình sao của một tier list */
    @Query("SELECT AVG(r.stars) FROM TierListRating r WHERE r.tierList.id = :tierListId")
    Double getAverageRating(@Param("tierListId") Long tierListId);

    /** Đếm tổng lượt đánh giá */
    long countByTierListId(Long tierListId);
}
