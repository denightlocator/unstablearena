package com.unstable.characters.listeners;

import com.unstable.characters.UnstableCharacters;
import com.unstable.characters.models.Character;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final UnstableCharacters plugin;

    public ChatListener(UnstableCharacters plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Character character = plugin.getPlayerDataManager().getCharacter(player.getUniqueId());

        if (character != null) {
            String prefix = ChatColor.translateAlternateColorCodes('&', character.getPrefix());
            String suffix = ChatColor.translateAlternateColorCodes('&', character.getSuffix());
            String color = getChatColor(character.getChatColor()).toString();

            event.setFormat(prefix + color + character.getDisplayName() + suffix + ChatColor.WHITE + ": " + event.getMessage());
        } else {
            event.setFormat(ChatColor.GRAY + player.getName() + ChatColor.WHITE + ": " + event.getMessage());
        }
    }

    private org.bukkit.ChatColor getChatColor(String colorName) {
        if (colorName == null) return org.bukkit.ChatColor.WHITE;
        return switch (colorName.toLowerCase()) {
            case "black" -> org.bukkit.ChatColor.BLACK;
            case "dark_blue", "darkblue" -> org.bukkit.ChatColor.DARK_BLUE;
            case "dark_green", "darkgreen" -> org.bukkit.ChatColor.DARK_GREEN;
            case "dark_aqua", "darkaqua" -> org.bukkit.ChatColor.DARK_AQUA;
            case "dark_red", "darkred" -> org.bukkit.ChatColor.DARK_RED;
            case "dark_purple", "darkpurple" -> org.bukkit.ChatColor.DARK_PURPLE;
            case "gold" -> org.bukkit.ChatColor.GOLD;
            case "gray" -> org.bukkit.ChatColor.GRAY;
            case "dark_gray", "darkgray" -> org.bukkit.ChatColor.DARK_GRAY;
            case "blue" -> org.bukkit.ChatColor.BLUE;
            case "green" -> org.bukkit.ChatColor.GREEN;
            case "aqua" -> org.bukkit.ChatColor.AQUA;
            case "red" -> org.bukkit.ChatColor.RED;
            case "light_purple", "lightpurple" -> org.bukkit.ChatColor.LIGHT_PURPLE;
            case "yellow" -> org.bukkit.ChatColor.YELLOW;
            default -> org.bukkit.ChatColor.WHITE;
        };
    }
}
