package com.owmetagg.controllers;

import com.owmetagg.dtos.HeroStatsDTO;
import com.owmetagg.dtos.RankDistributionDTO;
import com.owmetagg.dtos.HeroTrendDTO;
import com.owmetagg.dtos.RoleStatisticsDTO;
import com.owmetagg.services.StatisticsService;
import com.owmetagg.services.StatisticsCalculationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/statistics")
@CrossOrigin(origins = "*")
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final StatisticsCalculationService calculationService;
    
    public StatisticsController(StatisticsService statisticsService, 
                               StatisticsCalculationService calculationService) {
        this.statisticsService = statisticsService;
        this.calculationService = calculationService;
    }
    
    @GetMapping("/heroes/top")
    public ResponseEntity<List<HeroStatsDTO>> getTopHeroes(
            @RequestParam(defaultValue = "competitive") String gameMode,
            @RequestParam(defaultValue = "pickrate") String sortBy,
            @RequestParam(defaultValue = "10") int limit) {
        
        log.info("📊 GET /api/statistics/heroes/top - gameMode: {}, sortBy: {}, limit: {}", 
                gameMode, sortBy, limit);
        
        List<HeroStatsDTO> heroes = statisticsService.getTopHeroes(gameMode, sortBy, limit);
        return ResponseEntity.ok(heroes);
    }
    
    @GetMapping("/heroes/winrates")
    public ResponseEntity<List<HeroStatsDTO>> getHeroWinRates(
            @RequestParam(defaultValue = "competitive") String gameMode,
            @RequestParam(defaultValue = "10") int minGames) {
        
        log.info("📊 GET /api/statistics/heroes/winrates - gameMode: {}, minGames: {}", 
                gameMode, minGames);
        
        List<HeroStatsDTO> heroes = statisticsService.getHeroWinRates(gameMode, minGames);
        return ResponseEntity.ok(heroes);
    }
    
    @GetMapping("/heroes/pickrates")
    public ResponseEntity<List<HeroStatsDTO>> getHeroPickRates(
            @RequestParam(defaultValue = "competitive") String gameMode) {
        
        log.info("📊 GET /api/statistics/heroes/pickrates - gameMode: {}", gameMode);
        
        List<HeroStatsDTO> heroes = statisticsService.getHeroPickRates(gameMode);
        return ResponseEntity.ok(heroes);
    }
    
    @GetMapping("/rank-distribution")
    public ResponseEntity<List<RankDistributionDTO>> getRankDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        if (date == null) {
            date = LocalDate.now();
        }
        
        log.info("🏆 GET /api/statistics/rank-distribution - date: {}", date);
        
        List<RankDistributionDTO> distribution = statisticsService.getRankDistribution(date);
        return ResponseEntity.ok(distribution);
    }
    
    @GetMapping("/trends/hero/{heroKey}")
    public ResponseEntity<List<HeroTrendDTO>> getHeroTrends(
            @PathVariable String heroKey,
            @RequestParam(defaultValue = "competitive") String gameMode,
            @RequestParam(defaultValue = "7") int days) {
        
        log.info("📈 GET /api/statistics/trends/hero/{} - gameMode: {}, days: {}", 
                heroKey, gameMode, days);
        
        List<HeroTrendDTO> trends = statisticsService.getHeroTrends(heroKey, gameMode, days);
        return ResponseEntity.ok(trends);
    }
    
    @GetMapping("/trends/all")
    public ResponseEntity<List<HeroTrendDTO>> getAllHeroTrends(
            @RequestParam(defaultValue = "competitive") String gameMode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        if (date == null) {
            date = LocalDate.now();
        }
        
        log.info("📈 GET /api/statistics/trends/all - gameMode: {}, date: {}", gameMode, date);
        
        List<HeroTrendDTO> trends = statisticsService.getAllHeroTrends(gameMode, date);
        return ResponseEntity.ok(trends);
    }
    
    @GetMapping("/roles")
    public ResponseEntity<List<RoleStatisticsDTO>> getRoleStatistics(
            @RequestParam(defaultValue = "competitive") String gameMode) {
        
        log.info("🛡️ GET /api/statistics/roles - gameMode: {}", gameMode);
        
        List<RoleStatisticsDTO> roleStats = statisticsService.getRoleStatistics(gameMode);
        return ResponseEntity.ok(roleStats);
    }
    
    @GetMapping("/meta-report")
    public ResponseEntity<Map<String, Object>> getMetaReport(
            @RequestParam(defaultValue = "competitive") String gameMode,
            @RequestParam(required = false) Integer srBracket) {
        
        log.info("📋 GET /api/statistics/meta-report - gameMode: {}, srBracket: {}", 
                gameMode, srBracket);
        
        Map<String, Object> report = statisticsService.getMetaReport(gameMode, srBracket);
        return ResponseEntity.ok(report);
    }
    
    @PostMapping("/recalculate")
    public ResponseEntity<Map<String, String>> triggerRecalculation() {
        log.info("🔄 POST /api/statistics/recalculate - Manual statistics recalculation triggered");
        
        try {
            // Trigger all calculations
            calculationService.calculateHeroStatistics();
            calculationService.calculateRankDistribution();
            calculationService.calculateHeroTrends();
            calculationService.calculateRoleStatistics();
            
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Statistics recalculation completed successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to recalculate statistics", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Failed to recalculate statistics: " + e.getMessage()
            ));
        }
    }
}