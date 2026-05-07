package com.example.demo.repository;

import com.example.demo.entity.TierList;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TierListRepository extends JpaRepository<TierList, Long> {

    /** Lấy Tier List chính thức của Admin */
    Optional<TierList> findFirstByIsOfficialTrueOrderByUpdatedAtDesc();

    /** Lấy danh sách Tier List cộng đồng (không official), sắp xếp mới nhất */
    List<TierList> findByIsOfficialFalseOrderByCreatedAtDesc();

    List<TierList> findByIsOfficialFalseOrderByCreatedAtDesc(Pageable pageable);

    long countByAuthorIdAndIsOfficialFalse(Long authorId);

    List<TierList> findByAuthorIdAndIsOfficialFalseOrderByCreatedAtDesc(Long authorId);
}
