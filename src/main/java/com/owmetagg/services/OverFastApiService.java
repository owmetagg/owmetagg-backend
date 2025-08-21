package com.owmetagg.services;

import com.owmetagg.dtos.overfast.*;
import com.owmetagg.entities.Hero;
import com.owmetagg.repositories.HeroRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class OverFastApiService {

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private HeroRepository heroRepository;

    @Value("${app.overfast.api.base-url}")
    private String overfastBaseUrl;

    private WebClient webClient;

    // Initialize WebClient lazily
    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = webClientBuilder
                    .baseUrl(overfastBaseUrl)
                    .build();
        }
        return webClient;
    }

    /**
     * Fetch all heroes from OverFast API
     */
    public Mono<List<OverFastHeroDTO>> fetchAllHeroes() {
        log.info("Fetching heroes from OverFast API");

        return getWebClient()
                .get()
                .uri("/heroes")
                .retrieve()
                .bodyToFlux(OverFastHeroDTO.class)
                .collectList()
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof WebClientResponseException.TooManyRequests))
                .timeout(Duration.ofSeconds(30))
                .doOnSuccess(heroes -> log.info("Successfully fetched {} heroes from OverFast API", heroes.size()))
                .doOnError(error -> log.error("Failed to fetch heroes from OverFast API", error));
    }

    /**
     * Fetch specific hero details
     */
    public Mono<OverFastHeroDetailDTO> fetchHeroDetails(String heroKey) {
        log.info("Fetching details for hero: {}", heroKey);

        return getWebClient()
                .get()
                .uri("/heroes/{heroKey}", heroKey)
                .retrieve()
                .bodyToMono(OverFastHeroDetailDTO.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .timeout(Duration.ofSeconds(30))
                .doOnSuccess(hero -> log.info("Successfully fetched details for hero: {}", heroKey))
                .doOnError(error -> log.error("Failed to fetch hero details for: {}", heroKey, error));
    }

    /**
     * Fetch player career stats (this gives us match-like data)
     */
    public Mono<OverFastPlayerStatsDTO> fetchPlayerStats(String battleTag, String platform) {
        log.info("Fetching player stats for: {} on {}", battleTag, platform);

        // Clean battle tag for URL (replace # with -)
        String cleanBattleTag = battleTag.replace("#", "-");

        return getWebClient()
                .get()
                .uri("/players/{platform}/{battleTag}/stats/summary", platform.toLowerCase(), cleanBattleTag)
                .retrieve()
                .bodyToMono(OverFastPlayerStatsDTO.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .timeout(Duration.ofSeconds(30))
                .doOnSuccess(stats -> log.info("Successfully fetched stats for player: {}", battleTag))
                .doOnError(error -> log.error("Failed to fetch stats for player: {}", battleTag, error));
    }

    /**
     * Sync heroes from OverFast API to local database
     */
    public Mono<String> syncHeroesFromOverFast() {
        log.info("Starting hero synchronization from OverFast API");

        return fetchAllHeroes()
                .flatMap(this::syncHeroesToDatabase)
                .onErrorResume(error -> {
                    log.error("Hero synchronization failed", error);
                    return Mono.just("Synchronization failed: " + error.getMessage());
                });
    }

    private Mono<String> syncHeroesToDatabase(List<OverFastHeroDTO> overfastHeroes) {
        int updatedCount = 0;
        int createdCount = 0;

        for (OverFastHeroDTO overfastHero : overfastHeroes) {
            Optional<Hero> existingHero = heroRepository.findByName(overfastHero.getName());

            if (existingHero.isPresent()) {
                // Update existing hero
                Hero hero = existingHero.get();
                hero.setRole(mapOverFastRoleToOurRole(overfastHero.getRole()));
                hero.setDescription(overfastHero.getDescription());
                heroRepository.save(hero);
                updatedCount++;
                log.debug("Updated hero: {}", hero.getName());
            } else {
                // Create new hero
                Hero newHero = new Hero();
                newHero.setName(overfastHero.getName());
                newHero.setRole(mapOverFastRoleToOurRole(overfastHero.getRole()));
                newHero.setDescription(overfastHero.getDescription());
                heroRepository.save(newHero);
                createdCount++;
                log.debug("Created new hero: {}", newHero.getName());
            }
        }

        String result = String.format("Hero sync completed: %d created, %d updated", createdCount, updatedCount);
        log.info(result);
        return Mono.just(result);
    }

    /**
     * Map OverFast API role names to our role names
     */
    private String mapOverFastRoleToOurRole(String overfastRole) {
        return switch (overfastRole.toLowerCase()) {
            case "tank" -> "TANK";
            case "damage", "dps" -> "DAMAGE";
            case "support" -> "SUPPORT";
            default -> {
                log.warn("Unknown role from OverFast API: {}, defaulting to DAMAGE", overfastRole);
                yield "DAMAGE";
            }
        };
    }

    /**
     * Test OverFast API connectivity
     */
    public Mono<String> testConnection() {
        log.info("Testing OverFast API connection");

        return getWebClient()
                .get()
                .uri("/heroes")
                .retrieve()
                .bodyToFlux(OverFastHeroDTO.class)
                .take(1) // Just take the first hero to test connectivity
                .collectList()
                .map(heroes -> heroes.isEmpty() ?
                        "OverFast API connection failed - no data received" :
                        "OverFast API connection successful - received hero data")
                .timeout(Duration.ofSeconds(10))
                .onErrorResume(error -> {
                    log.error("OverFast API connection test failed", error);
                    return Mono.just("OverFast API connection failed: " + error.getMessage());
                });
    }

    /**
     * Get API status and rate limit info
     */
    public Mono<OverFastApiStatusDTO> getApiStatus() {
        return getWebClient()
                .get()
                .uri("/")
                .retrieve()
                .bodyToMono(OverFastApiStatusDTO.class)
                .timeout(Duration.ofSeconds(10))
                .onErrorResume(error -> {
                    log.error("Failed to get OverFast API status", error);
                    // Use the correct constructor with all 4 parameters
                    return Mono.just(new OverFastApiStatusDTO("unknown", "Failed to get status", null, null));
                });
    }
}