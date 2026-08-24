package com.unstable.characters.commands;

import com.unstable.characters.UnstableCharacters;
import com.unstable.characters.models.Character;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClaimCharacterCommand implements CommandExecutor, TabCompleter {

    private final UnstableCharacters plugin;

    public ClaimCharacterCommand(UnstableCharacters plugin) {
        this.plugin = plugin;
        plugin.getCommand("claimcharacter").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("unstable.characters.claim")) {
            player.sendMessage(Component.text("You don't have permission to claim characters!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendCharacterList(player);
            return true;
        }

        // Try: exact id, then args joined (spaces), then display name match
        Character character = plugin.getCharacterManager().getCharacter(args[0].toLowerCase());
        if (character == null) {
            character = plugin.getCharacterManager().getCharacter(String.join("", args).toLowerCase());
        }
        if (character == null) {
            for (Character c : plugin.getCharacterManager().getCharacters().values()) {
                if (c.getDisplayName().equalsIgnoreCase(args[0])
                        || c.getDisplayName().toLowerCase().replace(" ", "").equals(String.join("", args).toLowerCase())) {
                    character = c;
                    break;
                }
            }
        }

        if (character == null) {
            player.sendMessage(Component.text("Character '" + String.join(" ", args) + "' not found!", NamedTextColor.RED));
            player.sendMessage(Component.text("Use /characters to see all available characters.", NamedTextColor.GRAY));
            return true;
        }

        if (!character.isAvailable() && !player.hasPermission("unstable.characters.admin")) {
            player.sendMessage(Component.text("This character is not available for claiming!", NamedTextColor.RED));
            return true;
        }

        // Check if character is already claimed
        String currentOwner = plugin.getCharacterManager().getCharacterOwner(character.getId());
        if (currentOwner != null && !currentOwner.equals(player.getUniqueId().toString())) {
            player.sendMessage(Component.text("This character is already claimed by another player!", NamedTextColor.RED));
            return true;
        }

        // Claim the character
        plugin.getPlayerDataManager().claimCharacter(player.getUniqueId(), character.getId());

        // Update display (tab, nametag, chat)
        plugin.getPlayerListener().updatePlayerDisplay(player);

        // Success message
        player.sendMessage(Component.text()
            .append(Component.text("✓ ", NamedTextColor.GREEN))
            .append(Component.text("You are now ", NamedTextColor.GRAY))
            .append(Component.text(character.getDisplayName(), NamedTextColor.GOLD))
            .append(Component.text(" [" + character.getClan() + "]", NamedTextColor.GRAY))
            .build());

        // Broadcast to others (optional)
        if (plugin.getConfig().getBoolean("settings.broadcast-claims", true)) {
            Bukkit.broadcast(Component.text()
                .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" has claimed the character ", NamedTextColor.GRAY))
                .append(Component.text(character.getDisplayName(), NamedTextColor.GOLD))
                .build());
        }

        return true;
    }

    private void sendCharacterList(Player player) {
        player.sendMessage(Component.text("╔════════════════════════════════════════╗", NamedTextColor.GOLD));
        player.sendMessage(Component.text("         AVAILABLE CHARACTERS", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("╚════════════════════════════════════════╝", NamedTextColor.GOLD));

        // Group characters by clan
        List<Character> characters = plugin.getCharacterManager().getAvailableCharacters();
        Map<String, List<Character>> groupedByClan = characters.stream()
            .collect(Collectors.groupingBy(Character::getClan));

        List<String> sortedClans = new ArrayList<>(groupedByClan.keySet());
        sortedClans.sort(String.CASE_INSENSITIVE_ORDER);

        for (String clan : sortedClans) {
            List<Character> clanChars = groupedByClan.get(clan);

            player.sendMessage(Component.text("【" + clan + "】", NamedTextColor.DARK_GRAY));

            for (Character character : clanChars) {
                // Check if claimed
                boolean claimed = plugin.getCharacterManager().isCharacterClaimed(character.getId());
                NamedTextColor statusColor = claimed ? NamedTextColor.DARK_RED : NamedTextColor.GREEN;
                String status = claimed ? " [CLAIMED]" : "";

                Component charComponent = Component.text()
                    .append(Component.text("  ◆ ", statusColor))
                    .append(Component.text(character.getDisplayName(), NamedTextColor.WHITE))
                    .append(Component.text(" - " + character.getDescription(), NamedTextColor.GRAY))
                    .append(Component.text(status, NamedTextColor.DARK_RED))
                    .hoverEvent(HoverEvent.showText(Component.text()
                        .append(Component.text("Click to claim ", NamedTextColor.YELLOW))
                        .append(Component.text(character.getDisplayName(), NamedTextColor.GOLD))
                        .append(Component.text("\nClan: " + character.getClan(), NamedTextColor.GRAY))
                        .build()))
                    .clickEvent(ClickEvent.suggestCommand("/claimcharacter " + character.getId()))
                    .build();

                player.sendMessage(charComponent);
            }
            player.sendMessage(Component.text(""));
        }

        player.sendMessage(Component.text()
            .append(Component.text("\nClick a character name or type ", NamedTextColor.GRAY))
            .append(Component.text("/claimcharacter <name>", NamedTextColor.YELLOW))
            .build());
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (Character character : plugin.getCharacterManager().getCharacters().values()) {
                if (character.getId().toLowerCase().startsWith(partial)
                        || character.getDisplayName().toLowerCase().startsWith(partial)) {
                    completions.add(character.getId());
                }
            }
        }

        return completions;
    }
}
