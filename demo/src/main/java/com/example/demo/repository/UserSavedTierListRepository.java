package com.example.demo.repository;

import com.example.demo.entity.UserSavedTierList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSavedTierListRepository extends JpaRepository<UserSavedTierList, Long> {

    Optional<UserSavedTierList> findByUserIdAndTierListId(Long userId, Long tierListId);

    List<UserSavedTierList> findByUserIdAndTierListIdIn(Long userId, Collection<Long> tierListIds);

    List<UserSavedTierList> findByUserIdOrderBySavedAtDesc(Long userId);

    void deleteByUserIdAndTierListId(Long userId, Long tierListId);

    void deleteByTierListId(Long tierListId);
}
