package com.owmetagg.services;

import com.owmetagg.dtos.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerService {

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Get player stats/profile from database
     */
    public PlayerDTO getPlayerStats(String battletag, String platform) {  // Changed return type
        String sql = """
        SELECT p.*, 
               (SELECT COUNT(DISTINCT hero_key) FROM hero_stats WHERE player_id = p.player_id) as heroes_played,
               (SELECT SUM(wins) FROM hero_stats WHERE player_id = p.player_id) as total_wins,
               (SELECT SUM(losses) FROM hero_stats WHERE player_id = p.player_id) as total_losses,
               (SELECT SUM(time_played) FROM hero_stats WHERE player_id = p.player_id) as total_play_time
        FROM players p
        WHERE p.battletag = ? AND p.platform = ?
    """;

        return jdbcTemplate.query(sql, new Object[]{battletag, platform}, rs -> {
            if (rs.next()) {
                Integer totalWins = rs.getInt("total_wins");
                Integer totalLosses = rs.getInt("total_losses");
                double winRate = totalWins + totalLosses > 0 ?
                        (double) totalWins / (totalWins + totalLosses) * 100 : 0;

                // Build the full PlayerDTO with nested data
                return PlayerDTO.builder()
                        .battletag(rs.getString("battletag"))
                        .username(rs.getString("username"))
                        .avatarUrl(rs.getString("avatar_url"))
                        .platform(rs.getString("platform"))
                        .skillRating(rs.getObject("skill_rating", Integer.class))
                        .totalPlayTime(rs.getLong("total_play_time"))
                        .latestSession(rs.getTimestamp("last_updated").getTime())
                        // Add nested complex data here
                        .playedHeroes(getPlayedHeroesMap(battletag, platform))
                        .build();
            }
            return null;
        });
    }

    /**
     * Search players - first check database, then OverFast API if needed
     */
    public List<PlayerSearchResultDTO> searchPlayers(String query, int limit) {
        // First try local database
        String sql = """
            SELECT player_id, battletag, username, avatar_url, platform, skill_rating
            FROM players
            WHERE LOWER(battletag) LIKE LOWER(?) OR LOWER(username) LIKE LOWER(?)
            ORDER BY last_updated DESC
            LIMIT ?
        """;

        String searchPattern = "%" + query + "%";
        List<PlayerSearchResultDTO> dbResults = jdbcTemplate.query(
                sql,
                new Object[]{searchPattern, searchPattern, limit},
                (rs, rowNum) -> PlayerSearchResultDTO.builder()
                        .playerId(rs.getString("player_id"))
                        .battletag(rs.getString("battletag"))
                        .name(rs.getString("username"))
                        .avatarUrl(rs.getString("avatar_url"))
                        .platform(rs.getString("platform"))
                        .build()
        );

        // If we have enough results from DB, return them
        if (dbResults.size() >= limit) {
            return dbResults;
        }

        // Otherwise, search OverFast API
        try {
            String url = String.format("https://overfast-api.tekrop.fr/players?name=%s&limit=%d", query, limit);
            String response = restTemplate.getForObject(url, String.class);

            JsonNode json = objectMapper.readTree(response);
            JsonNode results = json.get("results");

            List<PlayerSearchResultDTO> apiResults = new ArrayList<>();
            if (results != null && results.isArray()) {
                for (JsonNode player : results) {
                    apiResults.add(PlayerSearchResultDTO.builder()
                            .playerId(player.get("player_id").asText())
                            .battletag(player.get("name").asText().replace("#", "-"))
                            .name(player.get("name").asText())
                            .avatarUrl(player.get("avatar").asText())
                            .platform("pc")
                            .build());
                }
            }

            // Combine and deduplicate results
            Set<String> existingBattletags = dbResults.stream()
                    .map(PlayerSearchResultDTO::getBattletag)
                    .collect(Collectors.toSet());

            for (PlayerSearchResultDTO apiResult : apiResults) {
                if (!existingBattletags.contains(apiResult.getBattletag()) && dbResults.size() < limit) {
                    dbResults.add(apiResult);
                }
            }

            return dbResults;
        } catch (Exception e) {
            log.error("Error searching OverFast API", e);
            return dbResults; // Return what we have from DB
        }
    }

    /**
     * Get player metadata
     */
    public PlayerMetadataDTO getPlayerMetadata(String battletag, String platform) {
        String sql = """
            SELECT battletag, username, avatar_url, skill_rating, last_updated
            FROM players
            WHERE battletag = ? AND platform = ?
        """;

        return jdbcTemplate.query(sql, new Object[]{battletag, platform}, rs -> {
            if (rs.next()) {
                return PlayerMetadataDTO.builder()
                        .battletag(rs.getString("battletag"))
                        .username(rs.getString("username"))
                        .avatarUrl(rs.getString("avatar_url"))
                        .skillRating(rs.getObject("skill_rating", Integer.class))
                        .currentRank(convertSRToRank(rs.getObject("skill_rating", Integer.class)))
                        .lastUpdated(rs.getTimestamp("last_updated").toLocalDateTime())
                        .build();
            }
            return null;
        });
    }

    /**
     * Get recently active players
     */
    public List<RecentlyActivePlayerDTO> getRecentlyActivePlayers() {
        String sql = """
            SELECT p.*, 
                   (SELECT hero_key FROM hero_stats 
                    WHERE player_id = p.player_id 
                    ORDER BY time_played DESC LIMIT 1) as most_played_hero,
                   (SELECT SUM(wins + losses) FROM hero_stats 
                    WHERE player_id = p.player_id) as total_games
            FROM players p
            ORDER BY p.last_updated DESC
            LIMIT 20
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) ->
                RecentlyActivePlayerDTO.builder()
                        .playerId(rs.getString("player_id"))
                        .battletag(rs.getString("battletag"))
                        .username(rs.getString("username"))
                        .avatarUrl(rs.getString("avatar_url"))
                        .platform(rs.getString("platform"))
                        .skillRating(rs.getObject("skill_rating", Integer.class))
                        .lastUpdated(rs.getTimestamp("last_updated").toLocalDateTime())
                        .recentGamesPlayed(rs.getInt("total_games"))
                        .build()
        );
    }

    /**
     * Get player ID from battletag
     */
    public String getPlayerIdFromBattletag(String battletag, String platform) {
        return battletag.replace("#", "_") + "_" + platform.toLowerCase();
    }

    /**
     * Get hero stats
     */
    public List<HeroStatsDTO> getHeroStats(String battletag, String platform, String gameMode) {
        // Implementation from earlier
        return new ArrayList<>();
    }

    /**
     * Get top heroes
     */
    public List<HeroStatsDTO> getTopHeroes(String battletag, String platform, int limit) {
        List<HeroStatsDTO> allHeroes = getHeroStats(battletag, platform, "competitive");
        return allHeroes.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get played heroes map for a player
     */
    private Map<String, HeroSummaryDTO> getPlayedHeroesMap(String battletag, String platform) {
        String playerId = getPlayerIdFromBattletag(battletag, platform);
        String sql = """
            SELECT hero_key, 
                   SUM(wins) as wins,
                   SUM(losses) as losses,
                   SUM(time_played) as time_played,
                   SUM(eliminations) as eliminations,
                   SUM(deaths) as deaths,
                   SUM(assists) as assists
            FROM hero_stats
            WHERE player_id = ?
            GROUP BY hero_key
        """;
        
        Map<String, HeroSummaryDTO> heroesMap = new HashMap<>();
        jdbcTemplate.query(sql, new Object[]{playerId}, rs -> {
            while (rs.next()) {
                String heroKey = rs.getString("hero_key");
                int wins = rs.getInt("wins");
                int losses = rs.getInt("losses");
                int timePlayed = rs.getInt("time_played");
                long eliminations = rs.getLong("eliminations");
                long deaths = rs.getLong("deaths");
                long assists = rs.getLong("assists");
                
                HeroSummaryDTO summary = new HeroSummaryDTO();
                summary.setHeroKey(heroKey);
                summary.setHeroName(formatHeroName(heroKey));
                summary.setTimePlayed(timePlayed);
                summary.setGamesPlayed(wins + losses);
                summary.setWins(wins);
                summary.setLosses(losses);
                summary.setWinRate(wins + losses > 0 ? (double) wins / (wins + losses) * 100 : 0);
                summary.setKda(deaths > 0 ? (double)(eliminations + assists) / deaths : eliminations + assists);
                
                heroesMap.put(heroKey, summary);
            }
        });
        
        return heroesMap;
    }

    // Helper methods
    private String convertSRToRank(Integer sr) {
        if (sr == null) return "Unranked";

        if (sr >= 4500) return "Champion";
        if (sr >= 4000) return "Grandmaster " + (5 - (sr - 4000) / 100);
        if (sr >= 3500) return "Master " + (5 - (sr - 3500) / 100);
        if (sr >= 3000) return "Diamond " + (5 - (sr - 3000) / 100);
        if (sr >= 2500) return "Platinum " + (5 - (sr - 2500) / 100);
        if (sr >= 2000) return "Gold " + (5 - (sr - 2000) / 100);
        if (sr >= 1500) return "Silver " + (5 - (sr - 1500) / 100);
        return "Bronze " + (5 - (sr - 1000) / 100);
    }

    private String formatHeroName(String heroKey) {
        if (heroKey == null) return "";

        Map<String, String> specialNames = Map.of(
                "dva", "D.Va",
                "junker-queen", "Junker Queen",
                "soldier-76", "Soldier: 76",
                "wrecking-ball", "Wrecking Ball"
        );

        if (specialNames.containsKey(heroKey)) {
            return specialNames.get(heroKey);
        }

        return Arrays.stream(heroKey.split("-"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}