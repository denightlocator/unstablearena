package com.unstable.characters;

import com.unstable.characters.commands.ClaimCharacterCommand;
import com.unstable.characters.commands.CharactersCommand;
import com.unstable.characters.commands.KitCommand;
import com.unstable.characters.commands.KitsCommand;
import com.unstable.characters.commands.UnclaimCommand;
import com.unstable.characters.listeners.ChatListener;
import com.unstable.characters.listeners.PlayerListener;
import com.unstable.characters.managers.CharacterManager;
import com.unstable.characters.managers.KitManager;
import com.unstable.characters.managers.PlayerDataManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public class UnstableCharacters extends JavaPlugin {

    private static UnstableCharacters instance;
    private CharacterManager characterManager;
    private PlayerDataManager playerDataManager;
    private KitManager kitManager;
    private PlayerListener playerListener;

    @Override
    public void onEnable() {
        instance = this;

        // Save default config
        saveDefaultConfig();

        // Initialize managers
        characterManager = new CharacterManager(this);
        playerDataManager = new PlayerDataManager(this);
        kitManager = new KitManager(this);

        // Register commands
        registerCommand("claimcharacter", new ClaimCharacterCommand(this));
        registerCommand("characters", new CharactersCommand(this));
        registerCommand("unclaim", new UnclaimCommand(this));
        registerCommand("kit", new KitCommand(this));
        registerCommand("kits", new KitsCommand(this));

        // Register listeners
        playerListener = new PlayerListener(this);
        getServer().getPluginManager().registerEvents(playerListener, this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        getLogger().info("UnstableCharacters has been enabled!");
        getLogger().info("Loaded " + characterManager.getCharacters().size() + " characters");
        getLogger().info("Loaded " + kitManager.getKits().size() + " kits");
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command '" + name + "' is not defined in plugin.yml!");
            return;
        }
        command.setExecutor(executor);
        if (executor instanceof TabCompleter tabCompleter) {
            command.setTabCompleter(tabCompleter);
        }
    }

    @Override
    public void onDisable() {
        // Save all data
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }
        if (kitManager != null) {
            kitManager.saveAll();
        }
        getLogger().info("UnstableCharacters has been disabled!");
    }

    public static UnstableCharacters getInstance() {
        return instance;
    }

    public CharacterManager getCharacterManager() {
        return characterManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public PlayerListener getPlayerListener() {
        return playerListener;
    }
}
