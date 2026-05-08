package com.example.demo.repository;

import com.example.demo.entity.EsportsMatchGameLineup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EsportsMatchGameLineupRepository extends JpaRepository<EsportsMatchGameLineup, Long> {

    List<EsportsMatchGameLineup> findByGameIdOrderByTeamSideAscPositionNumberAsc(Long gameId);
}
