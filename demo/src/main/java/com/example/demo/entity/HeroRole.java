package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Vị trí đi đường (Lane Role) — dữ liệu cố định.
 * DSL (Đường Solo Lẻ), JGL (Đi Rừng), MID (Đường Giữa), ADL (Xạ Thủ), SUP (Hỗ Trợ).
 */
@Entity
@Table(name = "hero_roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HeroRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    /** Mã rút gọn: DSL, JGL, MID, ADL, SUP */
    @Column(nullable = false, unique = true, length = 10)
    private String code;

    /** Tên hiển thị: Đường Solo Lẻ, Đi Rừng, Đường Giữa, Xạ Thủ, Hỗ Trợ */
    @Column(nullable = false, length = 50)
    private String name;
}
