package com.owmetagg.controllers;

import com.owmetagg.dtos.StatsResponse;
import com.owmetagg.services.DataProcessingService;
import com.owmetagg.services.OverFastApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/data")
@CrossOrigin(origins = "http://localhost:3000")
@Slf4j
public class DataProcessingController {

    @Autowired
    private DataProcessingService dataProcessingService;

    @Autowired
    private OverFastApiService overFastApiService;

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
     * Manually trigger daily hero sync
     */
    @PostMapping("/sync/daily")
    public ResponseEntity<StatsResponse<String>> triggerDailySync() {
        log.info("Manually triggering daily sync");

        try {
            dataProcessingService.dailyHeroSync();
            return ResponseEntity.ok(StatsResponse.success("Daily sync triggered successfully"));
        } catch (Exception e) {
            log.error("Failed to trigger daily sync", e);
            return ResponseEntity.internalServerError()
                    .body(StatsResponse.error("Failed to trigger daily sync: " + e.getMessage()));
        }
    }

    /**
     * Health check for data processing
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Data processing service is healthy!");
    }
}