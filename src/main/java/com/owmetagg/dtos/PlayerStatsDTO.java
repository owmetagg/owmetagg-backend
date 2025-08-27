package com.owmetagg.dtos;

import lombok.Data;

@Data
public class PlayerStatsDTO {
    private String gameMode;
    private Integer totalGames;
    private Integer wins;
    private Integer losses;
    private Integer draws;
    private Double winRate;
    private Long totalEliminations;
    private Long totalDeaths;
    private Long totalAssists;
    private Double avgEliminations;
    private Double avgDeaths;
    private Double avgAssists;
    private Double kda;
    private Long totalDamageDealt;
    private Long totalHealingDone;
    private Integer totalTimePlayed; // in minutes
}