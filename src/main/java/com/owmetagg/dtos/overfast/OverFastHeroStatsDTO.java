package com.owmetagg.dtos.overfast;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OverFastHeroStatsDTO {
    @JsonProperty("hero")
    private String hero;

    @JsonProperty("eliminations")
    private Integer eliminations;

    @JsonProperty("assists")
    private Integer assists;

    @JsonProperty("deaths")
    private Integer deaths;

    @JsonProperty("damage_dealt")
    private Integer damageDone;

    @JsonProperty("healing_done")
    private Integer healingDone;

    @JsonProperty("time_played")
    private String timePlayed; // Usually in format "01:23:45"

    @JsonProperty("games_won")
    private Integer gamesWon;

    @JsonProperty("games_played")
    private Integer gamesPlayed;

    // Calculate win rate
    public Double getWinRate() {
        if (gamesPlayed == null || gamesPlayed == 0) return null;
        if (gamesWon == null) return 0.0;
        return (gamesWon.doubleValue() / gamesPlayed.doubleValue()) * 100.0;
    }

    // Parse time played to seconds
    public Integer getTimePlayedSeconds() {
        if (timePlayed == null || timePlayed.isEmpty()) return null;

        try {
            String[] parts = timePlayed.split(":");
            if (parts.length == 3) {
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                int seconds = Integer.parseInt(parts[2]);
                return hours * 3600 + minutes * 60 + seconds;
            }
        } catch (NumberFormatException e) {
            // Ignore parsing errors
        }
        return null;
    }
}