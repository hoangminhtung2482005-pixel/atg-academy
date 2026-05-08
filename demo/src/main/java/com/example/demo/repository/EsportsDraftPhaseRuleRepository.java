package com.example.demo.repository;

import com.example.demo.entity.EsportsDraftPhaseRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EsportsDraftPhaseRuleRepository extends JpaRepository<EsportsDraftPhaseRule, Long> {

    Optional<EsportsDraftPhaseRule> findByFormatIdAndStepNumber(Long formatId, Integer stepNumber);

    List<EsportsDraftPhaseRule> findByFormatIdOrderByStepNumberAsc(Long formatId);
}
