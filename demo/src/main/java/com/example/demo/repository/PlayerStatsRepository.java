package com.example.demo.repository;

import com.example.demo.entity.PlayerStats;
import com.example.demo.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerStatsRepository extends JpaRepository<PlayerStats, Long> {
    Optional<PlayerStats> findByUser(User user);

    Optional<PlayerStats> findByUserEmail(String email);

    List<PlayerStats> findAllByOrderByRatingDescTotalMatchesDesc(Pageable pageable);
}
