package com.owmetagg.repositories;

import com.owmetagg.models.Player;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, String> {

    // Find player by battletag and platform (primary lookup method)
    @Query("SELECT p FROM Player p WHERE p.battletag = :battletag AND p.platform = :platform")
    Optional<Player> findByBattletagAndPlatform(@Param("battletag") String battletag, @Param("platform") String platform);

    // Find player by battletag only (any platform)
    @Query("SELECT p FROM Player p WHERE p.battletag = :battletag")
    List<Player> findByBattletag(@Param("battletag") String battletag);

    // Sophisticated search query (adapted from your Tekken pattern)
    @Query("SELECT p FROM Player p WHERE " +
            "(LOWER(p.battletag) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.username) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.battletag) LIKE LOWER(CONCAT('%', REPLACE(:query, '#', ''), '%'))) " +
            "ORDER BY CASE " +
            "  WHEN LOWER(p.battletag) = LOWER(:query) THEN 0 " +
            "  WHEN LOWER(p.username) = LOWER(:query) THEN 1 " +
            "  WHEN LOWER(p.battletag) LIKE LOWER(CONCAT(:query, '%')) THEN 2 " +
            "  WHEN LOWER(p.username) LIKE LOWER(CONCAT(:query, '%')) THEN 3 " +
            "  ELSE 20 END, " +
            "LENGTH(p.battletag)")
    Optional<List<Player>> findByBattletagOrUsernameContainingIgnoreCase(@Param("query") String query, PageRequest pageRequest);

    // Find recently active players (adapted from your 10 minutes query)
    @Query(value = "SELECT * FROM players " +
            "WHERE last_updated > (NOW() - INTERVAL '10 minutes') " +
            "ORDER BY last_updated DESC LIMIT 40", nativeQuery = true)
    Optional<List<Player>> findAllActivePlayersInLast10Minutes();

    // Find recently active players with custom time window
    @Query(value = "SELECT * FROM players " +
            "WHERE last_updated > (NOW() - INTERVAL ':hours hours') " +
            "ORDER BY last_updated DESC LIMIT :limit", nativeQuery = true)
    Optional<List<Player>> findAllActivePlayersInLastHours(@Param("hours") int hours, @Param("limit") int limit);

    // Find player ID by battletag (utility method like your polarisId lookup)
    @Query("SELECT p.playerId FROM Player p WHERE p.battletag = :battletag AND p.platform = :platform")
    Optional<String> findPlayerIdByBattletagAndPlatform(@Param("battletag") String battletag, @Param("platform") String platform);

    // Find players by region with activity filter
    @Query("SELECT p FROM Player p WHERE p.region = :region " +
            "AND p.lastUpdated > :since ORDER BY p.lastUpdated DESC")
    List<Player> findActivePlayersByRegion(@Param("region") String region, @Param("since") LocalDateTime since);

    // Find players by skill rating range (for leaderboards)
    @Query("SELECT p FROM Player p WHERE p.skillRating BETWEEN :minRating AND :maxRating " +
            "AND p.platform = :platform ORDER BY p.skillRating DESC")
    List<Player> findPlayersBySkillRatingRange(@Param("minRating") Integer minRating,
                                               @Param("maxRating") Integer maxRating,
                                               @Param("platform") String platform,
                                               PageRequest pageRequest);

    // Top players by skill rating (for leaderboards)
    @Query("SELECT p FROM Player p WHERE p.skillRating IS NOT NULL " +
            "AND p.platform = :platform " +
            "ORDER BY p.skillRating DESC")
    List<Player> findTopPlayersBySkillRating(@Param("platform") String platform, PageRequest pageRequest);

    // Find players who need data refresh (haven't been updated recently)
    @Query("SELECT p FROM Player p WHERE p.lastUpdated < :threshold ORDER BY p.lastUpdated ASC")
    List<Player> findPlayersNeedingRefresh(@Param("threshold") LocalDateTime threshold, PageRequest pageRequest);

    // Count players by platform and region
    @Query("SELECT COUNT(p) FROM Player p WHERE p.platform = :platform AND p.region = :region")
    long countPlayersByPlatformAndRegion(@Param("platform") String platform, @Param("region") String region);

    // Find duplicate battletags across platforms (data quality check)
    @Query("SELECT p.battletag FROM Player p GROUP BY p.battletag HAVING COUNT(p) > 1")
    List<String> findDuplicateBattletags();

    // Statistical queries for dashboard
    @Query("SELECT COUNT(p) FROM Player p WHERE p.lastUpdated > :since")
    long countRecentlyActivePlayers(@Param("since") LocalDateTime since);

    @Query("SELECT AVG(p.skillRating) FROM Player p WHERE p.skillRating IS NOT NULL AND p.platform = :platform")
    Double getAverageSkillRatingByPlatform(@Param("platform") String platform);

    @Query("SELECT p.region, COUNT(p) FROM Player p GROUP BY p.region")
    List<Object[]> getPlayerCountsByRegion();

    // Batch operations support
    @Query("SELECT p FROM Player p WHERE p.playerId IN :playerIds")
    List<Player> findPlayersByIds(@Param("playerIds") List<String> playerIds);
}