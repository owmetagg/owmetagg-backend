-- Hero Statistics Table (Aggregated stats across all players)
CREATE TABLE IF NOT EXISTS hero_statistics (
    hero_key VARCHAR(50) NOT NULL,
    game_mode VARCHAR(20) NOT NULL,
    total_games_played BIGINT DEFAULT 0,
    total_wins BIGINT DEFAULT 0,
    total_losses BIGINT DEFAULT 0,
    pick_count INTEGER DEFAULT 0,
    pick_rate DECIMAL(5,2) DEFAULT 0.00,
    win_rate DECIMAL(5,2) DEFAULT 0.00,
    avg_eliminations DECIMAL(10,2) DEFAULT 0.00,
    avg_deaths DECIMAL(10,2) DEFAULT 0.00,
    avg_assists DECIMAL(10,2) DEFAULT 0.00,
    avg_kda DECIMAL(10,2) DEFAULT 0.00,
    last_calculated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (hero_key, game_mode)
);

-- Rank Distribution Table
CREATE TABLE IF NOT EXISTS rank_distribution (
    sr_bracket INTEGER NOT NULL,
    bracket_name VARCHAR(50) NOT NULL,
    player_count INTEGER DEFAULT 0,
    percentage DECIMAL(5,2) DEFAULT 0.00,
    snapshot_date DATE NOT NULL,
    PRIMARY KEY (sr_bracket, snapshot_date)
);

-- Hero Trends Table (Daily snapshots for trend analysis)
CREATE TABLE IF NOT EXISTS hero_trends (
    hero_key VARCHAR(50) NOT NULL,
    trend_date DATE NOT NULL,
    game_mode VARCHAR(20) NOT NULL,
    pick_rate DECIMAL(5,2) DEFAULT 0.00,
    win_rate DECIMAL(5,2) DEFAULT 0.00,
    games_played BIGINT DEFAULT 0,
    avg_kda DECIMAL(10,2) DEFAULT 0.00,
    PRIMARY KEY (hero_key, trend_date, game_mode)
);

-- Role Statistics Table
CREATE TABLE IF NOT EXISTS role_statistics (
    role VARCHAR(20) NOT NULL,
    game_mode VARCHAR(20) NOT NULL,
    avg_win_rate DECIMAL(5,2) DEFAULT 0.00,
    avg_pick_rate DECIMAL(5,2) DEFAULT 0.00,
    avg_kda DECIMAL(10,2) DEFAULT 0.00,
    total_players INTEGER DEFAULT 0,
    last_calculated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role, game_mode)
);

-- Create indexes for better query performance
CREATE INDEX idx_hero_statistics_win_rate ON hero_statistics(win_rate DESC);
CREATE INDEX idx_hero_statistics_pick_rate ON hero_statistics(pick_rate DESC);
CREATE INDEX idx_hero_statistics_game_mode ON hero_statistics(game_mode);

CREATE INDEX idx_rank_distribution_snapshot ON rank_distribution(snapshot_date DESC);
CREATE INDEX idx_rank_distribution_bracket ON rank_distribution(sr_bracket);

CREATE INDEX idx_hero_trends_date ON hero_trends(trend_date DESC);
CREATE INDEX idx_hero_trends_hero ON hero_trends(hero_key);

CREATE INDEX idx_role_statistics_role ON role_statistics(role);