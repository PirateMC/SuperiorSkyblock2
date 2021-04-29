package com.bgsoftware.superiorskyblock.raiding;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;

public enum CooldownType {
    WIN("after-win"),
    LOSS("after-loss"),
    DRAW("after-draw");

    private String configKey;
    private long duration;

    CooldownType(String configKey) {
        this.configKey = configKey;
    }

    public long getDuration() {
        return SuperiorSkyblockPlugin
                .getPlugin()
                .getConfig()
                .getLong("raids.cooldown." + configKey);
    }
}
