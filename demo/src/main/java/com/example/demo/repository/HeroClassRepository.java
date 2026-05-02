package com.example.demo.repository;

import com.example.demo.entity.HeroClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface HeroClassRepository extends JpaRepository<HeroClass, Long> {

    List<HeroClass> findAllByOrderByNameAsc();

    List<HeroClass> findByNameIn(Collection<String> names);

    Optional<HeroClass> findByNameIgnoreCase(String name);
}
