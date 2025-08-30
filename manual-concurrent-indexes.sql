-- ============================================
-- CONCURRENT INDEX CREATION FOR PRODUCTION
-- ============================================
-- Run this script manually when you need zero-downtime index creation
-- This is for production environments where you can't afford table locks
-- 
-- USAGE:
-- psql -U postgres -d owmetagg-database -h localhost -p 5432 -f manual-concurrent-indexes.sql
--
-- NOTE: Each CREATE INDEX CONCURRENTLY must be run outside a transaction
-- ============================================

\echo 'Creating concurrent indexes for production environment...'
\echo 'This may take a while but won't lock tables...'

-- PLAYERS TABLE
\echo 'Creating indexes on players table...'
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_players_battletag_concurrent ON players(battletag);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_players_platform_concurrent ON players(platform);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_players_last_updated_concurrent ON players(last_updated DESC);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_players_skill_rating_concurrent ON players(skill_rating DESC) WHERE skill_rating IS NOT NULL;

-- HERO_STATS TABLE
\echo 'Creating indexes on hero_stats table...'
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_hero_stats_player_id_concurrent ON hero_stats(player_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_hero_stats_hero_key_concurrent ON hero_stats(hero_key);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_hero_stats_game_mode_concurrent ON hero_stats(game_mode);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_hero_stats_last_played_concurrent ON hero_stats(last_played DESC);

-- Drop old indexes and rename concurrent ones (if needed)
\echo 'Swapping indexes (if applicable)...'
-- Example:
-- DROP INDEX IF EXISTS idx_players_battletag;
-- ALTER INDEX idx_players_battletag_concurrent RENAME TO idx_players_battletag;

\echo 'Concurrent index creation complete!'