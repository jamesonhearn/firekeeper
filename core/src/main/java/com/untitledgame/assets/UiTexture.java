package com.untitledgame.assets;

public enum UiTexture {
    HEALTHBAR_FULL("ui/healthbar_full"),
    HEALTHBAR_75("ui/healthbar_75"),
    HEALTHBAR_50("ui/healthbar_50"),
    HEALTHBAR_25("ui/healthbar_25"),
    HEALTHBAR_EMPTY("ui/healthbar_empty");

    private final String key;

    UiTexture(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
