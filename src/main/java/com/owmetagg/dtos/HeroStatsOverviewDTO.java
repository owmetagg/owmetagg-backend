package com.owmetagg.dtos;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HeroStatsOverviewDTO {
    private String heroName;
    private String heroRole;
    private String rank;
    private String region;
    private Long totalMatches;
    private Double winRate;
    private Double pickRate;
    private HeroPerformanceDTO performance;
}