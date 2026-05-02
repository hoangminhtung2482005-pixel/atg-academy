package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "esports_teams")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EsportsTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tên viết tắt của đội (VD: FS, SGP, FW...) */
    @Column(nullable = false, unique = true)
    private String teamCode;

    /** Tên đầy đủ (tùy chọn) */
    private String teamName;

    /** Đường dẫn logo đội (VD: /images/teams/FS.png) */
    private String logoUrl;

    /** Khu vực: RPL, AOG, GCS */
    @Column(nullable = false)
    private String region;

    /** Điểm Elo hiện tại */
    @Column(nullable = false)
    private Double score = 1200.0;

    /** Số ván thắng (game wins) */
    @Column(nullable = false)
    private Integer gameWins = 0;

    /** Số ván thua (game losses) */
    @Column(nullable = false)
    private Integer gameLosses = 0;

    /** Số trận thắng (match wins) */
    @Column(nullable = false)
    private Integer matchWins = 0;

    /** Số trận thua (match losses) */
    @Column(nullable = false)
    private Integer matchLosses = 0;
}
