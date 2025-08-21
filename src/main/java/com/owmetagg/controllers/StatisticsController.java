package com.owmetagg.controllers;

import com.owmetagg.dtos.*;
import com.owmetagg.services.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/hero-winrates")
    public ResponseEntity<StatsResponse<List<HeroWinRateDTO>>> getHeroWinRates(
            @RequestParam(required = false) String heroName,
            @RequestParam(required = false) String rank,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String gameMode,
            @RequestParam(required = false, defaultValue = "30") Integer days) {

        try {
            List<HeroWinRateDTO> winRates = statisticsService.getHeroWinRates(heroName, rank, region, gameMode, days);
            StatsMetadata metadata = statisticsService.createMetadata(days);

            return ResponseEntity.ok(StatsResponse.success(winRates, metadata));
        } catch (Exception e) {
            log.error("Error fetching hero win rates", e);
            return ResponseEntity.internalServerError()
                    .body(StatsResponse.error("Failed to fetch hero win rates"));
        }
    }

    @GetMapping("/hero-pickrates")
    public ResponseEntity<StatsResponse<List<HeroPickRateDTO>>> getHeroPickRates(
            @RequestParam(required = false) String rank,
            @RequestParam(required = false) String region,
            @RequestParam(required = false, defaultValue = "30") Integer days) {

        try {
            List<HeroPickRateDTO> pickRates = statisticsService.getHeroPickRates(rank, region, days);
            StatsMetadata metadata = statisticsService.createMetadata(days);

            return ResponseEntity.ok(StatsResponse.success(pickRates, metadata));
        } catch (Exception e) {
            log.error("Error fetching hero pick rates", e);
            return ResponseEntity.internalServerError()
                    .body(StatsResponse.error("Failed to fetch hero pick rates"));
        }
    }

    @GetMapping("/rank-distribution")
    public ResponseEntity<StatsResponse<List<RankDistributionDTO>>> getRankDistribution(
            @RequestParam(required = false) String region,
            @RequestParam(required = false, defaultValue = "30") Integer days) {

        try {
            List<RankDistributionDTO> distribution = statisticsService.getRankDistribution(region, days);
            StatsMetadata metadata = statisticsService.createMetadata(days);

            return ResponseEntity.ok(StatsResponse.success(distribution, metadata));
        } catch (Exception e) {
            log.error("Error fetching rank distribution", e);
            return ResponseEntity.internalServerError()
                    .body(StatsResponse.error("Failed to fetch rank distribution"));
        }
    }

    @GetMapping("/hero-performance/{heroName}")
    public ResponseEntity<StatsResponse<List<HeroPerformanceDTO>>> getHeroPerformance(
            @PathVariable String heroName,
            @RequestParam(required = false) String rank,
            @RequestParam(required = false, defaultValue = "30") Integer days) {

        try {
            List<HeroPerformanceDTO> performance = statisticsService.getHeroPerformance(heroName, rank, days);
            StatsMetadata metadata = statisticsService.createMetadata(days);

            return ResponseEntity.ok(StatsResponse.success(performance, metadata));
        } catch (Exception e) {
            log.error("Error fetching hero performance for {}", heroName, e);
            return ResponseEntity.internalServerError()
                    .body(StatsResponse.error("Failed to fetch hero performance"));
        }
    }

    @GetMapping("/meta-trends")
    public ResponseEntity<StatsResponse<List<MetaTrendDTO>>> getMetaTrends(
            @RequestParam(required = false, defaultValue = "DIAMOND") String rank,
            @RequestParam(required = false, defaultValue = "7") Integer days) {

        try {
            List<MetaTrendDTO> trends = statisticsService.getMetaTrends(rank, days);
            StatsMetadata metadata = statisticsService.createMetadata(days);

            return ResponseEntity.ok(StatsResponse.success(trends, metadata));
        } catch (Exception e) {
            log.error("Error fetching meta trends", e);
            return ResponseEntity.internalServerError()
                    .body(StatsResponse.error("Failed to fetch meta trends"));
        }
    }

    @GetMapping("/hero-overview/{heroName}")
    public ResponseEntity<StatsResponse<HeroStatsOverviewDTO>> getHeroOverview(
            @PathVariable String heroName,
            @RequestParam(required = false) String rank,
            @RequestParam(required = false) String region,
            @RequestParam(required = false, defaultValue = "30") Integer days) {

        try {
            HeroStatsOverviewDTO overview = statisticsService.getHeroOverview(heroName, rank, region, days);

            if (overview == null) {
                return ResponseEntity.notFound().build();
            }

            StatsMetadata metadata = statisticsService.createMetadata(days);
            return ResponseEntity.ok(StatsResponse.success(overview, metadata));
        } catch (Exception e) {
            log.error("Error fetching hero overview for {}", heroName, e);
            return ResponseEntity.internalServerError()
                    .body(StatsResponse.error("Failed to fetch hero overview"));
        }
    }

    // Quick stats endpoints
    @GetMapping("/top-heroes")
    public ResponseEntity<StatsResponse<List<HeroWinRateDTO>>> getTopHeroes(
            @RequestParam(required = false) String rank,
            @RequestParam(required = false) String region) {

        try {
            List<HeroWinRateDTO> topHeroes = statisticsService.getHeroWinRates(null, rank, region, "COMPETITIVE", 7)
                    .stream()
                    .limit(10)
                    .toList();

            return ResponseEntity.ok(StatsResponse.success(topHeroes));
        } catch (Exception e) {
            log.error("Error fetching top heroes", e);
            return ResponseEntity.internalServerError()
                    .body(StatsResponse.error("Failed to fetch top heroes"));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OWMetaGG Statistics API is healthy!");
    }
}