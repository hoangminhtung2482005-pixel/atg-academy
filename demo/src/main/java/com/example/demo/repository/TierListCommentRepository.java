package com.example.demo.repository;

import com.example.demo.entity.TierListComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TierListCommentRepository extends JpaRepository<TierListComment, Long> {

    List<TierListComment> findByTierListIdOrderByCreatedAtAsc(Long tierListId);

    long countByTierListId(Long tierListId);

    void deleteByTierListId(Long tierListId);
}
