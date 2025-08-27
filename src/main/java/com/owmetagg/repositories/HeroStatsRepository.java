package com.owmetagg.repositories;

import com.owmetagg.models.HeroStats;
import com.owmetagg.models.HeroStatsId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HeroStatsRepository extends JpaRepository<HeroStats, HeroStatsId> {

    // Basic lookups
    List<HeroStats> findByIdPlayerId(String playerId);
    List<HeroStats> findByIdHeroKey(String heroKey);
    Optional<HeroStats> findByIdPlayerIdAndIdHeroKey(String playerId, String heroKey);

    // Statistics calculation queries
    @Query("SELECT COUNT(DISTINCT hs.id.playerId) FROM HeroStats hs WHERE hs.id.heroKey = :heroKey")
    long countDistinctPlayersByHeroKey(@Param("heroKey") String heroKey);

    // Win rate calculation (adapted from your pattern)
    @Query("SELECT COALESCE(AVG(CAST(hs.wins AS double) / NULLIF(hs.wins + hs.losses + hs.draws, 0) * 100), 0) " +
            "FROM HeroStats hs WHERE hs.id.heroKey = :heroKey AND hs.id.gameMode = :gameMode " +
            "AND (hs.wins + hs.losses + hs.draws) >= :minGames")
    Double calculateWinRateByHeroKey(@Param("heroKey") String heroKey,
                                     @Param("gameMode") String gameMode,
                                     @Param("minGames") int minGames);

    // KDA calculation
    @Query("SELECT COALESCE(AVG(CAST(hs.eliminations + hs.assists AS double) / NULLIF(hs.deaths, 0)), 0) " +
            "FROM HeroStats hs WHERE hs.id.heroKey = :heroKey AND hs.id.gameMode = :gameMode " +
            "AND hs.deaths > 0 AND hs.timePlayed >= :minTimePlayed")
    Double calculateAverageKDAByHeroKey(@Param("heroKey") String heroKey,
                                        @Param("gameMode") String gameMode,
                                        @Param("minTimePlayed") int minTimePlayed);

    // Top performers for a hero (like your active players query)
    @Query("SELECT hs FROM HeroStats hs WHERE hs.id.heroKey = :heroKey " +
            "AND hs.id.gameMode = :gameMode AND hs.timePlayed >= :minTimePlayed " +
            "ORDER BY hs.skillTier DESC, hs.timePlayed DESC")
    List<HeroStats> findTopPerformersForHero(@Param("heroKey") String heroKey,
                                             @Param("gameMode") String gameMode,
                                             @Param("minTimePlayed") int minTimePlayed,
                                             PageRequest pageRequest);

    // Recently played heroes (adapted from your time-based queries)
    @Query(value = "SELECT * FROM hero_stats " +
            "WHERE last_played > (NOW() - INTERVAL '7 days') " +
            "ORDER BY last_played DESC LIMIT :limit", nativeQuery = true)
    List<HeroStats> findRecentlyPlayedHeroes(@Param("limit") int limit);

    // Hero popularity by platform
    @Query("SELECT hs.id.heroKey, COUNT(DISTINCT hs.id.playerId) as playerCount " +
            "FROM HeroStats hs WHERE hs.id.platform = :platform " +
            "AND hs.id.gameMode = :gameMode " +
            "GROUP BY hs.id.heroKey ORDER BY playerCount DESC")
    List<Object[]> getHeroPopularityByPlatform(@Param("platform") String platform,
                                               @Param("gameMode") String gameMode);

    // Aggregate statistics for role analysis
    @Query("SELECT h.role, " +
            "COUNT(DISTINCT hs.id.playerId) as totalPlayers, " +
            "AVG(CAST(hs.wins AS double) / NULLIF(hs.wins + hs.losses + hs.draws, 0) * 100) as avgWinRate, " +
            "AVG(CAST(hs.eliminations + hs.assists AS double) / NULLIF(hs.deaths, 0)) as avgKDA " +
            "FROM HeroStats hs " +
            "JOIN Hero h ON h.heroKey = hs.id.heroKey " +
            "WHERE hs.id.gameMode = :gameMode AND hs.id.platform = :platform " +
            "GROUP BY h.role")
    List<Object[]> getRoleStatistics(@Param("gameMode") String gameMode, @Param("platform") String platform);

    // Batch processing support (like your sophisticated queries)
    @Query("SELECT hs FROM HeroStats hs WHERE hs.id.heroKey IN :heroKeys " +
            "AND hs.id.gameMode = :gameMode AND hs.timePlayed >= :minTimePlayed")
    List<HeroStats> findHeroStatsBatch(@Param("heroKeys") List<String> heroKeys,
                                       @Param("gameMode") String gameMode,
                                       @Param("minTimePlayed") int minTimePlayed);

    // Data quality queries
    @Query("SELECT COUNT(hs) FROM HeroStats hs WHERE hs.wins = 0 AND hs.losses = 0 AND hs.draws = 0")
    long countEmptyStats();

    @Query("SELECT hs.id.heroKey, COUNT(hs) FROM HeroStats hs GROUP BY hs.id.heroKey ORDER BY COUNT(hs) DESC")
    List<Object[]> getHeroStatsDistribution();
}