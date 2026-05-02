package com.example.demo.repository;

import com.example.demo.entity.DraftHistory;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DraftHistoryRepository extends JpaRepository<DraftHistory, Long> {
    List<DraftHistory> findByBlueUserOrRedUserOrderByCreatedAtDesc(User blueUser, User redUser);

    Optional<DraftHistory> findFirstByRoomCodeOrderByCreatedAtDesc(String roomCode);
}
