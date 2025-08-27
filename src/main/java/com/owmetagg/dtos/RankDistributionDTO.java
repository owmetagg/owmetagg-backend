package com.owmetagg.dtos;

import lombok.Data;
import java.time.LocalDate;

@Data
public class RankDistributionDTO {
    private Integer srBracket;
    private String bracketName;
    private Integer playerCount;
    private Double percentage;
    private LocalDate snapshotDate;
}