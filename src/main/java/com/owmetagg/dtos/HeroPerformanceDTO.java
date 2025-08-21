package com.owmetagg.dtos;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HeroPerformanceDTO {
    private String rank;
    private Double avgEliminations;
    private Double avgDeaths;
    private Double avgAssists;
    private Double avgDamage;
    private Double avgHealing;

    // Calculated fields
    public Double getKdRatio() {
        if (avgDeaths == null || avgDeaths == 0) return null;
        return avgEliminations / avgDeaths;
    }

    public Double getKdaRatio() {
        if (avgDeaths == null || avgDeaths == 0) return null;
        return (avgEliminations + avgAssists) / avgDeaths;
    }
}