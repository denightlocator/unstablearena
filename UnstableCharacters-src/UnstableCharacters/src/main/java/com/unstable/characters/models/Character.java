package com.unstable.characters.models;

import java.util.List;

public class Character {
    private String id;
    private String displayName;
    private String clan;
    private String prefix;
    private String suffix;
    private String chatColor;
    private String description;
    private List<String> lore;
    private String tabPrefix;
    private String tabSuffix;
    private boolean glowEnabled;
    private String glowColor;
    private boolean available;

    public Character(String id, String displayName, String clan) {
        this.id = id;
        this.displayName = displayName;
        this.clan = clan;
        this.prefix = "";
        this.suffix = "";
        this.chatColor = "white";
        this.description = "";
        this.lore = List.of();
        this.tabPrefix = "";
        this.tabSuffix = "";
        this.glowEnabled = false;
        this.glowColor = "white";
        this.available = true;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getClan() { return clan; }
    public void setClan(String clan) { this.clan = clan; }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix != null ? prefix : ""; }

    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix != null ? suffix : ""; }

    public String getChatColor() { return chatColor; }
    public void setChatColor(String chatColor) { this.chatColor = chatColor; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getLore() { return lore; }
    public void setLore(List<String> lore) { this.lore = lore; }

    public String getTabPrefix() { return tabPrefix; }
    public void setTabPrefix(String tabPrefix) { this.tabPrefix = tabPrefix != null ? tabPrefix : ""; }

    public String getTabSuffix() { return tabSuffix; }
    public void setTabSuffix(String tabSuffix) { this.tabSuffix = tabSuffix != null ? tabSuffix : ""; }

    public boolean isGlowEnabled() { return glowEnabled; }
    public void setGlowEnabled(boolean glowEnabled) { this.glowEnabled = glowEnabled; }

    public String getGlowColor() { return glowColor; }
    public void setGlowColor(String glowColor) { this.glowColor = glowColor; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
}
