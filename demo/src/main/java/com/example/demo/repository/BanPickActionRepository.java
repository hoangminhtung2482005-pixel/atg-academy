package com.example.demo.repository;

import com.example.demo.entity.BanPickAction;
import com.example.demo.entity.BanPickRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BanPickActionRepository extends JpaRepository<BanPickAction, Long> {
    List<BanPickAction> findByRoomOrderByConfirmedAtAsc(BanPickRoom room);

    boolean existsByRoomAndHeroId(BanPickRoom room, Long heroId);

    void deleteByRoom(BanPickRoom room);
}
