package com.example.demo.repository;

import com.example.demo.entity.BanPickRankResetLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface BanPickRankResetLogRepository extends JpaRepository<BanPickRankResetLog, Long> {

    boolean existsByScheduledDate(LocalDate scheduledDate);

    Optional<BanPickRankResetLog> findByScheduledDate(LocalDate scheduledDate);

    Optional<BanPickRankResetLog> findFirstByOrderByExecutedAtDescIdDesc();
}
