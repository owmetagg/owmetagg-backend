package com.owmetagg.dtos;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankDistributionDTO {
    private String rank;
    private Long totalMatches;
    private Long uniqueHeroes;
    private Double avgWinRate;
}