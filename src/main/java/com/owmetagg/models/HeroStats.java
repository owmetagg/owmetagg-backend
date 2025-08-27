package com.owmetagg.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "hero_stats")
@Data
public class HeroStats {

    @EmbeddedId
    private HeroStatsId id;

    @Column(name = "last_played")
    private LocalDateTime lastPlayed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", insertable = false, updatable = false)
    private Player player;

    // Core statistics
    @Column(name = "wins")
    private int wins;

    @Column(name = "losses")
    private int losses;

    @Column(name = "draws")
    private int draws;

    @Column(name = "time_played") // in minutes
    private int timePlayed;

    // Performance metrics
    @Column(name = "eliminations")
    private long eliminations;

    @Column(name = "deaths")
    private long deaths;

    @Column(name = "assists")
    private long assists;

    @Column(name = "damage_dealt")
    private long damageDealt;

    @Column(name = "healing_done")
    private long healingDone;

    @Column(name = "skill_tier") // Like your danRank
    private int skillTier;

    // Transient fields for batch processing (like your pattern)
    @Transient
    private int winsIncrement = 0;

    @Transient
    private int lossesIncrement = 0;

    @Transient
    private int drawsIncrement = 0;

    @Transient
    private int timePlayedIncrement = 0;

    @Transient
    private long eliminationsIncrement = 0;

    @Transient
    private long deathsIncrement = 0;

    @Transient
    private long assistsIncrement = 0;

    @Transient
    private long damageDealtIncrement = 0;

    @Transient
    private long healingDoneIncrement = 0;

    public HeroStats() {
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.timePlayed = 0;
        this.eliminations = 0;
        this.deaths = 0;
        this.assists = 0;
        this.damageDealt = 0;
        this.healingDone = 0;
        this.skillTier = 0;
        this.lastPlayed = LocalDateTime.now();
    }

    // Business logic: Calculate KDA
    public double getKDA() {
        if (deaths == 0) return eliminations + assists; // Avoid division by zero
        return (double) (eliminations + assists) / deaths;
    }

    // Business logic: Calculate win rate
    public double getWinRate() {
        int totalGames = wins + losses + draws;
        if (totalGames == 0) return 0.0;
        return (double) wins / totalGames * 100.0;
    }

    // Business logic: Get damage per 10 minutes
    public double getDamagePer10Min() {
        if (timePlayed == 0) return 0.0;
        return (double) damageDealt / timePlayed * 10.0;
    }

    // Business logic: Get healing per 10 minutes
    public double getHealingPer10Min() {
        if (timePlayed == 0) return 0.0;
        return (double) healingDone / timePlayed * 10.0;
    }
}