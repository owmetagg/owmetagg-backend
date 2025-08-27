# Statistics Pipeline Testing Guide

## Prerequisites
1. Start your PostgreSQL Docker container
2. Start the Spring Boot application: `./mvnw spring-boot:run`
3. Ensure you have some player data in your database

## 1. Database Schema Verification

Connect to your PostgreSQL database and run:

```sql
-- Check if statistics tables exist
SELECT table_name 
FROM information_schema.tables 
WHERE table_schema = 'public' 
AND table_name IN ('hero_statistics', 'rank_distribution', 'hero_trends', 'role_statistics');

-- Check current data counts
SELECT 
  (SELECT COUNT(*) FROM players) as players,
  (SELECT COUNT(*) FROM hero_stats) as hero_stats,
  (SELECT COUNT(*) FROM hero_statistics) as hero_statistics,
  (SELECT COUNT(*) FROM rank_distribution) as rank_distribution;
```

## 2. Manual Statistics Calculation Test

```bash
# Trigger manual recalculation
curl -X POST http://localhost:8080/api/statistics/recalculate \
  -H "Content-Type: application/json"

# Should return: {"status":"success","message":"Statistics recalculation completed successfully"}
```

## 3. API Endpoint Tests

### Top Heroes by Pick Rate
```bash
curl "http://localhost:8080/api/statistics/heroes/top?gameMode=competitive&sortBy=pickrate&limit=10"
```

### Hero Win Rates
```bash
curl "http://localhost:8080/api/statistics/heroes/winrates?gameMode=competitive&minGames=5"
```

### Rank Distribution
```bash
curl "http://localhost:8080/api/statistics/rank-distribution"
```

### Meta Report (comprehensive overview)
```bash
curl "http://localhost:8080/api/statistics/meta-report?gameMode=competitive"
```

### Hero Trends (if you have historical data)
```bash
curl "http://localhost:8080/api/statistics/trends/hero/genji?gameMode=competitive&days=7"
```

### Role Statistics
```bash
curl "http://localhost:8080/api/statistics/roles?gameMode=competitive"
```

## 4. Event-Driven Statistics Test

To test if statistics automatically recalculate when player data is processed:

```bash
# Add a new player (this should trigger statistics recalculation)
curl -X POST "http://localhost:8080/api/players/fetch" \
  -H "Content-Type: application/json" \
  -d '{"battletag":"super#12850","platform":"pc"}'

# Wait 2-3 seconds, then check if statistics updated
curl "http://localhost:8080/api/statistics/heroes/top?gameMode=competitive&limit=5"
```

## 5. Database Verification Queries

After running tests, verify data was created:

```sql
-- Check hero statistics were calculated
SELECT hero_key, game_mode, pick_rate, win_rate, total_games_played 
FROM hero_statistics 
WHERE total_games_played > 0 
ORDER BY pick_rate DESC LIMIT 10;

-- Check rank distribution
SELECT bracket_name, player_count, percentage 
FROM rank_distribution 
WHERE snapshot_date = CURRENT_DATE 
ORDER BY sr_bracket;

-- Check if trends are being tracked
SELECT hero_key, trend_date, pick_rate, win_rate 
FROM hero_trends 
ORDER BY trend_date DESC LIMIT 10;

-- Check role statistics
SELECT role, game_mode, avg_win_rate, total_players 
FROM role_statistics 
ORDER BY role;
```

## 6. Performance Test

Test caching is working:

```bash
# First call (should be slower)
time curl "http://localhost:8080/api/statistics/heroes/top?gameMode=competitive&limit=10"

# Second call (should be faster due to cache)
time curl "http://localhost:8080/api/statistics/heroes/top?gameMode=competitive&limit=10"
```

## Expected Results

✅ **Success Indicators:**
- All 4 statistics tables exist
- Manual recalculation returns success message
- API endpoints return JSON data with hero statistics
- Statistics automatically update when new player data is added
- Database contains calculated statistics data
- Cached responses are faster on subsequent calls

❌ **Failure Indicators:**
- 500 errors from API endpoints
- Empty statistics tables after recalculation
- Missing statistics tables
- Statistics don't update after adding players

## Troubleshooting

If tests fail, check:
1. Application logs for errors: `docker logs <spring-boot-container>`
2. PostgreSQL logs: `docker logs <postgres-container>`
3. Verify player and hero_stats data exists before calculating statistics
4. Check if Flyway migrations ran successfully in application logs