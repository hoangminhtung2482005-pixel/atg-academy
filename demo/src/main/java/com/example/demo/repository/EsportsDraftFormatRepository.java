package com.example.demo.repository;

import com.example.demo.entity.EsportsDraftFormat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EsportsDraftFormatRepository extends JpaRepository<EsportsDraftFormat, Long> {

    Optional<EsportsDraftFormat> findByCode(String code);

    Optional<EsportsDraftFormat> findFirstByDefaultFormatTrueAndActiveTrueOrderByIdAsc();
}
