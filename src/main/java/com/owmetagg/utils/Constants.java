package com.owmetagg.utils;

public class Constants {

    // Categories
    public static final String COMPETITIVE_CATEGORY = "competitive";
    public static final String QUICKPLAY_CATEGORY = "quickplay";
    public static final String OVERALL_CATEGORY = "overall";

    // Entity field constants (like your CHARACTER_NAME, DAN_RANK)
    public static final String HERO_NAME = "heroName";
    public static final String HERO_KEY = "heroKey";
    public static final String SKILL_TIER = "skillTier";
    public static final String ROLE_NAME = "roleName";

    // Rank categories
    public static final String BRONZE_RANK_CATEGORY = "bronze";
    public static final String SILVER_RANK_CATEGORY = "silver";
    public static final String GOLD_RANK_CATEGORY = "gold";
    public static final String PLATINUM_RANK_CATEGORY = "platinum";
    public static final String DIAMOND_RANK_CATEGORY = "diamond";
    public static final String MASTER_RANK_CATEGORY = "master";
    public static final String GRANDMASTER_RANK_CATEGORY = "grandmaster";
    public static final String CHAMPION_RANK_CATEGORY = "champion";
    public static final String ALL_RANKS = "allRanks";

    // Headers
    public static final String TIMESTAMP_HEADER = "fetch-timestamp";
    public static final String PLATFORM_HEADER = "platform";
    public static final String BATTLETAG_HEADER = "battletag";

    // Platforms
    public static final String PLATFORM_PC = "pc";
    public static final String PLATFORM_CONSOLE = "console";

    // Roles
    public static final String ROLE_TANK = "tank";
    public static final String ROLE_DAMAGE = "damage";
    public static final String ROLE_SUPPORT = "support";

    // Game Modes
    public static final String GAME_MODE_COMPETITIVE = "competitive";
    public static final String GAME_MODE_QUICKPLAY = "quickplay";
    public static final String GAME_MODE_ARCADE = "arcade";

    // Regions
    public static final String GLOBAL_REGION = "Global";
    public static final String REGION_US = "us";
    public static final String REGION_EU = "eu";
    public static final String REGION_ASIA = "asia";

    // Processing constants (like your CHUNK_SIZE)
    public static final int CHUNK_SIZE = 1000;
    public static final int MAX_BATTLETAG_LENGTH = 32;
    public static final String USER_ID = "userId";

    // API Operation types (like your GET_PROFILE, GET_LEADERBOARD_DATA)
    public static final String GET_PLAYER_PROFILE = "GET_PLAYER_PROFILE";
    public static final String GET_HERO_STATS = "GET_HERO_STATS";
    public static final String GET_LEADERBOARD_DATA = "GET_LEADERBOARD_DATA";
    public static final String CALCULATE_STATISTICS = "CALCULATE_STATISTICS";

    // Queue Names
    public static final String PLAYER_DATA_QUEUE = "player.data.queue";
    public static final String PLAYER_BATCH_QUEUE = "player.batch.queue";
    public static final String STATISTICS_TRIGGER_QUEUE = "statistics.trigger.queue";

    // Exchange and Routing
    public static final String OVERWATCH_EXCHANGE = "overwatch.exchange";
    public static final String PLAYER_DATA_ROUTING_KEY = "player.data";
    public static final String STATISTICS_ROUTING_KEY = "statistics.trigger";

    // Processing Status Values
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    // OverFast API Constants
    public static final int OVERFAST_TIMEOUT_MS = 30000;
    public static final int OVERFAST_DEFAULT_RATE_LIMIT = 5; // requests per second
    public static final String OVERFAST_USER_AGENT = "OverwatchStats-Backend/1.0";
    public static final String ERROR_CODE_429_MESSAGE = "OverFast API rate limit exceeded.";

    // Statistics Processing
    public static final int STATISTICS_BATCH_SIZE = 1000;
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final int STATISTICS_CALCULATION_DELAY_MS = 5000; // 5 seconds

    // Time Constants
    public static final long PLAYER_DATA_RETENTION_DAYS = 30;
    public static final long STATISTICS_REFRESH_INTERVAL_HOURS = 4;

    // Error Messages
    public static final String ERROR_PLAYER_NOT_FOUND = "Player not found in OverFast API";
    public static final String ERROR_API_TIMEOUT = "OverFast API timeout";
    public static final String ERROR_RATE_LIMIT_EXCEEDED = "OverFast API rate limit exceeded";

    private Constants() {
        // Utility class - prevent instantiation
    }
}