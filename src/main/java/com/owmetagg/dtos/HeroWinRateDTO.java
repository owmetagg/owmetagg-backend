package com.owmetagg.dtos;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

// Hero Win Rate Response
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HeroWinRateDTO {
    private String heroName;
    private String heroRole;
    private String rank;
    private String region;
    private Long totalMatches;
    private Long wins;
    private Double winRate;
}