package com.example.demo.repository;

import com.example.demo.entity.TierListAdminRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TierListAdminRatingRepository extends JpaRepository<TierListAdminRating, Long> {

    Optional<TierListAdminRating> findByTierListId(Long tierListId);

    void deleteByTierListId(Long tierListId);
}
