package com.example.demo.repository;

import com.example.demo.entity.GuideArcana;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuideArcanaRepository extends JpaRepository<GuideArcana, Long> {

    List<GuideArcana> findByHuongDanIdOrderByThuTuAsc(Long huongDanId);

    void deleteByHuongDanId(Long huongDanId);
}
