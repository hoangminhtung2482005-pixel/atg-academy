package com.example.demo.repository;

import com.example.demo.entity.EsportsTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EsportsTeamRepository extends JpaRepository<EsportsTeam, Long> {

    Optional<EsportsTeam> findByTeamCode(String teamCode);

    /** Lấy danh sách đội sắp xếp theo điểm giảm dần (cho bảng xếp hạng) */
    List<EsportsTeam> findAllByOrderByScoreDesc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update EsportsTeam team
            set team.score = 1200.0,
                team.gameWins = 0,
                team.gameLosses = 0,
                team.matchWins = 0,
                team.matchLosses = 0
            """)
    int resetAllRankingFields();
}
