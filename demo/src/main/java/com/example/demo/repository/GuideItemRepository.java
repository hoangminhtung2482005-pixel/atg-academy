package com.example.demo.repository;

import com.example.demo.entity.GuideItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuideItemRepository extends JpaRepository<GuideItem, Long> {

    List<GuideItem> findByHuongDanIdOrderByThuTuAsc(Long huongDanId);

    void deleteByHuongDanId(Long huongDanId);
}
