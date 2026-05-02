package com.example.demo.repository;

import com.example.demo.entity.BanPickRoom;
import com.example.demo.entity.BanPickRoomStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BanPickRoomRepository extends JpaRepository<BanPickRoom, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select room from BanPickRoom room where room.roomCode = :roomCode")
    Optional<BanPickRoom> findByRoomCodeForUpdate(@Param("roomCode") String roomCode);

    @Query("""
            select room.roomCode
            from BanPickRoom room
            where room.status = :status
              and (
                   (room.phaseDeadlineAt is not null and room.phaseDeadlineAt < :now)
                or (room.lineupDeadlineAt is not null and room.lineupDeadlineAt < :now)
              )
            """)
    List<String> findExpiredRoomCodes(@Param("status") BanPickRoomStatus status,
                                      @Param("now") LocalDateTime now);

    boolean existsByRoomCode(String roomCode);
}
