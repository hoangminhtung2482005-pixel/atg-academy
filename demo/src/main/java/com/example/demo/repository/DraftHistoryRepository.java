package com.example.demo.repository;

import com.example.demo.entity.DraftHistory;
import com.example.demo.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface DraftHistoryRepository extends JpaRepository<DraftHistory, Long> {
    List<DraftHistory> findByBlueUserOrRedUserOrderByCreatedAtDesc(User blueUser, User redUser);

    @Query("""
            SELECT h
            FROM DraftHistory h
            WHERE h.blueUser = :user OR h.redUser = :user
            ORDER BY COALESCE(h.resultRecordedAt, h.createdAt) DESC,
                     h.createdAt DESC,
                     h.id DESC
            """)
    List<DraftHistory> findByParticipantOrderByRecentDesc(@Param("user") User user);

    @Query("""
            SELECT h
            FROM DraftHistory h
            WHERE h.blueUser = :user OR h.redUser = :user
            ORDER BY COALESCE(h.resultRecordedAt, h.createdAt) DESC,
                     h.createdAt DESC,
                     h.id DESC
            """)
    List<DraftHistory> findRecentByParticipantOrderByRecentDesc(@Param("user") User user, Pageable pageable);

    @Query("""
            SELECT h
            FROM DraftHistory h
            WHERE COALESCE(h.resultRecordedAt, h.createdAt) >= :windowStart
              AND COALESCE(h.resultRecordedAt, h.createdAt) < :windowEnd
            """)
    List<DraftHistory> findCompletedBetween(@Param("windowStart") LocalDateTime windowStart,
                                            @Param("windowEnd") LocalDateTime windowEnd);

    @Query("""
            SELECT COUNT(h)
            FROM DraftHistory h
            WHERE COALESCE(h.resultRecordedAt, h.createdAt) >= :windowStart
              AND COALESCE(h.resultRecordedAt, h.createdAt) < :windowEnd
              AND (
                    (h.blueUser.id = :lowerUserId AND h.redUser.id = :higherUserId)
                 OR (h.blueUser.id = :higherUserId AND h.redUser.id = :lowerUserId)
              )
            """)
    long countCompletedPairMatchesWithinWindow(@Param("lowerUserId") Long lowerUserId,
                                               @Param("higherUserId") Long higherUserId,
                                               @Param("windowStart") LocalDateTime windowStart,
                                               @Param("windowEnd") LocalDateTime windowEnd);

    Optional<DraftHistory> findFirstByRoomCodeOrderByCreatedAtDesc(String roomCode);
}
