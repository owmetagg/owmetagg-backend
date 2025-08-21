package com.owmetagg.services;

import com.owmetagg.dtos.overfast.OverFastHeroStatsDTO;
import com.owmetagg.dtos.overfast.OverFastPlayerStatsDTO;
import com.owmetagg.entities.Hero;
import com.owmetagg.entities.MatchStats;
import com.owmetagg.repositories.HeroRepository;
import com.owmetagg.repositories.MatchStatsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
@Slf4j
public class DataProcessingService {

    @Autowired
    private OverFastApiService overFastApiService;

    @Autowired
    private HeroRepository heroRepository;

    @Autowired
    private MatchStatsRepository matchStatsRepository;

    private final Random random = new Random();

    /**
     * Scheduled job to sync heroes daily
     */
    @Scheduled(cron = "0 0 6 * * ?") // Every day at 6 AM
    public void dailyHeroSync() {
        log.info("Starting daily hero synchronization");

        overFastApiService.syncHeroesFromOverFast()
                .doOnSuccess(result -> log.info("Daily hero sync completed: {}", result))
                .doOnError(error -> log.error("Daily hero sync failed", error))
                .subscribe();
    }

    /**
     * Process player stats and convert to match stats
     * This simulates getting match data from player career stats
     */
    @Transactional
    public String processPlayerStatsToMatchStats(String battleTag, String platform) {
        log.info("Processing player stats for: {} on {}", battleTag, platform);

        try {
            OverFastPlayerStatsDTO playerStats = overFastApiService.fetchPlayerStats(battleTag, platform)
                    .block(); // Block for synchronous processing

            if (playerStats == null || playerStats.getStats() == null) {
                return "No stats found for player: " + battleTag;
            }

            int processedMatches = 0;

            // Process competitive stats
            if (playerStats.getStats().getCompetitive() != null &&
                    playerStats.getStats().getCompetitive().getCareerStats() != null) {

                processedMatches += processHeroStats(
                        playerStats.getStats().getCompetitive().getCareerStats(),
                        battleTag,
                        "COMPETITIVE",
                        getPlayerRankFromCompetitive(playerStats.getCompetitive()),
                        getPlayerRegion(battleTag)
                );
            }

            // Process quickplay stats
            if (playerStats.getStats().getQuickplay() != null &&
                    playerStats.getStats().getQuickplay().getCareerStats() != null) {

                processedMatches += processHeroStats(
                        playerStats.getStats().getQuickplay().getCareerStats(),
                        battleTag,
                        "QUICKPLAY",
                        "UNRANKED",
                        getPlayerRegion(battleTag)
                );
            }

            String result = String.format("Processed %d hero stat entries for player %s", processedMatches, battleTag);
            log.info(result);
            return result;

        } catch (Exception e) {
            log.error("Failed to process player stats for: {}", battleTag, e);
            return "Failed to process player stats: " + e.getMessage();
        }
    }

    private int processHeroStats(Map<String, OverFastHeroStatsDTO> heroStatsMap,
                                 String battleTag, String gameMode, String rank, String region) {

        int processedCount = 0;

        for (Map.Entry<String, OverFastHeroStatsDTO> entry : heroStatsMap.entrySet()) {
            String heroName = entry.getKey();
            OverFastHeroStatsDTO heroStats = entry.getValue();

            // Find hero in our database
            Optional<Hero> heroOpt = heroRepository.findByName(heroName);
            if (heroOpt.isEmpty()) {
                log.warn("Hero not found in database: {}", heroName);
                continue;
            }

            Hero hero = heroOpt.get();

            // Convert hero stats to multiple match stats entries
            // Since OverFast gives us aggregated stats, we'll simulate individual matches
            int gamesPlayed = heroStats.getGamesPlayed() != null ? heroStats.getGamesPlayed() : 0;
            if (gamesPlayed == 0) continue;

            // Create up to 10 simulated matches from the aggregated stats
            int matchesToCreate = Math.min(gamesPlayed, 10);

            for (int i = 0; i < matchesToCreate; i++) {
                MatchStats matchStats = createMatchStatsFromHeroStats(
                        hero, heroStats, battleTag, gameMode, rank, region, gamesPlayed
                );

                matchStatsRepository.save(matchStats);
                processedCount++;
            }
        }

        return processedCount;
    }

    private MatchStats createMatchStatsFromHeroStats(Hero hero, OverFastHeroStatsDTO heroStats,
                                                     String battleTag, String gameMode, String rank, String region,
                                                     int totalGames) {
        MatchStats matchStats = new MatchStats();

        // Basic info
        matchStats.setHero(hero);
        matchStats.setPlayerId(battleTag);
        matchStats.setPlayerName(extractPlayerName(battleTag));
        matchStats.setPlayerRank(rank);
        matchStats.setRegion(region);
        matchStats.setGameMode(gameMode);
        matchStats.setMatchDate(LocalDateTime.now().minusDays(random.nextInt(30)));

        // Win/Loss (based on overall win rate with some randomness)
        Double winRate = heroStats.getWinRate();
        if (winRate != null) {
            matchStats.setWon(random.nextDouble() * 100 < winRate);
        } else {
            matchStats.setWon(random.nextBoolean());
        }

        // Performance stats (distribute totals across simulated matches)
        if (heroStats.getEliminations() != null && totalGames > 0) {
            int avgElims = heroStats.getEliminations() / totalGames;
            matchStats.setEliminations(addVariance(avgElims));
        }

        if (heroStats.getDeaths() != null && totalGames > 0) {
            int avgDeaths = heroStats.getDeaths() / totalGames;
            matchStats.setDeaths(addVariance(avgDeaths));
        }

        if (heroStats.getAssists() != null && totalGames > 0) {
            int avgAssists = heroStats.getAssists() / totalGames;
            matchStats.setAssists(addVariance(avgAssists));
        }

        if (heroStats.getDamageDone() != null && totalGames > 0) {
            int avgDamage = heroStats.getDamageDone() / totalGames;
            matchStats.setDamage(addVariance(avgDamage));
        }

        if (heroStats.getHealingDone() != null && totalGames > 0) {
            int avgHealing = heroStats.getHealingDone() / totalGames;
            matchStats.setHealing(addVariance(avgHealing));
        }

        // Time played (estimate match duration)
        Integer timePlayedSeconds = heroStats.getTimePlayedSeconds();
        if (timePlayedSeconds != null && totalGames > 0) {
            int avgMatchTime = timePlayedSeconds / totalGames;
            matchStats.setMatchDurationSeconds(addVariance(avgMatchTime));
            matchStats.setTimePlayedSeconds(addVariance(avgMatchTime));
        } else {
            // Default match duration
            matchStats.setMatchDurationSeconds(random.nextInt(1200) + 300); // 5-25 minutes
        }

        return matchStats;
    }

    private int addVariance(int baseValue) {
        if (baseValue <= 0) return 0;
        int variance = (int) (baseValue * 0.3); // 30% variance
        return Math.max(0, baseValue + random.nextInt(variance * 2) - variance);
    }

    private String getPlayerRankFromCompetitive(Map<String, ?> competitive) {
        if (competitive == null) return "UNRANKED";

        // Try to extract rank from competitive data
        // This is simplified - OverFast API structure may vary
        return "DIAMOND"; // Default for now
    }

    private String getPlayerRegion(String battleTag) {
        // Simple region detection based on battle tag patterns
        // This is a simplified approach
        if (battleTag.contains("EU") || battleTag.contains("eu")) return "EU";
        if (battleTag.contains("ASIA") || battleTag.contains("asia")) return "ASIA";
        return "US"; // Default
    }

    private String extractPlayerName(String battleTag) {
        // Extract player name from battle tag (before the #)
        int hashIndex = battleTag.indexOf('#');
        if (hashIndex > 0) {
            return battleTag.substring(0, hashIndex);
        }
        return battleTag;
    }

    /**
     * Manual trigger for processing a specific player
     */
    public String processSpecificPlayer(String battleTag, String platform) {
        return processPlayerStatsToMatchStats(battleTag, platform);
    }
}