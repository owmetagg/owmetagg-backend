package com.owmetagg.controllers;

import com.owmetagg.entities.Hero;
import com.owmetagg.entities.MatchStats;
import com.owmetagg.repositories.HeroRepository;
import com.owmetagg.repositories.MatchStatsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api/v1/heroes")
@CrossOrigin(origins = "http://localhost:3000")
public class HeroController {

    @Autowired
    private HeroRepository heroRepository;

    @Autowired
    private MatchStatsRepository matchStatsRepository;

    @GetMapping
    public List<Hero> getAllHeroes() {
        return heroRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Hero> getHeroById(@PathVariable Long id) {
        return heroRepository.findById(id)
                .map(hero -> ResponseEntity.ok().body(hero))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/role/{role}")
    public List<Hero> getHeroesByRole(@PathVariable String role) {
        return heroRepository.findByRole(role.toUpperCase());
    }

    @PostMapping
    public Hero createHero(@RequestBody Hero hero) {
        return heroRepository.save(hero);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OWMetaGG Heroes API is healthy!");
    }

    // Debug endpoints
    @GetMapping("/count")
    public ResponseEntity<String> getHeroCount() {
        long count = heroRepository.count();
        return ResponseEntity.ok("Total heroes in database: " + count);
    }

    @PostMapping("/seed")
    public ResponseEntity<String> seedDatabase() {
        // Clear existing data
        heroRepository.deleteAll();

        // Add sample heroes
        heroRepository.save(new Hero(null, "Tracer", "DAMAGE", "High-mobility time-manipulating fighter"));
        heroRepository.save(new Hero(null, "Reinhardt", "TANK", "Barrier-wielding melee fighter"));
        heroRepository.save(new Hero(null, "Mercy", "SUPPORT", "Guardian angel healer"));
        heroRepository.save(new Hero(null, "Genji", "DAMAGE", "Cybernetic ninja"));
        heroRepository.save(new Hero(null, "D.Va", "TANK", "Mobile mech pilot"));
        heroRepository.save(new Hero(null, "Widowmaker", "DAMAGE", "Long-range sniper"));

        long count = heroRepository.count();
        return ResponseEntity.ok("Database seeded with " + count + " heroes");
    }

    @PostMapping("/seed-match-stats")
    public ResponseEntity<String> seedMatchStats() {
        List<Hero> heroes = heroRepository.findAll();
        if (heroes.isEmpty()) {
            return ResponseEntity.badRequest().body("Please seed heroes first using /heroes/seed");
        }

        // Clear existing match stats
        matchStatsRepository.deleteAll();

        Random random = new Random();
        String[] ranks = {"BRONZE", "SILVER", "GOLD", "PLATINUM", "DIAMOND", "MASTER", "GRANDMASTER"};
        String[] regions = {"US", "EU", "ASIA"};
        String[] gameModes = {"COMPETITIVE", "QUICKPLAY"};

        int totalMatches = 0;

        // Generate realistic match data for last 30 days
        for (Hero hero : heroes) {
            for (String rank : ranks) {
                for (String region : regions) {
                    for (String gameMode : gameModes) {
                        // Generate different amounts of data based on rank (Diamond has most data)
                        int numMatches = switch (rank) {
                            case "BRONZE", "SILVER" -> random.nextInt(20) + 10;
                            case "GOLD", "PLATINUM" -> random.nextInt(40) + 20;
                            case "DIAMOND" -> random.nextInt(60) + 40;
                            case "MASTER" -> random.nextInt(30) + 15;
                            case "GRANDMASTER" -> random.nextInt(15) + 5;
                            default -> 10;
                        };

                        for (int i = 0; i < numMatches; i++) {
                            MatchStats matchStats = new MatchStats();

                            // Basic match info
                            matchStats.setHero(hero);
                            matchStats.setPlayerRank(rank);
                            matchStats.setRegion(region);
                            matchStats.setGameMode(gameMode);
                            matchStats.setMatchDate(LocalDateTime.now().minusDays(random.nextInt(30)));

                            // Generate realistic win rates based on hero role and rank
                            double baseWinRate = switch (hero.getRole()) {
                                case "TANK" -> 0.52; // Tanks slightly higher win rate
                                case "SUPPORT" -> 0.51; // Supports slightly higher
                                case "DAMAGE" -> 0.49; // DPS slightly lower
                                default -> 0.50;
                            };

                            // Adjust win rate by rank (higher ranks = slightly better performance)
                            double rankMultiplier = switch (rank) {
                                case "BRONZE" -> 0.95;
                                case "SILVER" -> 0.97;
                                case "GOLD" -> 0.99;
                                case "PLATINUM" -> 1.00;
                                case "DIAMOND" -> 1.02;
                                case "MASTER" -> 1.04;
                                case "GRANDMASTER" -> 1.06;
                                default -> 1.0;
                            };

                            double finalWinRate = baseWinRate * rankMultiplier;
                            matchStats.setWon(random.nextDouble() < finalWinRate);

                            // Generate realistic performance stats
                            matchStats.setEliminations(generateStat(hero.getRole(), "eliminations", rank, random));
                            matchStats.setDeaths(generateStat(hero.getRole(), "deaths", rank, random));
                            matchStats.setAssists(generateStat(hero.getRole(), "assists", rank, random));
                            matchStats.setDamage(generateStat(hero.getRole(), "damage", rank, random));
                            matchStats.setHealing(generateStat(hero.getRole(), "healing", rank, random));

                            matchStats.setMatchDurationSeconds(random.nextInt(1200) + 300); // 5-25 minutes

                            matchStatsRepository.save(matchStats);
                            totalMatches++;
                        }
                    }
                }
            }
        }

        return ResponseEntity.ok("Database seeded with " + totalMatches + " match stats for " + heroes.size() + " heroes");
    }

    private Integer generateStat(String role, String statType, String rank, Random random) {
        // Base stats by role and stat type
        int base = switch (role + "_" + statType) {
            case "TANK_eliminations" -> 15;
            case "TANK_deaths" -> 8;
            case "TANK_assists" -> 12;
            case "TANK_damage" -> 8000;
            case "TANK_healing" -> 0;

            case "DAMAGE_eliminations" -> 25;
            case "DAMAGE_deaths" -> 10;
            case "DAMAGE_assists" -> 8;
            case "DAMAGE_damage" -> 12000;
            case "DAMAGE_healing" -> 0;

            case "SUPPORT_eliminations" -> 8;
            case "SUPPORT_deaths" -> 6;
            case "SUPPORT_assists" -> 20;
            case "SUPPORT_damage" -> 4000;
            case "SUPPORT_healing" -> 8000;

            default -> 0;
        };

        // Rank multiplier (higher ranks = better stats)
        double rankMultiplier = switch (rank) {
            case "BRONZE" -> 0.7;
            case "SILVER" -> 0.8;
            case "GOLD" -> 0.9;
            case "PLATINUM" -> 1.0;
            case "DIAMOND" -> 1.2;
            case "MASTER" -> 1.4;
            case "GRANDMASTER" -> 1.6;
            default -> 1.0;
        };

        int adjustedBase = (int) (base * rankMultiplier);
        return adjustedBase + random.nextInt(Math.max(1, adjustedBase / 2)); // Add some variance
    }
}