package com.owmetagg.dtos;

import lombok.Data;
import java.time.LocalDate;

@Data
public class HeroTrendDTO {
    private String heroKey;
    private LocalDate trendDate;
    private String gameMode;
    private Double pickRate;
    private Double winRate;
    private Long gamesPlayed;
    private Double avgKda;
}