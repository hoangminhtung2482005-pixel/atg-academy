package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Đặc điểm tướng (Hero Attribute) — dữ liệu động, admin có thể thêm bớt.
 * Ví dụ: Cơ động, Chống chịu, Khống chế, Cấu rỉa, Hồi phục, Sát thương sốc cao...
 */
@Entity
@Table(name = "hero_attributes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HeroAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Tên đặc điểm hiển thị: Cơ động, Khống chế, Hồi phục... */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String iconUrl;

    private Integer sortOrder;
}
