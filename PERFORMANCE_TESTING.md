# Performance Testing Guide

## Testing Order - Before Scaling to 100k Players

### Phase 1: Index Verification (Do First!)
Run these tests with your current ~4 players to ensure indexes work correctly.

#### 1. Apply the Migration
```bash
# Start your application - Flyway will auto-apply V4 migration
./mvnw spring-boot:run

# Check logs for successful migration
# You should see: "Successfully applied 1 migration"
```

#### 2. Verify Indexes Exist
```bash
# Connect to PostgreSQL
psql -U postgres -d owmetagg-database-dev -h localhost -p 5433

# Run verification query
\di+ *idx*
```

#### 3. Run Performance Tests
```bash
# Execute the test script
psql -U postgres -d owmetagg-database-dev -h localhost -p 5433 -f test-index-performance.sql
```

#### What to Look For:
✅ **Good Signs:**
- `Index Scan` or `Index Only Scan` in EXPLAIN output
- Query execution times < 10ms for lookups
- No `Seq Scan` on large tables

❌ **Bad Signs:**
- `Seq Scan` in EXPLAIN output
- High execution times (> 100ms for simple queries)
- Indexes not being used (idx_scan = 0)

### Phase 2: Load Testing (With Current Data)

#### 1. JMeter Test (Concurrent Requests)
```bash
# Test endpoint performance with current data
curl -w "@curl-format.txt" -o /dev/null -s \
  "http://localhost:8080/api/statistics/heroes/top?gameMode=competitive&limit=10"

# Run 100 concurrent requests
for i in {1..100}; do
  curl "http://localhost:8080/api/statistics/heroes/top?gameMode=competitive&limit=10" &
done
wait
```

#### 2. Database Connection Pool Testing
```sql
-- Monitor active connections
SELECT count(*) FROM pg_stat_activity WHERE datname = 'owmetagg-database-dev';

-- Check for connection pool efficiency
SELECT state, count(*) 
FROM pg_stat_activity 
WHERE datname = 'owmetagg-database-dev' 
GROUP BY state;
```

### Phase 3: Gradual Scaling Test

Before jumping to 100k players, test incrementally:

#### Step 1: Add 10 Players
```bash
# Create a test script to add players
curl -X POST "http://localhost:8080/api/players/fetch" \
  -H "Content-Type: application/json" \
  -d '{"battletag":"player1#1234","platform":"pc"}'
```

#### Step 2: Monitor Performance
```sql
-- Check query performance as data grows
EXPLAIN ANALYZE 
SELECT * FROM hero_statistics 
WHERE game_mode = 'competitive' 
ORDER BY pick_rate DESC;

-- Monitor table sizes
SELECT 
    relname as table_name,
    pg_size_pretty(pg_total_relation_size(relid)) as total_size
FROM pg_stat_user_tables
ORDER BY pg_total_relation_size(relid) DESC;
```

#### Step 3: Add 100 Players
- Repeat the process with 100 players
- Monitor response times
- Check if indexes are still being used

#### Step 4: Add 1000 Players
- Use batch import if available
- Monitor database metrics
- Check application logs for timeouts

## When to Scale to 100k Players

### ✅ **Ready to Scale When:**

1. **All indexes are being used:**
   ```sql
   SELECT indexname, idx_scan 
   FROM pg_stat_user_indexes 
   WHERE idx_scan > 0;
   ```

2. **Response times are acceptable:**
   - API endpoints < 100ms
   - Database queries < 50ms
   - No timeout errors

3. **Connection pool is stable:**
   - No connection exhaustion
   - Pool size handles concurrent load

4. **Memory usage is reasonable:**
   ```bash
   # Check PostgreSQL memory
   ps aux | grep postgres
   ```

### ❌ **Not Ready If:**

1. Sequential scans on large tables
2. API timeouts occurring
3. Connection pool exhaustion
4. Memory/CPU at limits with small dataset

## Scaling Strategy

### Option 1: Gradual Real Data
- Add 10 real players per day
- Monitor performance daily
- Adjust indexes as needed

### Option 2: Synthetic Test Data
```sql
-- Generate test players
INSERT INTO players (player_id, battletag, platform, skill_rating, last_updated)
SELECT 
    'test_' || generate_series as player_id,
    'TestPlayer' || generate_series || '#' || (1000 + generate_series) as battletag,
    CASE WHEN random() > 0.5 THEN 'pc' ELSE 'console' END as platform,
    1500 + (random() * 3000)::int as skill_rating,
    NOW() - (random() * interval '30 days') as last_updated
FROM generate_series(1, 1000);

-- Generate test hero stats
INSERT INTO hero_stats (player_id, hero_key, game_mode, wins, losses, time_played)
SELECT 
    p.player_id,
    h.hero,
    CASE WHEN random() > 0.5 THEN 'competitive' ELSE 'quickplay' END,
    (random() * 100)::int,
    (random() * 100)::int,
    (random() * 1000)::int
FROM players p
CROSS JOIN (
    SELECT unnest(ARRAY['genji', 'mercy', 'reinhardt', 'widowmaker', 'dva']) as hero
) h
WHERE p.player_id LIKE 'test_%';
```

### Option 3: Use Production Database Clone
- Best for realistic testing
- Clone your future production database
- Test with actual data patterns

## Monitoring Commands

```bash
# Watch query performance in real-time
watch -n 1 "psql -U postgres -d owmetagg-database-dev -c \"SELECT query, calls, mean_time FROM pg_stat_statements WHERE query LIKE '%hero%' ORDER BY mean_time DESC LIMIT 5;\""

# Monitor table growth
watch -n 5 "psql -U postgres -d owmetagg-database-dev -c \"SELECT relname, n_live_tup FROM pg_stat_user_tables ORDER BY n_live_tup DESC LIMIT 10;\""

# Check cache hit ratio (should be > 95%)
psql -c "SELECT sum(heap_blks_hit) / nullif(sum(heap_blks_hit) + sum(heap_blks_read), 0) * 100 AS cache_hit_ratio FROM pg_statio_user_tables;"
```

## Next Steps After Testing

1. **If performance is good:** Start scaling gradually
2. **If performance issues:** 
   - Review slow query log
   - Add missing indexes
   - Optimize queries
   - Consider partitioning for hero_stats table
3. **Move to managed database** when ready for production scale