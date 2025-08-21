package com.owmetagg.dtos;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MetaTrendDTO {
    private String heroName;
    private String heroRole;
    private LocalDate matchDay;
    private Long dailyPicks;
    private Double dailyWinRate;
}