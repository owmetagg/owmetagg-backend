package com.owmetagg.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Data
@NoArgsConstructor
public class HeroStatsId implements Serializable {

    @Column(name = "player_id")
    private String playerId;

    @Column(name = "hero_key")
    private String heroKey; // tracer, mercy, reinhardt, etc.

    @Column(name = "platform")
    private String platform; // pc, console

    @Column(name = "game_mode")
    private String gameMode; // competitive, quickplay

    public HeroStatsId(String playerId, String heroKey, String platform, String gameMode) {
        this.playerId = playerId;
        this.heroKey = heroKey;
        this.platform = platform;
        this.gameMode = gameMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HeroStatsId that = (HeroStatsId) o;
        return Objects.equals(playerId, that.playerId) &&
                Objects.equals(heroKey, that.heroKey) &&
                Objects.equals(platform, that.platform) &&
                Objects.equals(gameMode, that.gameMode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, heroKey, platform, gameMode);
    }
}