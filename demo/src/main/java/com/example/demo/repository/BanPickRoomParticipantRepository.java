package com.example.demo.repository;

import com.example.demo.entity.BanPickRoom;
import com.example.demo.entity.BanPickRoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BanPickRoomParticipantRepository extends JpaRepository<BanPickRoomParticipant, Long> {
    List<BanPickRoomParticipant> findByRoomOrderByJoinedAtAsc(BanPickRoom room);
}
