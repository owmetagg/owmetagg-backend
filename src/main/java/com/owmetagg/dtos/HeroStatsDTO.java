package com.owmetagg.dtos;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HeroStatsDTO {
    private String heroKey;
    private String gameMode;
    private Long totalGamesPlayed;
    private Long totalWins;
    private Long totalLosses;
    private Integer pickCount;
    private Double pickRate;
    private Double winRate;
    private Double avgEliminations;
    private Double avgDeaths;
    private Double avgAssists;
    private Double avgKda;
    private LocalDateTime lastCalculated;
}