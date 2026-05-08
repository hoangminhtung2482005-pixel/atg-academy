package com.example.demo.repository;

import com.example.demo.entity.Spell;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpellRepository extends JpaRepository<Spell, Long> {

    List<Spell> findAllByOrderByIdAsc();

    Optional<Spell> findBySlug(String slug);
}
