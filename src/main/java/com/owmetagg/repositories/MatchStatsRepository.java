package com.owmetagg.repositories;

import com.owmetagg.entities.MatchStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MatchStatsRepository extends JpaRepository<MatchStats, Long> {

    // Hero Win Rate Analysis - Fixed column names
    @Query("""
        SELECT h.name as heroName,
               h.role as heroRole,
               ms.playerRank as rank,
               ms.region as region,
               COUNT(ms) as totalMatches,
               SUM(CASE WHEN ms.won = true THEN 1 ELSE 0 END) as wins,
               ROUND(AVG(CASE WHEN ms.won = true THEN 1.0 ELSE 0.0 END) * 100, 2) as winRate
        FROM MatchStats ms 
        JOIN ms.hero h 
        WHERE (:heroName IS NULL OR h.name = :heroName)
        AND (:rank IS NULL OR ms.playerRank = :rank)
        AND (:region IS NULL OR ms.region = :region)
        AND (:gameMode IS NULL OR ms.gameMode = :gameMode)
        AND ms.matchDate >= :startDate
        GROUP BY h.name, h.role, ms.playerRank, ms.region
        HAVING COUNT(ms) >= :minMatches
        ORDER BY winRate DESC
        """)
    List<Object[]> findHeroWinRates(
            @Param("heroName") String heroName,
            @Param("rank") String rank,
            @Param("region") String region,
            @Param("gameMode") String gameMode,
            @Param("startDate") LocalDateTime startDate,
            @Param("minMatches") Long minMatches
    );

    // Hero Pick Rate Analysis - Fixed
    @Query("""
        SELECT h.name as heroName,
               h.role as heroRole,
               ms.playerRank as rank,
               COUNT(ms) as timesPlayed,
               ROUND(COUNT(ms) * 100.0 / 
                   (SELECT COUNT(ms2) FROM MatchStats ms2 
                    WHERE ms2.playerRank = ms.playerRank 
                    AND (:region IS NULL OR ms2.region = :region)
                    AND ms2.matchDate >= :startDate), 2) as pickRate
        FROM MatchStats ms 
        JOIN ms.hero h 
        WHERE (:rank IS NULL OR ms.playerRank = :rank)
        AND (:region IS NULL OR ms.region = :region)
        AND ms.matchDate >= :startDate
        GROUP BY h.name, h.role, ms.playerRank
        HAVING COUNT(ms) >= 5
        ORDER BY pickRate DESC
        """)
    List<Object[]> findHeroPickRates(
            @Param("rank") String rank,
            @Param("region") String region,
            @Param("startDate") LocalDateTime startDate
    );

    // Rank Distribution - Fixed
    @Query("""
        SELECT ms.playerRank as rank,
               COUNT(ms) as totalMatches,
               COUNT(DISTINCT h.id) as uniqueHeroes,
               ROUND(AVG(CASE WHEN ms.won = true THEN 1.0 ELSE 0.0 END) * 100, 2) as avgWinRate
        FROM MatchStats ms 
        JOIN ms.hero h
        WHERE (:region IS NULL OR ms.region = :region)
        AND ms.matchDate >= :startDate
        GROUP BY ms.playerRank
        ORDER BY 
        CASE ms.playerRank 
            WHEN 'BRONZE' THEN 1 
            WHEN 'SILVER' THEN 2 
            WHEN 'GOLD' THEN 3 
            WHEN 'PLATINUM' THEN 4 
            WHEN 'DIAMOND' THEN 5 
            WHEN 'MASTER' THEN 6 
            WHEN 'GRANDMASTER' THEN 7 
            WHEN 'CHAMPION' THEN 8 
        END
        """)
    List<Object[]> findRankDistribution(
            @Param("region") String region,
            @Param("startDate") LocalDateTime startDate
    );

    // Hero Performance Analysis - Fixed
    @Query("""
        SELECT ms.playerRank as rank,
               AVG(CAST(ms.eliminations AS DOUBLE)) as avgEliminations,
               AVG(CAST(ms.deaths AS DOUBLE)) as avgDeaths,
               AVG(CAST(ms.assists AS DOUBLE)) as avgAssists,
               AVG(CAST(ms.damage AS DOUBLE)) as avgDamage,
               AVG(CAST(ms.healing AS DOUBLE)) as avgHealing
        FROM MatchStats ms 
        WHERE ms.hero.name = :heroName
        AND (:rank IS NULL OR ms.playerRank = :rank)
        AND ms.matchDate >= :startDate
        AND ms.eliminations IS NOT NULL
        GROUP BY ms.playerRank
        ORDER BY 
        CASE ms.playerRank 
            WHEN 'BRONZE' THEN 1 
            WHEN 'SILVER' THEN 2 
            WHEN 'GOLD' THEN 3 
            WHEN 'PLATINUM' THEN 4 
            WHEN 'DIAMOND' THEN 5 
            WHEN 'MASTER' THEN 6 
            WHEN 'GRANDMASTER' THEN 7 
            WHEN 'CHAMPION' THEN 8 
        END
        """)
    List<Object[]> findHeroPerformanceByRank(
            @Param("heroName") String heroName,
            @Param("rank") String rank,
            @Param("startDate") LocalDateTime startDate
    );

    // Meta Trends Over Time - Fixed
    @Query("""
        SELECT h.name as heroName,
               h.role as heroRole,
               CAST(ms.matchDate AS DATE) as matchDay,
               COUNT(ms) as dailyPicks,
               ROUND(AVG(CASE WHEN ms.won = true THEN 1.0 ELSE 0.0 END) * 100, 2) as dailyWinRate
        FROM MatchStats ms 
        JOIN ms.hero h 
        WHERE ms.playerRank = :rank
        AND ms.matchDate >= :startDate
        GROUP BY h.name, h.role, CAST(ms.matchDate AS DATE)
        HAVING COUNT(ms) >= :minDailyPicks
        ORDER BY matchDay DESC, dailyPicks DESC
        """)
    List<Object[]> findMetaTrends(
            @Param("rank") String rank,
            @Param("startDate") LocalDateTime startDate,
            @Param("minDailyPicks") Long minDailyPicks
    );

    // Simple queries for basic stats
    List<MatchStats> findByHero_NameAndPlayerRank(String heroName, String playerRank);

    List<MatchStats> findByPlayerRankAndRegion(String playerRank, String region);

    @Query("SELECT COUNT(ms) FROM MatchStats ms WHERE ms.hero.name = :heroName")
    Long countByHeroName(@Param("heroName") String heroName);
}