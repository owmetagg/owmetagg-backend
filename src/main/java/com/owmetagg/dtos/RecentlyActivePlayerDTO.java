package com.owmetagg.dtos;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class RecentlyActivePlayerDTO {
    private String playerId;
    private String battletag;
    private String platform;
    private String region;
    private Integer skillRating;
    private String username;
    private String avatarUrl;
    private LocalDateTime lastUpdated;
    private Integer recentGamesPlayed;
}