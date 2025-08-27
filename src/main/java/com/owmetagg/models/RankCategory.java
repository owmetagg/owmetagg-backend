package com.owmetagg.models;

public enum RankCategory {
    ALL_RANKS("All Ranks"),
    CHAMPION("Champion"),
    GRANDMASTER("Grandmaster"),
    MASTER("Master"),
    DIAMOND("Diamond"),
    PLATINUM("Platinum"),
    GOLD("Gold"),
    SILVER("Silver"),
    BRONZE("Bronze");

    String category;

    RankCategory(String category) {
        this.category = category;
    }
}
