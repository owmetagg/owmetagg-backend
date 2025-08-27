package com.owmetagg.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.owmetagg.utils.OverwatchDataMapperUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.owmetagg.utils.Constants.*;

@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
public class Player {

    @Id
    @Column(name = "player_id", unique = true, nullable = false)
    private String playerId;

    @Column(name = "battletag", nullable = false)
    private String battletag;

    @Column(name = "platform", nullable = false)
    private String platform;

    @Column(name = "username")
    private String username;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "region")
    private String region;

    @Column(name = "skill_rating")
    private Integer skillRating;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    // FIXED: Changed from Map to List since we're using composite key
    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<HeroStats> heroStats = new ArrayList<>();

    // If you need map-like access, create a helper method
    public Map<HeroStatsId, HeroStats> getHeroStatsAsMap() {
        return heroStats.stream()
                .collect(Collectors.toMap(
                        HeroStats::getId,
                        hs -> hs,
                        (existing, replacement) -> existing
                ));
    }

    // Update your business logic methods to work with List instead of Map
    private Map<String, String> findMainHero() {
        Map<String, String> result = new HashMap<>();

        if (heroStats == null || heroStats.isEmpty()) {
            result.put(HERO_KEY, "No Hero Data");
            result.put(SKILL_TIER, "0");
            return result;
        }

        // Step 1: Find the highest skill tier
        int highestSkillTier = heroStats.stream()
                .mapToInt(HeroStats::getSkillTier)
                .max()
                .orElse(0);

        // Step 2: Group heroes and sum their total playtime
        Map<String, Integer> totalPlaytimeByHero = heroStats.stream()
                .collect(Collectors.groupingBy(
                        hs -> hs.getId().getHeroKey(),
                        Collectors.summingInt(HeroStats::getTimePlayed)
                ));

        // Step 3: Find heroes that reached the highest skill tier
        Set<String> heroesWithHighestTier = heroStats.stream()
                .filter(hs -> hs.getSkillTier() == highestSkillTier)
                .map(hs -> hs.getId().getHeroKey())
                .collect(Collectors.toSet());

        // Step 4: Among heroes with highest tier, find the one with most playtime
        return heroesWithHighestTier.stream()
                .max(Comparator.comparingInt(totalPlaytimeByHero::get))
                .map(heroKey -> {
                    Map<String, String> map = new HashMap<>();
                    map.put(HERO_KEY, heroKey);
                    map.put(SKILL_TIER, String.valueOf(highestSkillTier));
                    return map;
                })
                .orElseGet(() -> {
                    Map<String, String> map = new HashMap<>();
                    map.put(HERO_KEY, "No Hero Data");
                    map.put(SKILL_TIER, "0");
                    return map;
                });
    }

    public Map<String, String> getRecentlyPlayedHero() {
        Map<String, String> result = new HashMap<>();

        if (heroStats == null || heroStats.isEmpty()) {
            result.put(HERO_NAME, "No Hero Data");
            result.put(SKILL_TIER, "N/A");
            return result;
        }

        // Find the hero stats with the latest update timestamp
        Optional<HeroStats> latestHeroStats = heroStats.stream()
                .max(Comparator.comparing(HeroStats::getLastPlayed));

        if (latestHeroStats.isPresent()) {
            String heroKey = latestHeroStats.get().getId().getHeroKey();
            int skillTier = latestHeroStats.get().getSkillTier();
            result.put(HERO_NAME, OverwatchDataMapperUtils.getHeroName(heroKey));
            result.put(SKILL_TIER, OverwatchDataMapperUtils.getSkillTierName(String.valueOf(skillTier)));
        }

        return result;
    }

    // Rest of your methods remain the same...
}