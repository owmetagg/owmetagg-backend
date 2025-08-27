package com.owmetagg.dtos;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RoleStatisticsDTO {
    private String role;
    private String gameMode;
    private Double avgWinRate;
    private Double avgPickRate;
    private Double avgKda;
    private Integer totalPlayers;
    private LocalDateTime lastCalculated;
}