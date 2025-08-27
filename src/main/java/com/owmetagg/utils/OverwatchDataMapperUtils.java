package com.owmetagg.utils;

import java.util.HashMap;
import java.util.Map;

public class OverwatchDataMapperUtils {

    private static final Map<String, String> heroMap = new HashMap<>();
    private static final Map<String, String> mapMap = new HashMap<>();
    private static final Map<String, String> skillTierMap = new HashMap<>();
    private static final Map<String, String> roleMap = new HashMap<>();
    private static final Map<String, String> gameModeMap = new HashMap<>();

    static {
        // Hero mappings (hero key -> hero name)
        // Tank Heroes
        heroMap.put("dva", "D.Va");
        heroMap.put("doomfist", "Doomfist");
        heroMap.put("hazard", "Hazard");
        heroMap.put("junker-queen", "Junker Queen");
        heroMap.put("mauga", "Mauga");
        heroMap.put("orisa", "Orisa");
        heroMap.put("ramattra", "Ramattra");
        heroMap.put("reinhardt", "Reinhardt");
        heroMap.put("roadhog", "Roadhog");
        heroMap.put("sigma", "Sigma");
        heroMap.put("winston", "Winston");
        heroMap.put("wrecking-ball", "Wrecking Ball");
        heroMap.put("zarya", "Zarya");

        // Damage Heroes
        heroMap.put("ashe", "Ashe");
        heroMap.put("bastion", "Bastion");
        heroMap.put("cassidy", "Cassidy");
        heroMap.put("echo", "Echo");
        heroMap.put("freja", "Freja");
        heroMap.put("genji", "Genji");
        heroMap.put("hanzo", "Hanzo");
        heroMap.put("junkrat", "Junkrat");
        heroMap.put("mei", "Mei");
        heroMap.put("pharah", "Pharah");
        heroMap.put("reaper", "Reaper");
        heroMap.put("sojourn", "Sojourn");
        heroMap.put("soldier-76", "Soldier: 76");
        heroMap.put("sombra", "Sombra");
        heroMap.put("symmetra", "Symmetra");
        heroMap.put("torbjorn", "Torbjörn");
        heroMap.put("tracer", "Tracer");
        heroMap.put("widowmaker", "Widowmaker");
        heroMap.put("venture", "Venture");

        // Support Heroes
        heroMap.put("ana", "Ana");
        heroMap.put("baptiste", "Baptiste");
        heroMap.put("brigitte", "Brigitte");
        heroMap.put("kiriko", "Kiriko");
        heroMap.put("lifeweaver", "Lifeweaver");
        heroMap.put("lucio", "Lúcio");
        heroMap.put("mercy", "Mercy");
        heroMap.put("moira", "Moira");
        heroMap.put("zenyatta", "Zenyatta");
        heroMap.put("illari", "Illari");
        heroMap.put("juno", "Juno");

        // Map mappings (map key -> map name)
        mapMap.put("hanamura", "Hanamura");
        mapMap.put("temple-of-anubis", "Temple of Anubis");
        mapMap.put("volskaya-industries", "Volskaya Industries");
        mapMap.put("dorado", "Dorado");
        mapMap.put("havana", "Havana");
        mapMap.put("junkertown", "Junkertown");
        mapMap.put("rialto", "Rialto");
        mapMap.put("route-66", "Route 66");
        mapMap.put("watchpoint-gibraltar", "Watchpoint: Gibraltar");
        mapMap.put("blizzard-world", "Blizzard World");
        mapMap.put("eichenwalde", "Eichenwalde");
        mapMap.put("hollywood", "Hollywood");
        mapMap.put("kings-row", "King's Row");
        mapMap.put("numbani", "Numbani");
        mapMap.put("busan", "Busan");
        mapMap.put("ilios", "Ilios");
        mapMap.put("lijiang-tower", "Lijiang Tower");
        mapMap.put("nepal", "Nepal");
        mapMap.put("oasis", "Oasis");
        mapMap.put("colosseo", "Colosseo");
        mapMap.put("esperanca", "Esperança");
        mapMap.put("new-queen-street", "New Queen Street");
        mapMap.put("paraiso", "Paraíso");
        mapMap.put("circuit-royal", "Circuit Royal");
        mapMap.put("midtown", "Midtown");
        mapMap.put("suravasa", "Suravasa");
        mapMap.put("aatlis", "Aatlis");

        // Skill Tier mappings
        skillTierMap.put("0", "Unranked");
        skillTierMap.put("1", "Bronze 5");
        skillTierMap.put("2", "Bronze 4");
        skillTierMap.put("3", "Bronze 3");
        skillTierMap.put("4", "Bronze 2");
        skillTierMap.put("5", "Bronze 1");
        skillTierMap.put("6", "Silver 5");
        skillTierMap.put("7", "Silver 4");
        skillTierMap.put("8", "Silver 3");
        skillTierMap.put("9", "Silver 2");
        skillTierMap.put("10", "Silver 1");
        skillTierMap.put("11", "Gold 5");
        skillTierMap.put("12", "Gold 4");
        skillTierMap.put("13", "Gold 3");
        skillTierMap.put("14", "Gold 2");
        skillTierMap.put("15", "Gold 1");
        skillTierMap.put("16", "Platinum 5");
        skillTierMap.put("17", "Platinum 4");
        skillTierMap.put("18", "Platinum 3");
        skillTierMap.put("19", "Platinum 2");
        skillTierMap.put("20", "Platinum 1");
        skillTierMap.put("21", "Diamond 5");
        skillTierMap.put("22", "Diamond 4");
        skillTierMap.put("23", "Diamond 3");
        skillTierMap.put("24", "Diamond 2");
        skillTierMap.put("25", "Diamond 1");
        skillTierMap.put("26", "Master 5");
        skillTierMap.put("27", "Master 4");
        skillTierMap.put("28", "Master 3");
        skillTierMap.put("29", "Master 2");
        skillTierMap.put("30", "Master 1");
        skillTierMap.put("31", "Grandmaster 5");
        skillTierMap.put("32", "Grandmaster 4");
        skillTierMap.put("33", "Grandmaster 3");
        skillTierMap.put("34", "Grandmaster 2");
        skillTierMap.put("35", "Grandmaster 1");
        skillTierMap.put("36", "Champion");

        // Role mappings
        roleMap.put("tank", "Tank");
        roleMap.put("damage", "Damage");
        roleMap.put("support", "Support");

        // Game Mode mappings
        gameModeMap.put("competitive", "Competitive");
        gameModeMap.put("quickplay", "Quick Play");
        gameModeMap.put("stadium", "Stadium");
        gameModeMap.put("arcade", "Arcade");
        gameModeMap.put("mystery-heroes", "Mystery Heroes");
        gameModeMap.put("total-mayhem", "Total Mayhem");

    }

    public static String getHeroName(String heroKey) {
        return heroMap.getOrDefault(heroKey, "Undefined Hero: " + heroKey);
    }

    public static String getMapName(String mapKey) {
        return mapMap.getOrDefault(mapKey, "Undefined Map: " + mapKey);
    }

    public static String getSkillTierName(String skillTierId) {
        return skillTierMap.getOrDefault(skillTierId, "Undefined Skill Tier: " + skillTierId);
    }

    public static String getRoleName(String roleKey) {
        return roleMap.getOrDefault(roleKey, "Undefined Role: " + roleKey);
    }

    public static String getGameModeName(String gameModeKey) {
        return gameModeMap.getOrDefault(gameModeKey, "Undefined Game Mode: " + gameModeKey);
    }

    // Utility method to get hero role from hero key
    public static String getHeroRole(String heroKey) {
        // Tank heroes
        if (heroKey.matches("dva|doomfist|hazard|junker-queen|orisa|ramattra|reinhardt|roadhog|sigma|winston|wrecking-ball|zarya|mauga")) {
            return "tank";
        }
        // Support heroes
        if (heroKey.matches("ana|baptiste|brigitte|kiriko|lifeweaver|lucio|mercy|moira|zenyatta|illari|juno")) {
            return "support";
        }
        // Default to damage
        return "damage";
    }

    // Utility method to convert skill rating to tier (approximate)
    public static String skillRatingToTier(int skillRating) {
        if (skillRating == 0) return "0";
        if (skillRating < 1500) return "1"; // Bronze 5
        if (skillRating < 1600) return "2"; // Bronze 4
        if (skillRating < 1700) return "3"; // Bronze 3
        if (skillRating < 1800) return "4"; // Bronze 2
        if (skillRating < 1900) return "5"; // Bronze 1
        if (skillRating < 2000) return "6"; // Silver 5
        if (skillRating < 2100) return "7"; // Silver 4
        if (skillRating < 2200) return "8"; // Silver 3
        if (skillRating < 2300) return "9"; // Silver 2
        if (skillRating < 2400) return "10"; // Silver 1
        if (skillRating < 2500) return "11"; // Gold 5
        if (skillRating < 2600) return "12"; // Gold 4
        if (skillRating < 2700) return "13"; // Gold 3
        if (skillRating < 2800) return "14"; // Gold 2
        if (skillRating < 2900) return "15"; // Gold 1
        if (skillRating < 3000) return "16"; // Platinum 5
        if (skillRating < 3100) return "17"; // Platinum 4
        if (skillRating < 3200) return "18"; // Platinum 3
        if (skillRating < 3300) return "19"; // Platinum 2
        if (skillRating < 3400) return "20"; // Platinum 1
        if (skillRating < 3500) return "21"; // Diamond 5
        if (skillRating < 3600) return "22"; // Diamond 4
        if (skillRating < 3700) return "23"; // Diamond 3
        if (skillRating < 3800) return "24"; // Diamond 2
        if (skillRating < 3900) return "25"; // Diamond 1
        if (skillRating < 4000) return "26"; // Master 5
        if (skillRating < 4100) return "27"; // Master 4
        if (skillRating < 4200) return "28"; // Master 3
        if (skillRating < 4300) return "29"; // Master 2
        if (skillRating < 4400) return "30"; // Master 1
        if (skillRating < 4500) return "31"; // Grandmaster 5
        if (skillRating < 4600) return "32"; // Grandmaster 4
        if (skillRating < 4700) return "33"; // Grandmaster 3
        if (skillRating < 4800) return "34"; // Grandmaster 2
        if (skillRating < 4900) return "35"; // Grandmaster 1
        return "36"; // Champion
    }
}