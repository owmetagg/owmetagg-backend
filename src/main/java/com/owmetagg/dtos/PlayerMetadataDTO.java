package com.owmetagg.dtos;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class PlayerMetadataDTO {
    private String battletag;
    private String username;
    private String avatarUrl;
    private String platform;
    private Integer skillRating;
    private String currentRank;  // "Grandmaster 1"
    private String lastUpdatedDate;  // String for display "2 hours ago"
    private LocalDateTime lastUpdated; // Actual timestamp
    private Map<String, String> mainHeroAndStats;  // {"hero": "Genji", "time": "42 hours"}
}