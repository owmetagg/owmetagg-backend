package com.owmetagg.dtos;

import lombok.Data;

@Data
public class HeroSummaryDTO {
    private String heroKey;
    private String heroName;
    private Integer timePlayed; // in minutes
    private Integer gamesPlayed;
    private Integer wins;
    private Integer losses;
    private Double winRate;
    private Double kda;
}