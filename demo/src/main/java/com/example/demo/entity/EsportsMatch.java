package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "esports_matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EsportsMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ngày giờ diễn ra trận đấu */
    @Column(nullable = false)
    private LocalDateTime matchDate;

    /** Mã đội 1 (VD: FS, SGP...) */
    @Column(nullable = false)
    private String team1Code;

    /** Mã đội 2 */
    @Column(nullable = false)
    private String team2Code;

    /** Số ván thắng của đội 1 */
    @Column(nullable = false)
    private Integer score1;

    /** Số ván thắng của đội 2 */
    @Column(nullable = false)
    private Integer score2;

    /** Tier giải đấu: 0 (quốc tế), 1 (khu vực), 2 (nhỏ) */
    @Column(nullable = false)
    private String tier = "1";

    /** Giai đoạn: ck, playoff, bang, vongloai */
    @Column(nullable = false)
    private String stage = "bang";
}
