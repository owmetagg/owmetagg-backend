package com.owmetagg.services;

import com.owmetagg.events.PlayerDataProcessedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class StatisticsCalculationService {

    private final JdbcTemplate jdbcTemplate;
    
    // Minimum games threshold for statistics (lowered for testing)
    private static final int MIN_GAMES_FOR_STATS = 1;
    
    public StatisticsCalculationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Async
    @EventListener
    public void handlePlayerDataProcessedEvent(PlayerDataProcessedEvent event) {
        log.info("📊 Statistics calculation triggered by player update: {}", event.getBattletag());
        
        try {
            // Calculate all statistics in parallel
            calculateHeroStatistics();
            calculateRankDistribution();
            calculateHeroTrends();
            
            log.info("✅ Statistics calculation completed successfully");
        } catch (Exception e) {
            log.error("❌ Failed to calculate statistics", e);
        }
    }
    
    @Transactional
    public void calculateHeroStatistics() {
        log.info("🎮 Calculating hero statistics...");
        long startTime = System.currentTimeMillis();
        
        // Calculate aggregate hero stats across all players
        String sql = """
            INSERT INTO hero_statistics (
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
            )
            SELECT 
                hero_key,
                game_mode,
                SUM(COALESCE(wins, 0) + COALESCE(losses, 0) + COALESCE(draws, 0)) as total_games_played,
                SUM(COALESCE(wins, 0)) as total_wins,
                SUM(COALESCE(losses, 0)) as total_losses,
                COUNT(DISTINCT player_id) as pick_count,
                CAST(COUNT(DISTINCT player_id) AS FLOAT) / 
                    NULLIF((SELECT COUNT(DISTINCT player_id) FROM players), 0) * 100 as pick_rate,
                CASE 
                    WHEN SUM(COALESCE(wins, 0) + COALESCE(losses, 0)) > 0 
                    THEN CAST(SUM(COALESCE(wins, 0)) AS FLOAT) / SUM(COALESCE(wins, 0) + COALESCE(losses, 0)) * 100
                    ELSE 0 
                END as win_rate,
                AVG(CASE WHEN time_played > 0 THEN eliminations::float / (time_played / 60.0) ELSE 0 END) as avg_eliminations,
                AVG(CASE WHEN time_played > 0 THEN deaths::float / (time_played / 60.0) ELSE 0 END) as avg_deaths,
                AVG(CASE WHEN time_played > 0 THEN assists::float / (time_played / 60.0) ELSE 0 END) as avg_assists,
                AVG(
                    CASE 
                        WHEN deaths > 0 THEN (eliminations + assists)::float / deaths
                        WHEN eliminations + assists > 0 THEN (eliminations + assists)::float
                        ELSE 0 
                    END
                ) as avg_kda,
                NOW() as last_calculated
            FROM hero_stats hs
            WHERE time_played >= 0
            GROUP BY hero_key, game_mode
            HAVING SUM(COALESCE(wins, 0) + COALESCE(losses, 0) + COALESCE(draws, 0)) >= 0
            ON CONFLICT (hero_key, game_mode) DO UPDATE SET
                total_games_played = EXCLUDED.total_games_played,
                total_wins = EXCLUDED.total_wins,
                total_losses = EXCLUDED.total_losses,
                pick_count = EXCLUDED.pick_count,
                pick_rate = EXCLUDED.pick_rate,
                win_rate = EXCLUDED.win_rate,
                avg_eliminations = EXCLUDED.avg_eliminations,
                avg_deaths = EXCLUDED.avg_deaths,
                avg_assists = EXCLUDED.avg_assists,
                avg_kda = EXCLUDED.avg_kda,
                last_calculated = EXCLUDED.last_calculated
            """;
        
        int updated = jdbcTemplate.update(sql);
        
        log.info("⚡ Hero statistics calculation completed in {} ms. Updated {} hero entries",
                System.currentTimeMillis() - startTime, updated);
    }
    
    @Transactional
    public void calculateRankDistribution() {
        log.info("🏆 Calculating rank distribution...");
        long startTime = System.currentTimeMillis();
        
        // Calculate distribution across SR brackets
        String sql = """
            INSERT INTO rank_distribution (
                sr_bracket,
                bracket_name,
                player_count,
                percentage,
                snapshot_date
            )
            SELECT 
                CASE 
                    WHEN skill_rating < 1500 THEN 1000
                    WHEN skill_rating < 2000 THEN 1500
                    WHEN skill_rating < 2500 THEN 2000
                    WHEN skill_rating < 3000 THEN 2500
                    WHEN skill_rating < 3500 THEN 3000
                    WHEN skill_rating < 4000 THEN 3500
                    WHEN skill_rating < 4500 THEN 4000
                    ELSE 4500
                END as sr_bracket,
                CASE 
                    WHEN skill_rating < 1500 THEN 'Bronze'
                    WHEN skill_rating < 2000 THEN 'Silver'
                    WHEN skill_rating < 2500 THEN 'Gold'
                    WHEN skill_rating < 3000 THEN 'Platinum'
                    WHEN skill_rating < 3500 THEN 'Diamond'
                    WHEN skill_rating < 4000 THEN 'Master'
                    WHEN skill_rating < 4500 THEN 'Grandmaster'
                    ELSE 'Champion'
                END as bracket_name,
                COUNT(*) as player_count,
                CAST(COUNT(*) AS FLOAT) / (SELECT COUNT(*) FROM players WHERE skill_rating IS NOT NULL) * 100 as percentage,
                CURRENT_DATE as snapshot_date
            FROM players
            WHERE skill_rating IS NOT NULL
            GROUP BY sr_bracket, bracket_name
            ON CONFLICT (sr_bracket, snapshot_date) DO UPDATE SET
                bracket_name = EXCLUDED.bracket_name,
                player_count = EXCLUDED.player_count,
                percentage = EXCLUDED.percentage
            """;
        
        int updated = jdbcTemplate.update(sql);
        
        log.info("⚡ Rank distribution calculation completed in {} ms. Updated {} brackets",
                System.currentTimeMillis() - startTime, updated);
    }
    
    @Transactional
    public void calculateHeroTrends() {
        log.info("📈 Calculating hero trends...");
        long startTime = System.currentTimeMillis();
        
        // Store daily snapshot of hero performance
        String sql = """
            INSERT INTO hero_trends (
                hero_key,
                trend_date,
                game_mode,
                pick_rate,
                win_rate,
                games_played,
                avg_kda
            )
            SELECT 
                hero_key,
                CURRENT_DATE as trend_date,
                game_mode,
                CAST(COUNT(DISTINCT player_id) AS FLOAT) / 
                    (SELECT COUNT(DISTINCT player_id) FROM hero_stats WHERE game_mode = hs.game_mode) * 100 as pick_rate,
                CASE 
                    WHEN SUM(COALESCE(wins, 0) + COALESCE(losses, 0)) > 0 
                    THEN CAST(SUM(COALESCE(wins, 0)) AS FLOAT) / SUM(COALESCE(wins, 0) + COALESCE(losses, 0)) * 100
                    ELSE 0 
                END as win_rate,
                SUM(COALESCE(wins, 0) + COALESCE(losses, 0) + COALESCE(draws, 0)) as games_played,
                AVG(
                    CASE 
                        WHEN deaths > 0 THEN (eliminations + assists)::float / deaths
                        WHEN eliminations + assists > 0 THEN (eliminations + assists)::float
                        ELSE 0 
                    END
                ) as avg_kda
            FROM hero_stats hs
            WHERE time_played > 0
                AND last_played >= CURRENT_DATE - INTERVAL '7 days'
            GROUP BY hero_key, game_mode
            HAVING SUM(COALESCE(wins, 0) + COALESCE(losses, 0) + COALESCE(draws, 0)) >= 0
            ON CONFLICT (hero_key, trend_date, game_mode) DO UPDATE SET
                pick_rate = EXCLUDED.pick_rate,
                win_rate = EXCLUDED.win_rate,
                games_played = EXCLUDED.games_played,
                avg_kda = EXCLUDED.avg_kda
            """;
        
        int updated = jdbcTemplate.update(sql);
        
        log.info("⚡ Hero trends calculation completed in {} ms. Updated {} trend entries",
                System.currentTimeMillis() - startTime, updated);
    }
    
    @Transactional
    public void calculateRoleStatistics() {
        log.info("🛡️ Calculating role-based statistics...");
        long startTime = System.currentTimeMillis();
        
        // Map heroes to roles
        Map<String, String> heroRoles = getHeroRoleMapping();
        
        // Calculate stats per role
        String sql = """
            INSERT INTO role_statistics (
                role,
                game_mode,
                avg_win_rate,
                avg_pick_rate,
                avg_kda,
                total_players,
                last_calculated
            )
            SELECT 
                ? as role,
                game_mode,
                AVG(
                    CASE 
                        WHEN (wins + losses) > 0 
                        THEN CAST(wins AS FLOAT) / (wins + losses) * 100
                        ELSE 0 
                    END
                ) as avg_win_rate,
                CAST(COUNT(DISTINCT player_id) AS FLOAT) / 
                    (SELECT COUNT(DISTINCT player_id) FROM hero_stats WHERE game_mode = hs.game_mode) * 100 as avg_pick_rate,
                AVG(
                    CASE 
                        WHEN deaths > 0 THEN (eliminations + assists)::float / deaths
                        WHEN eliminations + assists > 0 THEN (eliminations + assists)::float
                        ELSE 0 
                    END
                ) as avg_kda,
                COUNT(DISTINCT player_id) as total_players,
                NOW() as last_calculated
            FROM hero_stats hs
            WHERE hero_key = ANY(?)
                AND time_played > 0
            GROUP BY game_mode
            ON CONFLICT (role, game_mode) DO UPDATE SET
                avg_win_rate = EXCLUDED.avg_win_rate,
                avg_pick_rate = EXCLUDED.avg_pick_rate,
                avg_kda = EXCLUDED.avg_kda,
                total_players = EXCLUDED.total_players,
                last_calculated = EXCLUDED.last_calculated
            """;
        
        // Calculate for each role
        for (Map.Entry<String, String> roleEntry : getRoleHeroGroups().entrySet()) {
            String role = roleEntry.getKey();
            String[] heroes = roleEntry.getValue().split(",");
            jdbcTemplate.update(sql, role, heroes);
        }
        
        log.info("⚡ Role statistics calculation completed in {} ms",
                System.currentTimeMillis() - startTime);
    }
    
    private Map<String, String> getHeroRoleMapping() {
        Map<String, String> heroRoles = new HashMap<>();
        
        // Tanks
        String[] tanks = {"dva", "doomfist", "junker-queen", "mauga", "orisa", "ramattra", "reinhardt", "roadhog", "sigma", "winston", "wrecking-ball", "zarya"};
        for (String hero : tanks) heroRoles.put(hero, "tank");
        
        // Damage
        String[] damage = {"ashe", "bastion", "cassidy", "echo", "genji", "hanzo", "junkrat", "mei", "pharah", "reaper", "sojourn", "soldier-76", "sombra", "symmetra", "torbjorn", "tracer", "venture", "widowmaker"};
        for (String hero : damage) heroRoles.put(hero, "damage");
        
        // Support
        String[] support = {"ana", "baptiste", "brigitte", "illari", "juno", "kiriko", "lifeweaver", "lucio", "mercy", "moira", "zenyatta"};
        for (String hero : support) heroRoles.put(hero, "support");
        
        return heroRoles;
    }
    
    private Map<String, String> getRoleHeroGroups() {
        Map<String, String> roleGroups = new HashMap<>();
        roleGroups.put("tank", "dva,doomfist,junker-queen,mauga,orisa,ramattra,reinhardt,roadhog,sigma,winston,wrecking-ball,zarya");
        roleGroups.put("damage", "ashe,bastion,cassidy,echo,genji,hanzo,junkrat,mei,pharah,reaper,sojourn,soldier-76,sombra,symmetra,torbjorn,tracer,venture,widowmaker");
        roleGroups.put("support", "ana,baptiste,brigitte,illari,juno,kiriko,lifeweaver,lucio,mercy,moira,zenyatta");
        return roleGroups;
    }
}