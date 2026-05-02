package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "draft_histories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DraftHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String roomCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blue_user_id", nullable = false)
    private User blueUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "red_user_id", nullable = false)
    private User redUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_user_id")
    private User winnerUser;

    @Column(columnDefinition = "TEXT")
    private String bluePicks;

    @Column(columnDefinition = "TEXT")
    private String redPicks;

    @Column(columnDefinition = "TEXT")
    private String blueBans;

    @Column(columnDefinition = "TEXT")
    private String redBans;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime resultRecordedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
