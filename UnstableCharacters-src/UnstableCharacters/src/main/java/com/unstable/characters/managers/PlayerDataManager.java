package com.unstable.characters.managers;

import com.unstable.characters.UnstableCharacters;
import com.unstable.characters.models.Character;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final UnstableCharacters plugin;
    private final File playerDataFile;
    private final Map<UUID, PlayerData> playerDataCache;
    private FileConfiguration config;

    public PlayerDataManager(UnstableCharacters plugin) {
        this.plugin = plugin;
        this.playerDataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        this.playerDataCache = new HashMap<>();
        loadData();
    }

    private void loadData() {
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create playerdata.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(playerDataFile);

        // Load player claims
        if (config.contains("claims")) {
            for (String uuid : config.getConfigurationSection("claims").getKeys(false)) {
                try {
                    UUID playerUuid = UUID.fromString(uuid);
                    String characterId = config.getString("claims." + uuid + ".character", "");
                    PlayerData data = new PlayerData(playerUuid, characterId);

                    if (config.contains("claims." + uuid + ".cooldowns")) {
                        for (String kit : config.getConfigurationSection("claims." + uuid + ".cooldowns").getKeys(false)) {
                            long time = config.getLong("claims." + uuid + ".cooldowns." + kit);
                            data.setKitCooldown(kit, time);
                        }
                    }

                    playerDataCache.put(playerUuid, data);

                    // Restore claims to character manager
                    if (!characterId.isEmpty()) {
                        plugin.getCharacterManager().claimCharacter(characterId, playerUuid);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in playerdata.yml: " + uuid);
                }
            }
        }
    }

    public void saveAll() {
        for (PlayerData data : playerDataCache.values()) {
            savePlayerData(data);
        }
        saveConfig();
    }

    public void savePlayerData(PlayerData data) {
        config.set("claims." + data.getPlayerId().toString() + ".character", data.getClaimedCharacter());

        // Save cooldowns
        for (Map.Entry<String, Long> entry : data.getCooldowns().entrySet()) {
            config.set("claims." + data.getPlayerId().toString() + ".cooldowns." + entry.getKey(), entry.getValue());
        }
    }

    public void saveConfig() {
        try {
            config.save(playerDataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save playerdata.yml: " + e.getMessage());
        }
    }

    public PlayerData getPlayerData(UUID playerId) {
        return playerDataCache.computeIfAbsent(playerId, id -> new PlayerData(id, ""));
    }

    public void claimCharacter(UUID playerId, String characterId) {
        PlayerData data = getPlayerData(playerId);

        // Unclaim previous character if any
        if (!data.getClaimedCharacter().isEmpty()) {
            plugin.getCharacterManager().unclaimCharacter(data.getClaimedCharacter());
        }

        data.setClaimedCharacter(characterId);
        plugin.getCharacterManager().claimCharacter(characterId, playerId);
        savePlayerData(data);
        saveConfig();
    }

    public void unclaimCharacter(UUID playerId) {
        PlayerData data = getPlayerData(playerId);

        if (!data.getClaimedCharacter().isEmpty()) {
            plugin.getCharacterManager().unclaimCharacter(data.getClaimedCharacter());
            data.setClaimedCharacter("");
            savePlayerData(data);
            saveConfig();
        }
    }

    public String getClaimedCharacter(UUID playerId) {
        return getPlayerData(playerId).getClaimedCharacter();
    }

    public Character getCharacter(UUID playerId) {
        String charId = getClaimedCharacter(playerId);
        if (charId.isEmpty()) return null;
        return plugin.getCharacterManager().getCharacter(charId);
    }

    public void setKitCooldown(UUID playerId, String kitId, long endTime) {
        PlayerData data = getPlayerData(playerId);
        data.setKitCooldown(kitId, endTime);
        savePlayerData(data);
        saveConfig();
    }

    public long getKitCooldown(UUID playerId, String kitId) {
        return getPlayerData(playerId).getKitCooldown(kitId);
    }

    public boolean isOnCooldown(UUID playerId, String kitId) {
        return getKitCooldown(playerId, kitId) > System.currentTimeMillis();
    }

    public void clearCooldowns(UUID playerId) {
        getPlayerData(playerId).getCooldowns().clear();
        savePlayerData(getPlayerData(playerId));
        saveConfig();
    }

    public static class PlayerData {
        private final UUID playerId;
        private String claimedCharacter;
        private final Map<String, Long> cooldowns;

        public PlayerData(UUID playerId, String claimedCharacter) {
            this.playerId = playerId;
            this.claimedCharacter = claimedCharacter;
            this.cooldowns = new HashMap<>();
        }

        public UUID getPlayerId() { return playerId; }
        public String getClaimedCharacter() { return claimedCharacter; }
        public void setClaimedCharacter(String claimedCharacter) { this.claimedCharacter = claimedCharacter; }
        public Map<String, Long> getCooldowns() { return cooldowns; }

        public long getKitCooldown(String kitId) {
            return cooldowns.getOrDefault(kitId, 0L);
        }

        public void setKitCooldown(String kitId, long endTime) {
            cooldowns.put(kitId, endTime);
        }

        public void clearKitCooldown(String kitId) {
            cooldowns.remove(kitId);
        }

        public long getRemainingCooldown(String kitId) {
            long remaining = getKitCooldown(kitId) - System.currentTimeMillis();
            return Math.max(0, remaining);
        }
    }
}
