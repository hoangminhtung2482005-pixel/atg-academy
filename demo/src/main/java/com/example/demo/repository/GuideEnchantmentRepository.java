package com.example.demo.repository;

import com.example.demo.entity.GuideEnchantment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuideEnchantmentRepository extends JpaRepository<GuideEnchantment, Long> {

    List<GuideEnchantment> findByHuongDanIdOrderByThuTuAsc(Long huongDanId);

    void deleteByHuongDanId(Long huongDanId);
}
