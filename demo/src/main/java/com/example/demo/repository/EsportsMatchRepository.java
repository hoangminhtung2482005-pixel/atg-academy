package com.example.demo.repository;

import com.example.demo.entity.EsportsMatch;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EsportsMatchRepository extends JpaRepository<EsportsMatch, Long> {

    /** Lấy tất cả trận đấu sắp xếp theo ngày cũ → mới */
    List<EsportsMatch> findAllByOrderByMatchDateAsc();

    /** Lấy tất cả trận đấu sắp xếp theo ngày mới → cũ (cho Admin) */
    List<EsportsMatch> findAllByOrderByMatchDateDesc();

    List<EsportsMatch> findAllByOrderByMatchDateDesc(Pageable pageable);
}
