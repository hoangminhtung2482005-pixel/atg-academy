package com.example.demo.repository;

import com.example.demo.entity.EsportsMatchGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EsportsMatchGameRepository extends JpaRepository<EsportsMatchGame, Long> {

    List<EsportsMatchGame> findByMatchIdOrderByGameNumberAsc(Long matchId);

    boolean existsByMatchIdAndGameNumber(Long matchId, Integer gameNumber);

    boolean existsByMatchIdAndGameNumberAndIdNot(Long matchId, Integer gameNumber, Long id);
}
