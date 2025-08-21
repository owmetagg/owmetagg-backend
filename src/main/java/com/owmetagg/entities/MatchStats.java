package com.owmetagg.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Match Identification (inspired by their battle structure)
    @JsonProperty("match_id")
    @Column(name = "match_id")
    private String matchId; // External match ID from OverFast API

    @JsonProperty("player_id")
    @Column(name = "player_id")
    private String playerId; // Battle.net player ID

    @JsonProperty("player_name")
    @Column(name = "player_name")
    private String playerName;

    // Hero and Game Info
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hero_id", nullable = false)
    private Hero hero;

    @JsonProperty("player_rank")
    @Column(nullable = false)
    private String playerRank; // BRONZE, SILVER, GOLD, PLATINUM, DIAMOND, MASTER, GRANDMASTER, CHAMPION

    @JsonProperty("player_sr")
    @Column(name = "skill_rating")
    private Integer skillRating; // Actual SR number (like their rating system)

    @JsonProperty("sr_change")
    @Column(name = "sr_change")
    private Integer srChange; // SR gained/lost (like their rating_change)

    @Column(nullable = false)
    private String region; // US, EU, ASIA

    @Column(nullable = false)
    private String gameMode; // COMPETITIVE, QUICKPLAY, ARCADE

    @JsonProperty("map_name")
    @Column(name = "map_name")
    private String mapName; // King's Row, Hanamura, etc.

    @JsonProperty("map_type")
    @Column(name = "map_type")
    private String mapType; // ESCORT, ASSAULT, HYBRID, CONTROL

    // Match Outcome
    @JsonProperty("won")
    @Column(nullable = false)
    private Boolean won; // true if won, false if lost

    @JsonProperty("team_score")
    @Column(name = "team_score")
    private String teamScore; // "3-1", "2-0", etc. (like their rounds)

    // Performance Statistics
    @JsonProperty("eliminations")
    @Column
    private Integer eliminations;

    @JsonProperty("deaths")
    @Column
    private Integer deaths;

    @JsonProperty("assists")
    @Column
    private Integer assists;

    @JsonProperty("damage_dealt")
    @Column(name = "damage")
    private Integer damage;

    @JsonProperty("healing_done")
    @Column(name = "healing")
    private Integer healing;

    @JsonProperty("objective_time")
    @Column(name = "objective_time_seconds")
    private Integer objectiveTimeSeconds;

    @JsonProperty("final_blows")
    @Column(name = "final_blows")
    private Integer finalBlows;

    // Time and Duration
    @JsonProperty("match_duration")
    @Column(name = "match_duration_seconds")
    private Integer matchDurationSeconds;

    @JsonProperty("time_played")
    @Column(name = "time_played_seconds")
    private Integer timePlayedSeconds; // Time on this specific hero

    @JsonProperty("match_date")
    @Column(nullable = false)
    private LocalDateTime matchDate;

    @JsonProperty("competitive_season")
    @Column(name = "competitive_season")
    private Integer competitiveSeason; // Overwatch season number

    @CreationTimestamp
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    // Convenience constructor for quick creation
    public MatchStats(Hero hero, String playerRank, String region, String gameMode, Boolean won, LocalDateTime matchDate) {
        this.hero = hero;
        this.playerRank = playerRank;
        this.region = region;
        this.gameMode = gameMode;
        this.won = won;
        this.matchDate = matchDate;
    }

    // Calculated properties (like their Battle entity structure)
    public Double getKdRatio() {
        if (deaths == null || deaths == 0) return null;
        return eliminations != null ? (double) eliminations / deaths : null;
    }

    public Double getKdaRatio() {
        if (deaths == null || deaths == 0) return null;
        return (eliminations != null && assists != null) ?
                (double) (eliminations + assists) / deaths : null;
    }

    public Integer getNetSrChange() {
        return won != null && won ?
                (srChange != null ? Math.abs(srChange) : null) :
                (srChange != null ? -Math.abs(srChange) : null);
    }
}