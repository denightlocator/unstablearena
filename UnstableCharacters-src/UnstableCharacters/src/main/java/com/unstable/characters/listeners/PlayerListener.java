package com.unstable.characters.listeners;

import com.unstable.characters.UnstableCharacters;
import com.unstable.characters.models.Character;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class PlayerListener implements Listener {

    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();
    private static final LegacyComponentSerializer LEGACY_AMP = LegacyComponentSerializer.legacyAmpersand();

    private final UnstableCharacters plugin;

    public PlayerListener(UnstableCharacters plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Small delay so the client is fully connected before we push names
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                updatePlayerDisplay(player);
            }
        }, 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith("char_") && team.getEntries().contains(player.getName())) {
                team.removeEntry(player.getName());
            }
        }
    }

    /**
     * Applies (or removes) the character name transformation for a player:
     * tab list, name above head (display name) and chat prefix/suffix.
     */
    public void updatePlayerDisplay(Player player) {
        Character character = plugin.getPlayerDataManager().getCharacter(player.getUniqueId());

        if (character != null) {
            applyCharacterDisplay(player, character);
        } else {
            removeCharacterDisplay(player);
        }

        updateTabList(player, character);
    }

    private void applyCharacterDisplay(Player player, Character character) {
        String prefix = ChatColor.translateAlternateColorCodes('&', character.getPrefix());
        String suffix = ChatColor.translateAlternateColorCodes('&', character.getSuffix());
        String color = getChatColor(character.getChatColor()).toString();

        String fullName = prefix + color + character.getDisplayName() + suffix;
        Component nameComponent = LEGACY_SECTION.deserialize(fullName);

        // Name above head + name used in chat (visible to all players)
        player.displayName(nameComponent);
        // Tab list entry
        player.playerListName(nameComponent);

        // Glow team (optional enchantment glint on the nametag).
        // Only added when glow is enabled so the nametag keeps its own colors.
        removeFromCharacterTeams(player);

        if (character.isGlowEnabled()) {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            String teamName = "char_" + character.getId().toLowerCase();
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
                team.setCanSeeFriendlyInvisibles(true);
            }
            team.color(getTeamColor(character.getGlowColor()));
            team.addEntry(player.getName());
        }
    }

    private void removeCharacterDisplay(Player player) {
        removeFromCharacterTeams(player);
        player.setDisplayName(null);
        player.setPlayerListName(null);
    }

    private void removeFromCharacterTeams(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith("char_") && team.getEntries().contains(player.getName())) {
                team.removeEntry(player.getName());
            }
        }
    }

    private void updateTabList(Player player, Character character) {
        String header;
        if (character != null) {
            header = ChatColor.GOLD + "=== " + ChatColor.WHITE + "Unstable Arena" + ChatColor.GOLD + " ===\n" +
                ChatColor.GRAY + "Character: " + getChatColor(character.getChatColor()) + character.getDisplayName() + "\n" +
                ChatColor.GRAY + "Clan: " + getClanColor(character.getClan()) + character.getClan();
        } else {
            header = ChatColor.GOLD + "=== " + ChatColor.WHITE + "Unstable Arena" + ChatColor.GOLD + " ===\n" +
                ChatColor.GRAY + "No character claimed\n" +
                ChatColor.YELLOW + "Use /claimcharacter <name>";
        }

        String footer = "\n" + ChatColor.GOLD + "=====================";

        player.setPlayerListHeader(header);
        player.setPlayerListFooter(footer);
    }

    private net.kyori.adventure.text.format.NamedTextColor getTeamColor(String colorName) {
        return switch (colorName == null ? "" : colorName.toLowerCase()) {
            case "black" -> net.kyori.adventure.text.format.NamedTextColor.BLACK;
            case "dark_blue" -> net.kyori.adventure.text.format.NamedTextColor.DARK_BLUE;
            case "dark_green" -> net.kyori.adventure.text.format.NamedTextColor.DARK_GREEN;
            case "dark_aqua" -> net.kyori.adventure.text.format.NamedTextColor.DARK_AQUA;
            case "dark_red" -> net.kyori.adventure.text.format.NamedTextColor.DARK_RED;
            case "dark_purple" -> net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE;
            case "gold" -> net.kyori.adventure.text.format.NamedTextColor.GOLD;
            case "gray" -> net.kyori.adventure.text.format.NamedTextColor.GRAY;
            case "dark_gray" -> net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
            case "blue" -> net.kyori.adventure.text.format.NamedTextColor.BLUE;
            case "green" -> net.kyori.adventure.text.format.NamedTextColor.GREEN;
            case "aqua" -> net.kyori.adventure.text.format.NamedTextColor.AQUA;
            case "red" -> net.kyori.adventure.text.format.NamedTextColor.RED;
            case "light_purple" -> net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE;
            case "yellow" -> net.kyori.adventure.text.format.NamedTextColor.YELLOW;
            default -> net.kyori.adventure.text.format.NamedTextColor.WHITE;
        };
    }

    private ChatColor getChatColor(String colorName) {
        if (colorName == null) return ChatColor.WHITE;
        return switch (colorName.toLowerCase()) {
            case "black" -> ChatColor.BLACK;
            case "dark_blue", "darkblue" -> ChatColor.DARK_BLUE;
            case "dark_green", "darkgreen" -> ChatColor.DARK_GREEN;
            case "dark_aqua", "darkaqua" -> ChatColor.DARK_AQUA;
            case "dark_red", "darkred" -> ChatColor.DARK_RED;
            case "dark_purple", "darkpurple" -> ChatColor.DARK_PURPLE;
            case "gold" -> ChatColor.GOLD;
            case "gray" -> ChatColor.GRAY;
            case "dark_gray", "darkgray" -> ChatColor.DARK_GRAY;
            case "blue" -> ChatColor.BLUE;
            case "green" -> ChatColor.GREEN;
            case "aqua" -> ChatColor.AQUA;
            case "red" -> ChatColor.RED;
            case "light_purple", "lightpurple" -> ChatColor.LIGHT_PURPLE;
            case "yellow" -> ChatColor.YELLOW;
            default -> ChatColor.WHITE;
        };
    }

    private ChatColor getClanColor(String clan) {
        return switch (clan == null ? "" : clan.toLowerCase()) {
            case "protagonists" -> ChatColor.AQUA;
            case "allies" -> ChatColor.GREEN;
            case "zam empire" -> ChatColor.GOLD;
            case "mafia" -> ChatColor.DARK_GRAY;
            case "antagonists" -> ChatColor.RED;
            case "null" -> ChatColor.DARK_PURPLE;
            case "cindercrest" -> ChatColor.GOLD;
            case "kingdom of the caves" -> ChatColor.GREEN;
            case "soul keepers" -> ChatColor.DARK_PURPLE;
            case "law enforcement" -> ChatColor.BLUE;
            case "technicians" -> ChatColor.AQUA;
            case "builders" -> ChatColor.GREEN;
            default -> ChatColor.GRAY;
        };
    }

    public void refreshAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerDisplay(player);
        }
    }
}
