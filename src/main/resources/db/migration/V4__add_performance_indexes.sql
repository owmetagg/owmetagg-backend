-- V4: Add performance indexes for scaling to 100k+ players
-- Note: Removing CONCURRENTLY for Flyway compatibility
-- Run manual-concurrent-indexes.sql separately for zero-downtime index creation

-- ============================================
-- PLAYERS TABLE INDEXES
-- ============================================

-- Primary lookup indexes (similar to Tekken's idx_name pattern)
CREATE INDEX IF NOT EXISTS idx_players_battletag 
    ON players(battletag);

CREATE INDEX IF NOT EXISTS idx_players_platform 
    ON players(platform);

-- Time-based queries for recent activity
CREATE INDEX IF NOT EXISTS idx_players_last_updated 
    ON players(last_updated DESC);

-- Skill rating queries for rank distribution
CREATE INDEX IF NOT EXISTS idx_players_skill_rating 
    ON players(skill_rating DESC) 
    WHERE skill_rating IS NOT NULL;

-- Composite index for platform-specific leaderboards
CREATE INDEX IF NOT EXISTS idx_players_platform_sr 
    ON players(platform, skill_rating DESC) 
    WHERE skill_rating IS NOT NULL;

-- Region-based queries
CREATE INDEX IF NOT EXISTS idx_players_region 
    ON players(region) 
    WHERE region IS NOT NULL;

-- ============================================
-- HERO_STATS TABLE INDEXES
-- ============================================

-- Primary lookup indexes (similar to Tekken's character_stats pattern)
CREATE INDEX IF NOT EXISTS idx_hero_stats_player_id 
    ON hero_stats(player_id);

CREATE INDEX IF NOT EXISTS idx_hero_stats_hero_key 
    ON hero_stats(hero_key);

CREATE INDEX IF NOT EXISTS idx_hero_stats_game_mode 
    ON hero_stats(game_mode);

-- Time-based analysis
CREATE INDEX IF NOT EXISTS idx_hero_stats_last_played 
    ON hero_stats(last_played DESC);

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_hero_stats_player_game_mode 
    ON hero_stats(player_id, game_mode);

CREATE INDEX IF NOT EXISTS idx_hero_stats_hero_game_mode 
    ON hero_stats(hero_key, game_mode);

-- Performance metrics for statistics calculations
CREATE INDEX IF NOT EXISTS idx_hero_stats_wins_losses 
    ON hero_stats(wins DESC, losses DESC) 
    WHERE time_played > 0;

-- Active players index (players with significant playtime)
CREATE INDEX IF NOT EXISTS idx_hero_stats_time_played 
    ON hero_stats(time_played DESC) 
    WHERE time_played > 0;

-- Skill tier analysis
CREATE INDEX IF NOT EXISTS idx_hero_stats_skill_tier 
    ON hero_stats(skill_tier DESC) 
    WHERE skill_tier > 0;

-- Composite index for hero performance by skill tier
CREATE INDEX IF NOT EXISTS idx_hero_stats_hero_skill_tier 
    ON hero_stats(hero_key, skill_tier DESC, game_mode);

-- ============================================
-- HERO_STATISTICS TABLE INDEXES (Aggregated)
-- ============================================

-- Enhance existing indexes from V3
CREATE INDEX IF NOT EXISTS idx_hero_statistics_hero_key 
    ON hero_statistics(hero_key);

CREATE INDEX IF NOT EXISTS idx_hero_statistics_total_games 
    ON hero_statistics(total_games_played DESC) 
    WHERE total_games_played > 0;

-- Composite index for meta analysis
CREATE INDEX IF NOT EXISTS idx_hero_statistics_meta 
    ON hero_statistics(game_mode, pick_rate DESC, win_rate DESC);

-- ============================================
-- RANK_DISTRIBUTION TABLE INDEXES
-- ============================================

-- Additional index for percentage queries
CREATE INDEX IF NOT EXISTS idx_rank_distribution_percentage 
    ON rank_distribution(percentage DESC);

-- Composite index for time-series rank analysis
CREATE INDEX IF NOT EXISTS idx_rank_distribution_time_series 
    ON rank_distribution(snapshot_date DESC, sr_bracket);

-- ============================================
-- HERO_TRENDS TABLE INDEXES
-- ============================================

-- Composite index for efficient trend queries
CREATE INDEX IF NOT EXISTS idx_hero_trends_composite 
    ON hero_trends(hero_key, game_mode, trend_date DESC);

-- Index for finding trending heroes
CREATE INDEX IF NOT EXISTS idx_hero_trends_popularity 
    ON hero_trends(trend_date DESC, pick_rate DESC) 
    WHERE games_played > 0;

-- ============================================
-- ROLE_STATISTICS TABLE INDEXES
-- ============================================

-- Composite index for role meta analysis
CREATE INDEX IF NOT EXISTS idx_role_statistics_meta 
    ON role_statistics(game_mode, avg_pick_rate DESC);

-- ============================================
-- FOREIGN KEY CONSTRAINTS
-- ============================================

-- Add foreign key constraint if it doesn't exist
-- Note: This requires checking if constraint exists first
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_hero_stats_player'
    ) THEN
        ALTER TABLE hero_stats 
        ADD CONSTRAINT fk_hero_stats_player 
        FOREIGN KEY (player_id) 
        REFERENCES players(player_id) 
        ON DELETE CASCADE;
    END IF;
END $$;

-- ============================================
-- ANALYZE TABLES FOR QUERY PLANNER
-- ============================================

-- Update statistics for query planner optimization
ANALYZE players;
ANALYZE hero_stats;
ANALYZE hero_statistics;
ANALYZE rank_distribution;
ANALYZE hero_trends;
ANALYZE role_statistics;

-- ============================================
-- INDEX USAGE MONITORING QUERY (Comment)
-- ============================================

-- To monitor index usage after deployment, run:
-- SELECT 
--     schemaname,
--     tablename,
--     indexname,
--     idx_scan as index_scans,
--     idx_tup_read as tuples_read,
--     idx_tup_fetch as tuples_fetched,
--     pg_size_pretty(pg_relation_size(indexrelid)) as index_size
-- FROM pg_stat_user_indexes
-- WHERE schemaname = 'public'
-- ORDER BY idx_scan DESC;