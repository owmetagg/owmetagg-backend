# Quick Start: Testing Database Performance

## Step 1: Start Spring Boot (This will apply indexes)
```bash
./mvnw spring-boot:run
```

✅ **Success:** You should see "Successfully validated 4 migrations" in logs
❌ **If error:** Check that PostgreSQL is running on port 5433

## Step 2: Connect to Database
```bash
# Option A: Direct command with password prompt
psql -U postgres -d owmetagg-database-dev -h localhost -p 5433

# Option B: With password inline (less secure)
PGPASSWORD=guest psql -U postgres -d owmetagg-database-dev -h localhost -p 5433
```

## Step 3: Check Indexes Were Created
Once connected to psql, run:
```sql
-- List all indexes
\di+ *idx*

-- Or check specific table
\d players
\d hero_stats
```

## Step 4: Run Performance Tests

### Method 1: From Outside psql
```bash
# Make sure you're in the project directory
PGPASSWORD=guest psql -U postgres -d owmetagg-database-dev -h localhost -p 5433 -f test-index-performance.sql
```

### Method 2: From Inside psql
```sql
-- First connect (as shown in Step 2)
-- Then run:
\i test-index-performance.sql
```

## Step 5: Quick Performance Check
```sql
-- This should use index and be FAST (< 10ms)
EXPLAIN ANALYZE 
SELECT * FROM players WHERE battletag = 'super#12850';

-- Look for "Index Scan" in the output
-- Execution time should be < 10ms
```

## What Good Results Look Like

### ✅ GOOD - Index Being Used:
```
Index Scan using idx_players_battletag on players
  Index Cond: ((battletag)::text = 'super#12850'::text)
Planning Time: 0.123 ms
Execution Time: 0.045 ms  ← Fast!
```

### ❌ BAD - Sequential Scan:
```
Seq Scan on players
  Filter: ((battletag)::text = 'super#12850'::text)
Planning Time: 0.100 ms
Execution Time: 15.234 ms  ← Slow!
```

## Common Issues & Fixes

### Issue: "psql: command not found"
```bash
# Mac: Install with Homebrew
brew install postgresql

# Or use Docker
docker exec -it postgres-container psql -U postgres -d owmetagg-database-dev
```

### Issue: "password authentication failed"
```bash
# Check your docker-compose.yml for correct password
# Default is: guest
```

### Issue: "could not connect to server"
```bash
# Check PostgreSQL is running
docker ps | grep postgres

# If not running, start it
docker-compose up -d postgres
```

### Issue: Indexes not showing up
```sql
-- Force analyze to update statistics
ANALYZE players;
ANALYZE hero_stats;

-- Then check again
\di+ *idx*
```

## Next Steps
- If all tests pass → Ready to scale!
- If tests fail → Check PERFORMANCE_TESTING.md for detailed troubleshooting
- Add more players gradually and monitor performance