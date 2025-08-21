package com.owmetagg.controllers;

import com.owmetagg.dtos.StatsResponse;
import com.owmetagg.dtos.overfast.*;
import com.owmetagg.services.OverFastApiService;
import com.owmetagg.services.DataProcessingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/v1/overfast")
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class OverFastController {

    @Autowired
    private OverFastApiService overFastApiService;

    @Autowired
    private DataProcessingService dataProcessingService;

    /**
     * Test OverFast API connection
     */
    @GetMapping("/test")
    public Mono<ResponseEntity<StatsResponse<String>>> testConnection() {
        log.info("Testing OverFast API connection");

        return overFastApiService.testConnection()
                .map(result -> ResponseEntity.ok(StatsResponse.success(result)))
                .onErrorResume(error -> {
                    log.error("OverFast API test failed", error);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(StatsResponse.error("Connection test failed: " + error.getMessage())));
                });
    }

    /**
     * Get OverFast API status
     */
    @GetMapping("/status")
    public Mono<ResponseEntity<StatsResponse<OverFastApiStatusDTO>>> getApiStatus() {
        return overFastApiService.getApiStatus()
                .map(status -> ResponseEntity.ok(StatsResponse.success(status)))
                .onErrorResume(error -> {
                    log.error("Failed to get OverFast API status", error);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(StatsResponse.error("Failed to get API status")));
                });
    }

    /**
     * Fetch all heroes from OverFast API
     */
    @GetMapping("/heroes")
    public Mono<ResponseEntity<StatsResponse<List<OverFastHeroDTO>>>> fetchHeroes() {
        log.info("Fetching heroes from OverFast API via controller");

        return overFastApiService.fetchAllHeroes()
                .map(heroes -> ResponseEntity.ok(StatsResponse.success(heroes)))
                .onErrorResume(error -> {
                    log.error("Failed to fetch heroes", error);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(StatsResponse.error("Failed to fetch heroes: " + error.getMessage())));
                });
    }

    /**
     * Fetch specific hero details
     */
    @GetMapping("/heroes/{heroKey}")
    public Mono<ResponseEntity<StatsResponse<OverFastHeroDetailDTO>>> getHeroDetails(
            @PathVariable String heroKey) {

        log.info("Fetching hero details for: {}", heroKey);

        return overFastApiService.fetchHeroDetails(heroKey)
                .map(hero -> ResponseEntity.ok(StatsResponse.success(hero)))
                .onErrorResume(error -> {
                    log.error("Failed to fetch hero details for: {}", heroKey, error);
                    return Mono.just(ResponseEntity.notFound().build());
                });
    }

    /**
     * Fetch player statistics
     */
    @GetMapping("/players/{platform}/{battleTag}")
    public Mono<ResponseEntity<StatsResponse<OverFastPlayerStatsDTO>>> getPlayerStats(
            @PathVariable String platform,
            @PathVariable String battleTag) {

        log.info("Fetching player stats for: {} on {}", battleTag, platform);

        return overFastApiService.fetchPlayerStats(battleTag, platform)
                .map(stats -> ResponseEntity.ok(StatsResponse.success(stats)))
                .onErrorResume(error -> {
                    log.error("Failed to fetch player stats for: {}", battleTag, error);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(StatsResponse.error("Failed to fetch player stats: " + error.getMessage())));
                });
    }

    /**
     * Sync heroes from OverFast API to local database
     */
    @PostMapping("/sync/heroes")
    public Mono<ResponseEntity<StatsResponse<String>>> syncHeroes() {
        log.info("Starting hero synchronization from OverFast API");

        return overFastApiService.syncHeroesFromOverFast()
                .map(result -> ResponseEntity.ok(StatsResponse.success(result)))
                .onErrorResume(error -> {
                    log.error("Hero synchronization failed", error);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(StatsResponse.error("Synchronization failed: " + error.getMessage())));
                });
    }

    /**
     * Process a specific player's stats into match data
     */
    @PostMapping("/process/player/{platform}/{battleTag}")
    public ResponseEntity<StatsResponse<String>> processPlayer(
            @PathVariable String platform,
            @PathVariable String battleTag) {

        log.info("Processing player data for: {} on {}", battleTag, platform);

        try {
            String result = dataProcessingService.processSpecificPlayer(battleTag, platform);
            return ResponseEntity.ok(StatsResponse.success(result));
        } catch (Exception e) {
            log.error("Failed to process player: {}", battleTag, e);
            return ResponseEntity.internalServerError()
                    .body(StatsResponse.error("Failed to process player: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OverFast API integration is healthy!");
    }
}