package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    private EsportsTournament tournament;

    @Transient
    private Long tournamentIdPayload;

    @Transient
    public Long getTournamentId() {
        return tournament != null && tournament.getId() != null
                ? tournament.getId()
                : tournamentIdPayload;
    }

    public void setTournamentId(Long tournamentId) {
        this.tournamentIdPayload = tournamentId;
    }

    @Transient
    public String getTournamentName() {
        return tournament != null ? tournament.getName() : null;
    }

    @Transient
    public String getTournamentSlug() {
        return tournament != null ? tournament.getSlug() : null;
    }

    @Transient
    public String getTournamentTierLevel() {
        return tournament != null ? tournament.getTierLevel() : null;
    }

    @Transient
    public Integer getTournamentAerTier() {
        return tournament != null ? tournament.getAerTier() : null;
    }

    @Transient
    public String getTournamentFranchiseCode() {
        return tournament != null && tournament.getFranchise() != null
                ? tournament.getFranchise().getCode()
                : null;
    }

    @Transient
    public String getTournamentFranchiseName() {
        return tournament != null && tournament.getFranchise() != null
                ? tournament.getFranchise().getName()
                : null;
    }
}
