package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "player_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Integer totalMatches = 0;

    @Column(nullable = false)
    private Integer wins = 0;

    @Column(nullable = false)
    private Integer losses = 0;

    @Column(nullable = false)
    private Integer rating = 1000;

    @Column(columnDefinition = "TEXT")
    private String pickedHeroCounts;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (totalMatches == null) totalMatches = 0;
        if (wins == null) wins = 0;
        if (losses == null) losses = 0;
        if (rating == null) rating = 1000;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (totalMatches == null) totalMatches = 0;
        if (wins == null) wins = 0;
        if (losses == null) losses = 0;
        if (rating == null) rating = 1000;
    }
}
