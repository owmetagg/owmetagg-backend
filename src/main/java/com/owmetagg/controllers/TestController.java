package com.owmetagg.controllers;

import com.owmetagg.services.OverFastService;
import com.owmetagg.repositories.PlayerRepository;
import com.owmetagg.repositories.HeroStatsRepository;
import com.owmetagg.models.Player;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test")
public class
TestController {

    private final OverFastService overFastService;
    private final PlayerRepository playerRepository;
    private final HeroStatsRepository heroStatsRepository;

    public TestController(OverFastService overFastService,
                          PlayerRepository playerRepository,
                          HeroStatsRepository heroStatsRepository) {
        this.overFastService = overFastService;
        this.playerRepository = playerRepository;
        this.heroStatsRepository = heroStatsRepository;
    }

    @PostMapping("/fetch-player")
    public ResponseEntity<Map<String, String>> testFetchPlayer(
            @RequestParam String battletag,
            @RequestParam(defaultValue = "pc") String platform) {

        overFastService.fetchAndSendPlayerData(battletag, platform);

        return ResponseEntity.ok(Map.of(
                "message", "Sent " + battletag + " to processing queue!",
                "battletag", battletag,
                "platform", platform,
                "status", "queued"
        ));
    }

    @GetMapping("/quick-test")
    public ResponseEntity<Map<String, Object>> quickTest() {
        // These are known public Overwatch players that should have data
        List<String> testPlayers = Arrays.asList(
                "pge-11208",
                 "Infekted-11628",
                 "Samito-11602"
                 // trungs friend + white winston + rage guy
        );

        log.info("Starting quick test with {} known players", testPlayers.size());
        overFastService.fetchAndSendMultiplePlayers(testPlayers);

        return ResponseEntity.ok(Map.of(
                "message", "Started processing test players",
                "players", testPlayers,
                "count", testPlayers.size(),
                "status", "processing"
        ));
    }

    // ADD: Batch processing endpoint
    @PostMapping("/fetch-batch")
    public ResponseEntity<Map<String, Object>> fetchBatch(@RequestBody List<String> battletags) {
        log.info("Processing batch of {} players", battletags.size());
        overFastService.fetchAndSendMultiplePlayers(battletags);

        return ResponseEntity.ok(Map.of(
                "message", "Batch sent to processing queue",
                "count", battletags.size(),
                "status", "queued"
        ));
    }

    @GetMapping("/database-stats")
    public ResponseEntity<Map<String, Object>> getDatabaseStats() {
        long playerCount = playerRepository.count();
        long heroStatsCount = heroStatsRepository.count();

        return ResponseEntity.ok(Map.of(
                "totalPlayers", playerCount,
                "totalHeroStats", heroStatsCount,
                "message", playerCount > 0 ? "Database has data!" : "No data in database yet"
        ));
    }

    @GetMapping("/recent-players")
    public ResponseEntity<List<Player>> getRecentPlayers() {
        // Using findAll with pagination to get recent players
        List<Player> players = playerRepository.findAll();
        log.info("Found {} players in database", players.size());
        return ResponseEntity.ok(players);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean overfastHealthy = overFastService.checkOverFastAPIHealth();

        return ResponseEntity.ok(Map.of(
                "message", "🚀 Overwatch Stats Backend is running!",
                "overfastAPI", overfastHealthy ? "healthy" : "unhealthy",
                "databaseConnected", true,
                "timestamp", System.currentTimeMillis()
        ));
    }
}