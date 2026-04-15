package com.flowpvp.client.config;

public class NametagComponentConfig {

    public NametagComponent type;
    public boolean enabled;
    /**
     * For ELO: show "ELO" suffix (e.g. "886 ELO" vs "886").
     * For POSITION: show "Globally"/"Ranked" suffix (e.g. "#7,595 Globally" vs "#7,595").
     * Ignored for TIER and GAMEMODE.
     */
    public boolean showLabel;

    /** No-arg constructor required by Gson. */
    public NametagComponentConfig() {}

    public NametagComponentConfig(NametagComponent type, boolean enabled, boolean showLabel) {
        this.type = type;
        this.enabled = enabled;
        this.showLabel = showLabel;
    }
}
