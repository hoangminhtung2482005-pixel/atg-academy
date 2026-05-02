package com.example.demo.repository;

import com.example.demo.entity.EsportsTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EsportsTeamRepository extends JpaRepository<EsportsTeam, Long> {

    Optional<EsportsTeam> findByTeamCode(String teamCode);

    /** Lấy danh sách đội sắp xếp theo điểm giảm dần (cho bảng xếp hạng) */
    List<EsportsTeam> findAllByOrderByScoreDesc();
}
