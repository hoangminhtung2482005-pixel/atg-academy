package com.example.demo.repository;

import com.example.demo.entity.EsportsFranchise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EsportsFranchiseRepository extends JpaRepository<EsportsFranchise, Long> {

    Optional<EsportsFranchise> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}
