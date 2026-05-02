package com.example.demo.repository;

import com.example.demo.entity.HeroRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface HeroRoleRepository extends JpaRepository<HeroRole, Long> {

    List<HeroRole> findAllByOrderByCodeAsc();

    List<HeroRole> findByCodeIn(Collection<String> codes);
}
