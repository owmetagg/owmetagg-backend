package com.owmetagg.dtos;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatsMetadata {
    private Long totalRecords;
    private String timeRange;
    private String lastUpdated;
    private List<String> availableRanks;
    private List<String> availableRegions;
}