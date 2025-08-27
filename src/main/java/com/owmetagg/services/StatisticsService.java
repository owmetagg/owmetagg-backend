package com.owmetagg.services;

import com.owmetagg.dtos.HeroStatsDTO;
import com.owmetagg.dtos.RankDistributionDTO;
import com.owmetagg.dtos.HeroTrendDTO;
import com.owmetagg.dtos.RoleStatisticsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
public class StatisticsService {

    private final JdbcTemplate jdbcTemplate;
    
    public StatisticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Cacheable(value = "heroStatistics", key = "#gameMode + '_' + #sortBy + '_' + #limit")
    public List<HeroStatsDTO> getTopHeroes(String gameMode, String sortBy, int limit) {
        log.info("📊 Fetching top {} heroes for {} mode sorted by {}", limit, gameMode, sortBy);
        
        String orderByClause = switch (sortBy.toLowerCase()) {
            case "winrate" -> "win_rate DESC";
            case "pickrate" -> "pick_rate DESC";
            case "kda" -> "avg_kda DESC";
            case "games" -> "total_games_played DESC";
            default -> "pick_rate DESC";
        };
        
        String sql = """
            SELECT 
                hero_key,
                game_mode,
                total_games_played,
                total_wins,
                total_losses,
                pick_count,
                pick_rate,
                win_rate,
                avg_eliminations,
                avg_deaths,
                avg_assists,
                avg_kda,
                last_calculated
            FROM hero_statistics
            WHERE game_mode = ?
                AND total_games_played >= 10
            ORDER BY %s
            LIMIT ?
            """.formatted(orderByClause);
        
        return jdbcTemplate.query(sql, new HeroStatsRowMapper(), gameMode, limit);
    }
    
    @Cacheable(value = "heroWinRates", key = "#gameMode + '_' + #minGames")
    public List<HeroStatsDTO> getHeroWinRates(String gameMode, int minGames) {
        log.info("📊 Fetching hero win rates for {} mode with min {} games", gameMode, minGames);
        
        String sql = """
            SELECT 
                hero_key,
                game_mode,
                total_games_played,
                total_wins,
                total_losses,
                pick_count,
                pick_rate,
                win_rate,
                avg_eliminations,
                avg_deaths,
                avg_assists,
                avg_kda,
                last_calculated
            FROM hero_statistics
            WHERE game_mode = ?
                AND total_games_played >= ?
            ORDER BY win_rate DESC
            """;
        
        return jdbcTemplate.query(sql, new HeroStatsRowMapper(), gameMode, minGames);
    }
    
    @Cacheable(value = "heroPickRates", key = "#gameMode")
    public List<HeroStatsDTO> getHeroPickRates(String gameMode) {
        log.info("📊 Fetching hero pick rates for {} mode", gameMode);
        
        String sql = """
            SELECT 
                hero_key,
                game_mode,
                total_games_played,
                total_wins,
                total_losses,
                pick_count,
                pick_rate,
                win_rate,
                avg_eliminations,
                avg_deaths,
                avg_assists,
                avg_kda,
                last_calculated
            FROM hero_statistics
            WHERE game_mode = ?
            ORDER BY pick_rate DESC
            """;
        
        return jdbcTemplate.query(sql, new HeroStatsRowMapper(), gameMode);
    }
    
    @Cacheable(value = "rankDistribution", key = "#date")
    public List<RankDistributionDTO> getRankDistribution(LocalDate date) {
        log.info("🏆 Fetching rank distribution for {}", date);
        
        String sql = """
            SELECT 
                sr_bracket,
                bracket_name,
                player_count,
                percentage,
                snapshot_date
            FROM rank_distribution
            WHERE snapshot_date = ?
            ORDER BY sr_bracket ASC
            """;
        
        List<RankDistributionDTO> distribution = jdbcTemplate.query(sql, new RankDistributionRowMapper(), date);
        
        // If no data for specific date, get the most recent
        if (distribution.isEmpty()) {
            sql = """
                SELECT 
                    sr_bracket,
                    bracket_name,
                    player_count,
                    percentage,
                    snapshot_date
                FROM rank_distribution
                WHERE snapshot_date = (SELECT MAX(snapshot_date) FROM rank_distribution)
                ORDER BY sr_bracket ASC
                """;
            distribution = jdbcTemplate.query(sql, new RankDistributionRowMapper());
        }
        
        return distribution;
    }
    
    @Cacheable(value = "heroTrends", key = "#heroKey + '_' + #gameMode + '_' + #days")
    public List<HeroTrendDTO> getHeroTrends(String heroKey, String gameMode, int days) {
        log.info("📈 Fetching {} day trends for hero {} in {} mode", days, heroKey, gameMode);
        
        String sql = """
            SELECT 
                hero_key,
                trend_date,
                game_mode,
                pick_rate,
                win_rate,
                games_played,
                avg_kda
            FROM hero_trends
            WHERE hero_key = ?
                AND game_mode = ?
                AND trend_date >= CURRENT_DATE - INTERVAL '%d days'
            ORDER BY trend_date ASC
            """.formatted(days);
        
        return jdbcTemplate.query(sql, new HeroTrendRowMapper(), heroKey, gameMode);
    }
    
    @Cacheable(value = "allHeroTrends", key = "#gameMode + '_' + #date")
    public List<HeroTrendDTO> getAllHeroTrends(String gameMode, LocalDate date) {
        log.info("📈 Fetching all hero trends for {} mode on {}", gameMode, date);
        
        String sql = """
            SELECT 
                hero_key,
                trend_date,
                game_mode,
                pick_rate,
                win_rate,
                games_played,
                avg_kda
            FROM hero_trends
            WHERE game_mode = ?
                AND trend_date = ?
            ORDER BY pick_rate DESC
            """;
        
        return jdbcTemplate.query(sql, new HeroTrendRowMapper(), gameMode, date);
    }
    
    @Cacheable(value = "roleStatistics", key = "#gameMode")
    public List<RoleStatisticsDTO> getRoleStatistics(String gameMode) {
        log.info("🛡️ Fetching role statistics for {} mode", gameMode);
        
        String sql = """
            SELECT 
                role,
                game_mode,
                avg_win_rate,
                avg_pick_rate,
                avg_kda,
                total_players,
                last_calculated
            FROM role_statistics
            WHERE game_mode = ?
            ORDER BY role
            """;
        
        return jdbcTemplate.query(sql, new RoleStatisticsRowMapper(), gameMode);
    }
    
    @Cacheable(value = "metaReport", key = "#gameMode + '_' + #srBracket")
    public Map<String, Object> getMetaReport(String gameMode, Integer srBracket) {
        log.info("📋 Generating meta report for {} mode at SR bracket {}", gameMode, srBracket);
        
        Map<String, Object> report = new HashMap<>();
        
        // Get top picked heroes
        report.put("topPicks", getTopHeroes(gameMode, "pickrate", 10));
        
        // Get top win rate heroes
        report.put("topWinRates", getTopHeroes(gameMode, "winrate", 10));
        
        // Get role statistics
        report.put("roleStats", getRoleStatistics(gameMode));
        
        // Get current rank distribution
        report.put("rankDistribution", getRankDistribution(LocalDate.now()));
        
        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("gameMode", gameMode);
        metadata.put("srBracket", srBracket);
        metadata.put("generatedAt", LocalDate.now());
        report.put("metadata", metadata);
        
        return report;
    }
    
    // Row Mappers
    private static class HeroStatsRowMapper implements RowMapper<HeroStatsDTO> {
        @Override
        public HeroStatsDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            HeroStatsDTO dto = new HeroStatsDTO();
            dto.setHeroKey(rs.getString("hero_key"));
            dto.setGameMode(rs.getString("game_mode"));
            dto.setTotalGamesPlayed(rs.getLong("total_games_played"));
            dto.setTotalWins(rs.getLong("total_wins"));
            dto.setTotalLosses(rs.getLong("total_losses"));
            dto.setPickCount(rs.getInt("pick_count"));
            dto.setPickRate(rs.getDouble("pick_rate"));
            dto.setWinRate(rs.getDouble("win_rate"));
            dto.setAvgEliminations(rs.getDouble("avg_eliminations"));
            dto.setAvgDeaths(rs.getDouble("avg_deaths"));
            dto.setAvgAssists(rs.getDouble("avg_assists"));
            dto.setAvgKda(rs.getDouble("avg_kda"));
            dto.setLastCalculated(rs.getTimestamp("last_calculated").toLocalDateTime());
            return dto;
        }
    }
    
    private static class RankDistributionRowMapper implements RowMapper<RankDistributionDTO> {
        @Override
        public RankDistributionDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            RankDistributionDTO dto = new RankDistributionDTO();
            dto.setSrBracket(rs.getInt("sr_bracket"));
            dto.setBracketName(rs.getString("bracket_name"));
            dto.setPlayerCount(rs.getInt("player_count"));
            dto.setPercentage(rs.getDouble("percentage"));
            dto.setSnapshotDate(rs.getDate("snapshot_date").toLocalDate());
            return dto;
        }
    }
    
    private static class HeroTrendRowMapper implements RowMapper<HeroTrendDTO> {
        @Override
        public HeroTrendDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            HeroTrendDTO dto = new HeroTrendDTO();
            dto.setHeroKey(rs.getString("hero_key"));
            dto.setTrendDate(rs.getDate("trend_date").toLocalDate());
            dto.setGameMode(rs.getString("game_mode"));
            dto.setPickRate(rs.getDouble("pick_rate"));
            dto.setWinRate(rs.getDouble("win_rate"));
            dto.setGamesPlayed(rs.getLong("games_played"));
            dto.setAvgKda(rs.getDouble("avg_kda"));
            return dto;
        }
    }
    
    private static class RoleStatisticsRowMapper implements RowMapper<RoleStatisticsDTO> {
        @Override
        public RoleStatisticsDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
            RoleStatisticsDTO dto = new RoleStatisticsDTO();
            dto.setRole(rs.getString("role"));
            dto.setGameMode(rs.getString("game_mode"));
            dto.setAvgWinRate(rs.getDouble("avg_win_rate"));
            dto.setAvgPickRate(rs.getDouble("avg_pick_rate"));
            dto.setAvgKda(rs.getDouble("avg_kda"));
            dto.setTotalPlayers(rs.getInt("total_players"));
            dto.setLastCalculated(rs.getTimestamp("last_calculated").toLocalDateTime());
            return dto;
        }
    }
}