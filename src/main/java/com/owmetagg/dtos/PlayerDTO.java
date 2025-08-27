package com.owmetagg.dtos;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlayerDTO {
    private String battletag;
    private String username;
    private String avatarUrl;
    private String title;  // "Commander of Hope"
    private Integer endorsementLevel;
    private String currentRank;  // "Grandmaster 1"
    private Integer skillRating;
    private LocalDateTime lastUpdated;
    private Boolean isPrivate;
    private String platform;
    private Long totalPlayTime;
    private Long latestSession;
    
    // For RabbitMQ processing
    private String rawPlayerData;

    // Nested complex data
    private Map<String, HeroSummaryDTO> playedHeroes;  // All heroes with stats
    private List<SessionDTO> recentSessions;  // Recent play sessions
    private PlayerStatsDTO careerStats;  // Aggregated career stats
}