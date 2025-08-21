package com.owmetagg.services;

import com.owmetagg.dtos.*;
import com.owmetagg.repositories.MatchStatsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
public class StatisticsService {

    @Autowired
    private MatchStatsRepository matchStatsRepository;

    private static final List<String> RANKS = Arrays.asList(
            "BRONZE", "SILVER", "GOLD", "PLATINUM", "DIAMOND", "MASTER", "GRANDMASTER", "CHAMPION"
    );

    private static final List<String> REGIONS = Arrays.asList("US", "EU", "ASIA");

    public List<HeroWinRateDTO> getHeroWinRates(String heroName, String rank, String region, String gameMode, Integer days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days != null ? days : 30);
        Long minMatches = 10L; // Minimum matches for statistical significance

        List<Object[]> results = matchStatsRepository.findHeroWinRates(
                heroName, rank, region, gameMode, startDate, minMatches
        );

        return results.stream()
                .map(row -> new HeroWinRateDTO(
                        (String) row[0],  // heroName
                        (String) row[1],  // heroRole
                        (String) row[2],  // rank
                        (String) row[3],  // region
                        (Long) row[4],    // totalMatches
                        (Long) row[5],    // wins
                        (Double) row[6]   // winRate
                ))
                .collect(Collectors.toList());
    }

    public List<HeroPickRateDTO> getHeroPickRates(String rank, String region, Integer days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days != null ? days : 30);

        List<Object[]> results = matchStatsRepository.findHeroPickRates(rank, region, startDate);

        return results.stream()
                .map(row -> new HeroPickRateDTO(
                        (String) row[0],  // heroName
                        (String) row[1],  // heroRole
                        (String) row[2],  // rank
                        (Long) row[3],    // timesPlayed
                        (Double) row[4]   // pickRate
                ))
                .collect(Collectors.toList());
    }

    public List<RankDistributionDTO> getRankDistribution(String region, Integer days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days != null ? days : 30);

        List<Object[]> results = matchStatsRepository.findRankDistribution(region, startDate);

        return results.stream()
                .map(row -> new RankDistributionDTO(
                        (String) row[0],  // rank
                        (Long) row[1],    // totalMatches
                        (Long) row[2],    // uniqueHeroes
                        (Double) row[3]   // avgWinRate
                ))
                .collect(Collectors.toList());
    }

    public List<HeroPerformanceDTO> getHeroPerformance(String heroName, String rank, Integer days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days != null ? days : 30);

        List<Object[]> results = matchStatsRepository.findHeroPerformanceByRank(heroName, rank, startDate);

        return results.stream()
                .map(row -> new HeroPerformanceDTO(
                        (String) row[0],  // rank
                        (Double) row[1],  // avgEliminations
                        (Double) row[2],  // avgDeaths
                        (Double) row[3],  // avgAssists
                        (Double) row[4],  // avgDamage
                        (Double) row[5]   // avgHealing
                ))
                .collect(Collectors.toList());
    }

    public List<MetaTrendDTO> getMetaTrends(String rank, Integer days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days != null ? days : 7);
        Long minDailyPicks = 5L;

        List<Object[]> results = matchStatsRepository.findMetaTrends(rank, startDate, minDailyPicks);

        return results.stream()
                .map(row -> new MetaTrendDTO(
                        (String) row[0],     // heroName
                        (String) row[1],     // heroRole
                        ((java.sql.Date) row[2]).toLocalDate(),  // matchDay
                        (Long) row[3],       // dailyPicks
                        (Double) row[4]      // dailyWinRate
                ))
                .collect(Collectors.toList());
    }

    public HeroStatsOverviewDTO getHeroOverview(String heroName, String rank, String region, Integer days) {
        // Get win rate data
        List<HeroWinRateDTO> winRates = getHeroWinRates(heroName, rank, region, null, days);

        // Get pick rate data
        List<HeroPickRateDTO> pickRates = getHeroPickRates(rank, region, days);

        // Get performance data
        List<HeroPerformanceDTO> performance = getHeroPerformance(heroName, rank, days);

        if (winRates.isEmpty()) {
            return null;
        }

        HeroWinRateDTO winRate = winRates.get(0);
        HeroPickRateDTO pickRate = pickRates.stream()
                .filter(pr -> pr.getHeroName().equals(heroName))
                .findFirst()
                .orElse(new HeroPickRateDTO());

        HeroPerformanceDTO perf = performance.isEmpty() ? new HeroPerformanceDTO() : performance.get(0);

        return new HeroStatsOverviewDTO(
                winRate.getHeroName(),
                winRate.getHeroRole(),
                winRate.getRank(),
                winRate.getRegion(),
                winRate.getTotalMatches(),
                winRate.getWinRate(),
                pickRate.getPickRate(),
                perf
        );
    }

    public StatsMetadata createMetadata(Integer days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days != null ? days : 30);
        Long totalRecords = matchStatsRepository.count();

        return new StatsMetadata(
                totalRecords,
                "Last " + (days != null ? days : 30) + " days",
                LocalDateTime.now().toString(),
                RANKS,
                REGIONS
        );
    }
}