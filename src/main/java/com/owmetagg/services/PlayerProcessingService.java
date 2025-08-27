package com.owmetagg.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.owmetagg.dtos.PlayerDTO;
import com.owmetagg.events.PlayerDataProcessedEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class PlayerProcessingService {

    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    // Event publishing cooldown (like your Tekken service)
    private static final long COOLDOWN_PERIOD = TimeUnit.MINUTES.toMillis(2); // 2 minute cooldown
    private final AtomicLong lastEventPublishTime = new AtomicLong(0);
    private final AtomicBoolean isPublishing = new AtomicBoolean(false);

    public PlayerProcessingService(
            JdbcTemplate jdbcTemplate,
            ApplicationEventPublisher eventPublisher,
            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void processPlayerDataAsync(PlayerDTO message) throws JsonProcessingException {
        log.info("🔄 Processing player data for: {}", message.getBattletag());

        try {
            // Parse player data from OverFast API response
            JsonNode playerJson = objectMapper.readTree(message.getRawPlayerData());

            // Extract and process player information
            Map<String, Object> playerData = extractPlayerData(playerJson, message);
            Map<String, Object> heroStatsData = extractHeroStats(playerJson, message);

            // Execute bulk database operations (like your Tekken pattern)
            Set<String> insertedPlayerIds = executePlayerBulkWrite(Arrays.asList(playerData));
            executeHeroStatsBulkOperations(heroStatsData);

            if (!insertedPlayerIds.isEmpty()) {
                // Publish event for statistics recalculation (with cooldown like Tekken)
                tryPublishEvent(message.getBattletag());
            }

            log.info("✅ Successfully processed player: {}", message.getBattletag());

        } catch (Exception e) {
            log.error("❌ Failed to process player data for: {}", message.getBattletag(), e);
            throw e;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void processPlayerBatchAsync(List<PlayerDTO> messages) throws JsonProcessingException {
        log.info("🔄 Processing batch of {} players", messages.size());

        try {
            List<Map<String, Object>> playersData = new ArrayList<>();
            List<Map<String, Object>> allHeroStatsList = new ArrayList<>();  // Changed: collect all hero stats in one list

            // Extract data from all messages
            for (PlayerDTO message : messages) {
                JsonNode playerJson = objectMapper.readTree(message.getRawPlayerData());
                playersData.add(extractPlayerData(playerJson, message));

                // Changed: Extract the heroStats list from the map and add all to the combined list
                Map<String, Object> heroStatsData = extractHeroStats(playerJson, message);
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> heroStats = (List<Map<String, Object>>) heroStatsData.get("heroStats");
                if (heroStats != null && !heroStats.isEmpty()) {
                    allHeroStatsList.addAll(heroStats);
                }
            }

            // Bulk operations
            Set<String> insertedPlayerIds = executePlayerBulkWrite(playersData);

            // Changed: Wrap the combined list in a Map as expected by the method
            Map<String, Object> combinedHeroStatsData = new HashMap<>();
            combinedHeroStatsData.put("heroStats", allHeroStatsList);
            executeHeroStatsBulkOperations(combinedHeroStatsData);

            log.info("✅ Successfully processed batch of {} players", messages.size());

        } catch (Exception e) {
            log.error("❌ Failed to process player batch", e);
            throw e;
        }
    }

    /**
     * Bulk player insert/update (adapted from your executePlayerBulkOperations)
     */
    private Set<String> executePlayerBulkWrite(List<Map<String, Object>> playersData) {
        if (playersData.isEmpty()) {
            log.debug("Player data list is empty, skipping");
            return Collections.emptySet();
        }

        long startTime = System.currentTimeMillis();

        String sql =
                "INSERT INTO players " +
                        "(player_id, battletag, platform, region, last_updated, skill_rating, username, avatar_url) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (battletag, platform) DO UPDATE SET " +
                        "region = CASE WHEN EXCLUDED.last_updated > players.last_updated " +
                        "THEN EXCLUDED.region ELSE players.region END, " +
                        "skill_rating = CASE WHEN EXCLUDED.last_updated > players.last_updated " +
                        "THEN EXCLUDED.skill_rating ELSE players.skill_rating END, " +
                        "username = CASE WHEN EXCLUDED.last_updated > players.last_updated " +
                        "THEN EXCLUDED.username ELSE players.username END, " +
                        "avatar_url = CASE WHEN EXCLUDED.last_updated > players.last_updated " +
                        "THEN EXCLUDED.avatar_url ELSE players.avatar_url END, " +
                        "last_updated = CASE WHEN EXCLUDED.last_updated > players.last_updated " +
                        "THEN EXCLUDED.last_updated ELSE players.last_updated END " +
                        "RETURNING battletag";

        Set<String> insertedPlayerIds = jdbcTemplate.execute(
                (Connection con) -> con.prepareStatement(sql, new String[]{"battletag"}),
                (PreparedStatement ps) -> {
                    for (Map<String, Object> playerData : playersData) {
                        int i = 1;
                        ps.setString(i++, (String) playerData.get("playerId"));
                        ps.setString(i++, (String) playerData.get("battletag"));
                        ps.setString(i++, (String) playerData.get("platform"));
                        ps.setString(i++, (String) playerData.get("region"));
                        ps.setTimestamp(i++, Timestamp.valueOf((LocalDateTime) playerData.get("lastUpdated")));
                        setNullableInt(ps, i++, (Integer) playerData.get("skillRating"));
                        ps.setString(i++, (String) playerData.get("username"));
                        ps.setString(i++, (String) playerData.get("avatarUrl"));
                        ps.addBatch();
                    }

                    ps.executeBatch();

                    Set<String> ids = new HashSet<>();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        while (rs != null && rs.next()) {
                            ids.add(rs.getString(1));
                        }
                    }
                    return ids;
                });

        log.info("⚡ Player Bulk Upsert: {} ms, Processed Players: {}",
                (System.currentTimeMillis() - startTime), playersData.size());

        return insertedPlayerIds;
    }

    /**
     * Extract player data from OverFast JSON (like your setPlayerStatsWithBattle)
     */
    private Map<String, Object> extractPlayerData(JsonNode playerJson, PlayerDTO message) {
        Map<String, Object> playerData = new HashMap<>();

        try {
            String playerId = generatePlayerId(message.getBattletag(), message.getPlatform());
            playerData.put("playerId", playerId);
            playerData.put("battletag", message.getBattletag());
            playerData.put("platform", message.getPlatform());
            playerData.put("lastUpdated", LocalDateTime.now());

            // Extract from OverFast API response
            JsonNode summary = playerJson.get("summary");
            if (summary != null) {
                playerData.put("username", summary.has("username") ? summary.get("username").asText() : null);
                playerData.put("avatarUrl", summary.has("avatar") ? summary.get("avatar").asText() : null);

                // Extract competitive ranks - they're in summary.competitive.pc
                if (summary.has("competitive")) {
                    JsonNode competitive = summary.get("competitive");
                    JsonNode pcRanks = competitive.get("pc");  // or use message.getPlatform()

                    if (pcRanks != null) {
                        Integer highestSR = null;

                        // Check each role
                        String[] roles = {"tank", "damage", "support", "open"};
                        for (String role : roles) {
                            if (pcRanks.has(role)) {
                                JsonNode roleRank = pcRanks.get(role);
                                String division = roleRank.get("division").asText();
                                Integer tier = roleRank.get("tier").asInt();

                                Integer roleSR = convertDivisionAndTierToSR(division, tier);
                                log.info("🎯 {} rank: {} {} (SR: {})", role, division, tier, roleSR);

                                if (roleSR != null && (highestSR == null || roleSR > highestSR)) {
                                    highestSR = roleSR;
                                }
                            }
                        }

                        playerData.put("skillRating", highestSR);
                        log.info("💎 Highest SR for {}: {}", message.getBattletag(), highestSR);
                    }
                }
            }

            playerData.put("region", "us"); // Default region

        } catch (Exception e) {
            log.warn("⚠️ Could not extract all player data for: {}", message.getBattletag(), e);
        }

        return playerData;
    }

    // New method to convert OW2 division/tier to SR estimate
    private Integer convertDivisionAndTierToSR(String division, Integer tier) {
        if (division == null || tier == null) return null;

        // OW2 Rank Structure (tier 5 is lowest, 1 is highest within each division)
        Map<String, Integer> divisionBaseRatings = new HashMap<>();
        divisionBaseRatings.put("bronze", 1000);
        divisionBaseRatings.put("silver", 1500);
        divisionBaseRatings.put("gold", 2000);
        divisionBaseRatings.put("platinum", 2500);
        divisionBaseRatings.put("diamond", 3000);
        divisionBaseRatings.put("master", 3500);
        divisionBaseRatings.put("grandmaster", 4000);
        divisionBaseRatings.put("champion", 4500);

        Integer baseRating = divisionBaseRatings.get(division.toLowerCase());
        if (baseRating == null) return null;

        // Each tier is roughly 100 SR, tier 5 = +0, tier 1 = +400
        int tierBonus = (5 - tier) * 100;

        return baseRating + tierBonus;
    }

    /**
     * Extract hero stats from OverFast API response
     */
    private Map<String, Object> extractHeroStats(JsonNode playerJson, PlayerDTO message) {
        Map<String, Object> heroStatsData = new HashMap<>();
        List<Map<String, Object>> heroStatsList = new ArrayList<>();

        try {
            // ADD: Log the entire player JSON structure
            log.info("🔍 Full player JSON for {}: {}", message.getBattletag(), playerJson.toString());

            JsonNode stats = playerJson.get("stats");
            if (stats == null) {
                log.debug("No stats found for player: {}", message.getBattletag());
                return Map.of("heroStats", heroStatsList);
            }

            // ADD: Log what's in stats
            log.info("📊 Stats structure for {}: {}", message.getBattletag(), stats.toString());

            // ADD: Log available platforms
            log.info("🎮 Available platforms in stats: {}", stats.fieldNames());

            // Process PC stats (adjust platform as needed)
            String platform = message.getPlatform();
            JsonNode platformStats = stats.get(platform);
            if (platformStats == null) {
                // ADD: More detailed logging
                log.warn("No {} stats found for player: {}. Available platforms: {}",
                        platform, message.getBattletag(), stats.fieldNames());
                return Map.of("heroStats", heroStatsList);
            }

            // ADD: Log what game modes are available
            log.info("🎯 Available game modes for {}: {}", message.getBattletag(), platformStats.fieldNames());

            // Process competitive stats
            if (platformStats.has("competitive")) {
                JsonNode competitive = platformStats.get("competitive");
                log.info("💪 Processing competitive stats for {}", message.getBattletag());
                extractHeroStatsForGameMode(competitive, heroStatsList, message, "competitive");
            } else {
                log.info("No competitive stats found for {}", message.getBattletag());
            }

            // Process quickplay stats
            if (platformStats.has("quickplay")) {
                JsonNode quickplay = platformStats.get("quickplay");
                log.info("⚡ Processing quickplay stats for {}", message.getBattletag());
                extractHeroStatsForGameMode(quickplay, heroStatsList, message, "quickplay");
            } else {
                log.info("No quickplay stats found for {}", message.getBattletag());
            }

            heroStatsData.put("heroStats", heroStatsList);
            log.info("📊 Extracted {} hero stats entries for: {}", heroStatsList.size(), message.getBattletag());

        } catch (Exception e) {
            log.error("⚠️ Could not extract hero stats for: {}", message.getBattletag(), e);
            heroStatsData.put("heroStats", heroStatsList); // Return empty list on error
        }

        return heroStatsData;
    }

    /**
     * Extract hero stats for a specific game mode
     */
    private void extractHeroStatsForGameMode(JsonNode gameModeStats, List<Map<String, Object>> heroStatsList,
                                             PlayerDTO message, String gameMode) {
        try {
            // Look for career_stats instead of heroes
            JsonNode careerStats = gameModeStats.get("career_stats");
            if (careerStats == null) {
                log.warn("No career_stats found in {} mode", gameMode);
                return;
            }

            // Iterate through each hero in career_stats
            careerStats.fields().forEachRemaining(heroEntry -> {
                String heroKey = heroEntry.getKey();

                // Skip "all-heroes" as it's aggregate data
                if ("all-heroes".equals(heroKey)) {
                    return;
                }

                JsonNode heroData = heroEntry.getValue();

                try {
                    Map<String, Object> heroStatsMap = new HashMap<>();

                    // KEEP: Your existing identifiers
                    heroStatsMap.put("playerId", generatePlayerId(message.getBattletag(), message.getPlatform()));
                    heroStatsMap.put("heroKey", heroKey);
                    heroStatsMap.put("platform", message.getPlatform());
                    heroStatsMap.put("gameMode", gameMode);
                    heroStatsMap.put("lastPlayed", LocalDateTime.now());

                    // NEW: Parse the category-based structure
                    for (JsonNode category : heroData) {
                        String categoryName = category.get("category").asText();
                        JsonNode stats = category.get("stats");

                        if ("game".equals(categoryName) && stats != null) {
                            // Extract game stats (wins, losses, time_played)
                            for (JsonNode stat : stats) {
                                String key = stat.get("key").asText();
                                JsonNode valueNode = stat.get("value");

                                switch (key) {
                                    case "games_won":
                                        heroStatsMap.put("wins", valueNode.asInt(0));
                                        break;
                                    case "games_lost":
                                        heroStatsMap.put("losses", valueNode.asInt(0));
                                        break;
                                    case "time_played":
                                        // Convert seconds to minutes
                                        heroStatsMap.put("timePlayed", valueNode.asInt(0) / 60);
                                        break;
                                }
                            }
                        } else if ("combat".equals(categoryName) && stats != null) {
                            // Extract combat stats
                            for (JsonNode stat : stats) {
                                String key = stat.get("key").asText();
                                JsonNode valueNode = stat.get("value");

                                switch (key) {
                                    case "eliminations":
                                        heroStatsMap.put("eliminations", valueNode.asLong(0));
                                        break;
                                    case "deaths":
                                        heroStatsMap.put("deaths", valueNode.asLong(0));
                                        break;
                                    case "hero_damage_done":
                                        heroStatsMap.put("damageDealt", valueNode.asLong(0));
                                        break;
                                }
                            }
                        } else if ("assists".equals(categoryName) && stats != null) {
                            // Extract assist stats
                            for (JsonNode stat : stats) {
                                String key = stat.get("key").asText();
                                JsonNode valueNode = stat.get("value");

                                switch (key) {
                                    case "assists":
                                        heroStatsMap.put("assists", valueNode.asLong(0));
                                        break;
                                    case "healing_done":
                                        heroStatsMap.put("healingDone", valueNode.asLong(0));
                                        break;
                                }
                            }
                        }
                    }

                    // KEEP: Set defaults for missing values
                    setDefaultValues(heroStatsMap);

                    Integer timePlayed = (Integer) heroStatsMap.get("timePlayed");

                    if (timePlayed != null && timePlayed > 0) {
                        heroStatsList.add(heroStatsMap);
                        log.debug("📈 Extracted stats for {} ({}): {} min played",
                                heroKey, gameMode, timePlayed);
                    } else {
                        log.trace("⏭️ Skipping {} ({}) - no playtime recorded", heroKey, gameMode);
                    }

                } catch (Exception e) {
                    log.warn("⚠️ Failed to extract stats for hero {} ({}): {}",
                            heroKey, gameMode, e.getMessage());
                }
            });

        } catch (Exception e) {
            log.warn("⚠️ Failed to extract hero stats for game mode {}: {}", gameMode, e.getMessage());
        }
    }

    /**
     * Event publishing with cooldown (copied from your tryPublishEvent)
     */
    private void tryPublishEvent(String battletag) {
        long currentTime = System.currentTimeMillis();
        long lastPublishTime = lastEventPublishTime.get();

        // Check if enough time has passed since last publish
        if ((currentTime - lastPublishTime) < COOLDOWN_PERIOD) {
            log.info("⏱️ Skipping statistics computation due to cooldown");
            return;
        }

        // Try to acquire the publishing lock
        if (!isPublishing.compareAndSet(false, true)) {
            log.warn("⚠️ Another thread is currently publishing an event");
            return;
        }

        try {
            // Double-check the time again now that we have the lock
            if ((currentTime - lastEventPublishTime.get()) >= COOLDOWN_PERIOD) {
                eventPublisher.publishEvent(new PlayerDataProcessedEvent(battletag));
                lastEventPublishTime.set(currentTime);
                log.debug("📊 Published statistics computation event for: {}", battletag);
            }
        } finally {
            isPublishing.set(false);
        }
    }

    /**
     * Helper method for nullable integers (copied from your Tekken service)
     */
    private static void setNullableInt(PreparedStatement ps, int idx, Integer val) throws SQLException {
        if (val == null) {
            ps.setNull(idx, Types.INTEGER);
        } else {
            ps.setInt(idx, val);
        }
    }

    /**
     * Extract skill rating from competitive stats
     */
    private Integer extractSkillRating(JsonNode competitive) {
        try {
            // OverFast API may have different structures, try multiple paths

            // Try summary -> skill_rating
            if (competitive.has("summary")) {
                JsonNode summary = competitive.get("summary");
                if (summary.has("skill_rating")) {
                    return summary.get("skill_rating").asInt();
                }
            }

            // Try direct skill_rating field
            if (competitive.has("skill_rating")) {
                return competitive.get("skill_rating").asInt();
            }

            // Try competitive_rank
            if (competitive.has("competitive_rank")) {
                JsonNode rank = competitive.get("competitive_rank");
                if (rank.has("skill_rating")) {
                    return rank.get("skill_rating").asInt();
                }
            }

            // Try tier-based ranking (Bronze, Silver, etc.)
            if (competitive.has("tier")) {
                String tier = competitive.get("tier").asText();
                Integer division = competitive.has("division") ? competitive.get("division").asInt() : null;
                return convertTierToSkillRating(tier, division);
            }

            log.debug("Could not find skill rating in competitive stats structure");
            return null;

        } catch (Exception e) {
            log.warn("⚠️ Could not extract skill rating: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Convert tier/division to approximate skill rating
     */
    private Integer convertTierToSkillRating(String tier, Integer division) {
        if (tier == null) return null;

        // Approximate skill rating ranges (adjust based on current OW2 system)
        Map<String, Integer> tierBaseRatings = Map.of(
                "bronze", 1000,
                "silver", 1500,
                "gold", 2000,
                "platinum", 2500,
                "diamond", 3000,
                "master", 3500,
                "grandmaster", 4000,
                "champion", 4500
        );

        Integer baseRating = tierBaseRatings.get(tier.toLowerCase());
        if (baseRating == null) return null;

        // Adjust for division (5 = lowest, 1 = highest)
        int divisionAdjustment = division != null ? (5 - division) * 100 : 0;
        return baseRating + divisionAdjustment;
    }

    /**
     * Bulk hero stats insert/update (adapted from your Tekken executeCharacterStatsBulkOperations)
     */
    private void executeHeroStatsBulkOperations(Map<String, Object> allHeroStatsData) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> heroStatsList = (List<Map<String, Object>>) allHeroStatsData.get("heroStats");

        if (heroStatsList == null || heroStatsList.isEmpty()) {
            log.debug("Hero stats list is empty, skipping bulk operations");
            return;
        }

        long startTime = System.currentTimeMillis();

        String sql =
                "INSERT INTO hero_stats " +
                        "(player_id, hero_key, platform, game_mode, last_played, wins, losses, draws, " +
                        "time_played, eliminations, deaths, assists, damage_dealt, healing_done, skill_tier) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                        "ON CONFLICT (player_id, hero_key, platform, game_mode) DO UPDATE SET " +

                        "last_played = CASE WHEN EXCLUDED.last_played > hero_stats.last_played " +
                        "THEN EXCLUDED.last_played ELSE hero_stats.last_played END, " +

                        "wins = CASE WHEN EXCLUDED.last_played > hero_stats.last_played " +
                        "THEN EXCLUDED.wins ELSE hero_stats.wins END, " +

                        "losses = CASE WHEN EXCLUDED.last_played > hero_stats.last_played " +
                        "THEN EXCLUDED.losses ELSE hero_stats.losses END, " +

                        "draws = CASE WHEN EXCLUDED.last_played > hero_stats.last_played " +
                        "THEN EXCLUDED.draws ELSE hero_stats.draws END, " +

                        "time_played = CASE WHEN EXCLUDED.last_played > hero_stats.last_played " +
                        "THEN EXCLUDED.time_played ELSE hero_stats.time_played END, " +

                        "eliminations = CASE WHEN EXCLUDED.last_played > hero_stats.last_played " +
                        "THEN EXCLUDED.eliminations ELSE hero_stats.eliminations END, " +

                        "deaths = CASE WHEN EXCLUDED.last_played > hero_stats.last_played " +
                        "THEN EXCLUDED.deaths ELSE hero_stats.deaths END, " +

                        "assists = CASE WHEN EXCLUDED.last_played > hero_stats.last_played " +
                        "THEN EXCLUDED.assists ELSE hero_stats.assists END, " +

                        "damage_dealt = CASE WHEN EXCLUDED.last_played > hero_stats.last_played " +
                        "THEN EXCLUDED.damage_dealt ELSE hero_stats.damage_dealt END, " +

                        "healing_done = CASE WHEN EXCLUDED.last_played > hero_stats.last_played " +
                        "THEN EXCLUDED.healing_done ELSE hero_stats.healing_done END, " +

                        "skill_tier = CASE WHEN EXCLUDED.last_played > hero_stats.last_played " +
                        "THEN EXCLUDED.skill_tier ELSE hero_stats.skill_tier END";

        List<Object[]> batchArgs = new ArrayList<>();
        for (Map<String, Object> heroStats : heroStatsList) {
            Object[] args = new Object[]{
                    heroStats.get("playerId"),
                    heroStats.get("heroKey"),
                    heroStats.get("platform"),
                    heroStats.get("gameMode"),
                    Timestamp.valueOf((LocalDateTime) heroStats.get("lastPlayed")),
                    heroStats.get("wins"),
                    heroStats.get("losses"),
                    heroStats.get("draws"),
                    heroStats.get("timePlayed"),
                    heroStats.get("eliminations"),
                    heroStats.get("deaths"),
                    heroStats.get("assists"),
                    heroStats.get("damageDealt"),
                    heroStats.get("healingDone"),
                    heroStats.get("skillTier") != null ? heroStats.get("skillTier") : 0
            };
            batchArgs.add(args);
        }

        // Sort to prevent deadlocks (like your Tekken pattern)
        batchArgs.sort(Comparator.comparing((Object[] args) -> (String) args[0]) // player_id
                .thenComparing(args -> (String) args[1]) // hero_key
                .thenComparing(args -> (String) args[2])); // platform

        jdbcTemplate.batchUpdate(sql, batchArgs);

        log.info("⚡ HeroStats Bulk Upsert: {} ms, Processed HeroStats: {}",
                (System.currentTimeMillis() - startTime), heroStatsList.size());
    }

    private String generatePlayerId(String battletag, String platform) {
        return battletag.replace("#", "_") + "_" + platform.toLowerCase();
    }

    private int parseTimePlayedToMinutes(JsonNode timePlayedNode) {
        if (timePlayedNode == null || timePlayedNode.isNull()) {
            return 0;
        }

        try {
            String timePlayedStr = timePlayedNode.asText();
            // Parse formats like "10:30:45" (hours:minutes:seconds)
            String[] parts = timePlayedStr.split(":");
            if (parts.length >= 2) {
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                return hours * 60 + minutes;
            }
        } catch (Exception e) {
            log.debug("Could not parse time played: {}", timePlayedNode.asText());
        }
        return 0;
    }

    private int safeGetInt(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asInt() : 0;
    }

    private double safeGetDouble(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asDouble() : 0.0;
    }

    private double getTotalMinutes(Map<String, Object> heroStatsMap) {
        Integer timePlayed = (Integer) heroStatsMap.get("timePlayed");
        return timePlayed != null ? timePlayed.doubleValue() / 10.0 : 0.0; // Convert to 10-minute intervals
    }

    private void setDefaultValues(Map<String, Object> heroStatsMap) {
        heroStatsMap.putIfAbsent("eliminations", 0L);
        heroStatsMap.putIfAbsent("deaths", 0L);
        heroStatsMap.putIfAbsent("assists", 0L);
        heroStatsMap.putIfAbsent("damageDealt", 0L);
        heroStatsMap.putIfAbsent("healingDone", 0L);
        heroStatsMap.putIfAbsent("skillTier", 0);
    }
}