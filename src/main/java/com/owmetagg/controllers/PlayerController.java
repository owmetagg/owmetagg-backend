package com.owmetagg.controllers;

import com.owmetagg.dtos.*;
import com.owmetagg.services.PlayerService;
import com.owmetagg.services.OverFastService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/player")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PlayerController {

    private final PlayerService playerService;
    private final OverFastService OverFastService;

    /**
     * Get player profile by battletag
     * GET /api/player/pge-11208?platform=pc
     */
    @GetMapping("/{battletag}")
    public ResponseEntity<PlayerDTO> getPlayerStats(
            @PathVariable String battletag,
            @RequestParam(defaultValue = "pc") String platform,
            HttpServletRequest request) {

        log.info("Received request for Player: {} from IP: {}", battletag, request.getRemoteAddr());

        try {
            PlayerDTO playerProfile = playerService.getPlayerStats(battletag, platform);
            if (playerProfile == null) {
                log.warn("No player found for battletag: {}", battletag);
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(playerProfile);
        } catch (Exception e) {
            log.error("Error getting player stats for: {}", battletag, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Search for players
     * GET /api/player/search?query=pge&limit=10
     */
    @GetMapping("/search")
    public ResponseEntity<List<PlayerSearchResultDTO>> searchPlayers(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {

        // Validation like Tekken
        if (query == null || query.trim().isBlank() || query.trim().length() >= 30) {
            log.warn("Invalid search query: {}", query);
            return ResponseEntity.badRequest().build();
        }

        log.info("Received search query: {}", query);

        try {
            List<PlayerSearchResultDTO> results = playerService.searchPlayers(query.trim(), limit);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error searching players", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get player metadata (summary info)
     * GET /api/player/metadata/pge-11208?platform=pc
     */
    @GetMapping("/metadata/{battletag}")
    public ResponseEntity<PlayerMetadataDTO> getPlayerMetadata(
            @PathVariable String battletag,
            @RequestParam(defaultValue = "pc") String platform) {

        try {
            PlayerMetadataDTO metadata = playerService.getPlayerMetadata(battletag, platform);
            if (metadata == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            log.error("Error getting player metadata", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get hero stats for player
     * GET /api/player/pge-11208/heroes
     */
    @GetMapping("/{battletag}/heroes")
    public ResponseEntity<List<HeroStatsDTO>> getPlayerHeroStats(
            @PathVariable String battletag,
            @RequestParam(defaultValue = "pc") String platform,
            @RequestParam(required = false) String gameMode) {

        log.info("Getting hero stats for: {} on {} (mode: {})", battletag, platform, gameMode);

        try {
            List<HeroStatsDTO> heroStats = playerService.getHeroStats(battletag, platform, gameMode);
            return ResponseEntity.ok(heroStats);
        } catch (Exception e) {
            log.error("Error getting hero stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}