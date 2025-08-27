package com.owmetagg.events;

import org.springframework.context.ApplicationEvent;

public class PlayerDataProcessedEvent extends ApplicationEvent {
    private final String battletag;

    public PlayerDataProcessedEvent(Object source, String battletag) {
        super(source);
        this.battletag = battletag;
    }

    // Alternative simpler constructor if you don't need source
    public PlayerDataProcessedEvent(String battletag) {
        super(battletag);
        this.battletag = battletag;
    }

    public String getBattletag() {
        return battletag;
    }
}