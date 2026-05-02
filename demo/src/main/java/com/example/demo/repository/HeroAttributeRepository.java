package com.example.demo.repository;

import com.example.demo.entity.HeroAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface HeroAttributeRepository extends JpaRepository<HeroAttribute, Long> {

    interface HeroAttributeUsageView {
        Long getId();

        String getName();

        String getDescription();

        String getIconUrl();

        Integer getSortOrder();

        long getUsageCount();
    }

    List<HeroAttribute> findAllByOrderByNameAsc();

    List<HeroAttribute> findByNameIn(Collection<String> names);

    Optional<HeroAttribute> findByNameIgnoreCase(String name);

    @Query("""
            SELECT COUNT(h) FROM Hero h
            JOIN h.attributes attribute
            WHERE attribute.id = :attributeId
            """)
    long countHeroUsage(@Param("attributeId") Long attributeId);

    @Query(value = """
            SELECT
                a.id AS id,
                a.name AS name,
                a.description AS description,
                a.icon_url AS iconUrl,
                a.sort_order AS sortOrder,
                COUNT(m.hero_id) AS usageCount
            FROM hero_attributes a
            LEFT JOIN hero_attribute_mapping m ON m.attribute_id = a.id
            GROUP BY a.id, a.name, a.description, a.icon_url, a.sort_order
            ORDER BY COALESCE(a.sort_order, 2147483647), LOWER(a.name)
            """, nativeQuery = true)
    List<HeroAttributeUsageView> findAllWithUsageCount();

    @Modifying
    @Query(value = "DELETE FROM hero_attribute_mapping WHERE attribute_id = :attributeId", nativeQuery = true)
    int deleteMappingsByAttributeId(@Param("attributeId") Long attributeId);
}
