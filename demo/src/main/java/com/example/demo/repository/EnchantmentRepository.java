package com.example.demo.repository;

import com.example.demo.entity.Enchantment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnchantmentRepository extends JpaRepository<Enchantment, Long> {

    Optional<Enchantment> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Enchantment> findAllByOrderByTenAsc();
}
