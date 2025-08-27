package com.owmetagg.dtos;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class PlayerSearchResultDTO {
    private String playerId;
    private String name;           // For backwards compatibility
    private String battletag;      // Display name in search results
    private String username;       // Actual username (might differ from battletag)
    private String avatarUrl;      // Show avatar in search dropdown
    private String platform;
    private Integer skillRating;   // Show rank in search results
    private String rank;          // "Grandmaster 1" - formatted rank
}