package com.owmetagg.dtos;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

// Hero Pick Rate Response
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HeroPickRateDTO {
    private String heroName;
    private String heroRole;
    private String rank;
    private Long timesPlayed;
    private Double pickRate;
}