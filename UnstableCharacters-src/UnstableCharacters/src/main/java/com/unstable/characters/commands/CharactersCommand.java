package com.unstable.characters.commands;

import com.unstable.characters.UnstableCharacters;
import com.unstable.characters.models.Character;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CharactersCommand implements CommandExecutor, TabCompleter {

    private final UnstableCharacters plugin;

    public CharactersCommand(UnstableCharacters plugin) {
        this.plugin = plugin;
        plugin.getCommand("characters").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("unstable.characters.list")) {
            sender.sendMessage(Component.text("You don't have permission to view characters!", NamedTextColor.RED));
            return true;
        }

        String clanFilter = args.length > 0 ? args[0].toLowerCase() : null;

        List<Character> characters = new ArrayList<>(plugin.getCharacterManager().getCharacters().values());

        // Apply clan filter
        if (clanFilter != null) {
            characters = characters.stream()
                .filter(c -> c.getClan().toLowerCase().contains(clanFilter))
                .collect(Collectors.toList());
        }

        // Send header
        sender.sendMessage(Component.text("╔═══════════════════════════════════════════════════╗", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("            UNSTABLE SMP CHARACTERS", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("╚═══════════════════════════════════════════════════╝", NamedTextColor.GOLD));

        // Group by clan
        Map<String, List<Character>> groupedByClan = characters.stream()
            .collect(Collectors.groupingBy(Character::getClan));

        // Sort clans
        List<String> sortedClans = new ArrayList<>(groupedByClan.keySet());
        sortedClans.sort(String.CASE_INSENSITIVE_ORDER);

        for (String clan : sortedClans) {
            List<Character> clanChars = groupedByClan.get(clan);
            if (clanChars == null || clanChars.isEmpty()) continue;

            NamedTextColor clanColor = getClanColor(clan);
            sender.sendMessage(Component.text()
                .append(Component.text("◆ ", clanColor))
                .append(Component.text("【" + clan + "】", clanColor))
                .append(Component.text(" (" + clanChars.size() + " characters)", NamedTextColor.GRAY))
                .build());

            for (Character character : clanChars) {
                boolean claimed = plugin.getCharacterManager().isCharacterClaimed(character.getId());
                NamedTextColor statusColor = claimed ? NamedTextColor.DARK_RED : NamedTextColor.GREEN;
                String statusText = claimed ? " [CLAIMED]" : " [AVAILABLE]";

                NamedTextColor charColor = getCharacterColor(character.getChatColor());

                Component charLine = Component.text()
                    .append(Component.text("  └─ ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(character.getDisplayName(), charColor))
                    .append(Component.text(" (" + character.getId() + ")", NamedTextColor.DARK_GRAY))
                    .append(Component.text(statusText, statusColor))
                    .hoverEvent(HoverEvent.showText(buildHoverText(character, claimed)))
                    .clickEvent(ClickEvent.suggestCommand("/claimcharacter " + character.getId()))
                    .build();

                sender.sendMessage(charLine);
            }
            sender.sendMessage(Component.text(""));
        }

        // Summary
        sender.sendMessage(Component.text("═══════════════════════════════════════════════════", NamedTextColor.GOLD));
        sender.sendMessage(Component.text()
            .append(Component.text("Total: " + characters.size() + " characters | ", NamedTextColor.GRAY))
            .append(Component.text("Use /claimcharacter <name> ", NamedTextColor.YELLOW))
            .append(Component.text("to claim", NamedTextColor.GRAY))
            .build());

        return true;
    }

    private Component buildHoverText(Character character, boolean claimed) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text(character.getDisplayName(), getCharacterColor(character.getChatColor())));
        lines.add(Component.text("Clan: " + character.getClan(), NamedTextColor.GRAY));
        lines.add(Component.text(""));
        lines.add(Component.text(character.getDescription(), NamedTextColor.DARK_GRAY));

        if (!character.getLore().isEmpty()) {
            lines.add(Component.text(""));
            for (String lore : character.getLore()) {
                lines.add(Component.text(lore).decoration(TextDecoration.ITALIC, TextDecoration.State.TRUE));
            }
        }

        lines.add(Component.text(""));
        if (claimed) {
            lines.add(Component.text("⚠ This character is already claimed!", NamedTextColor.RED));
        } else {
            lines.add(Component.text("➤ Click to claim this character!", NamedTextColor.GREEN));
        }

        return Component.join(Component.newline(), lines);
    }

    private NamedTextColor getClanColor(String clan) {
        return switch (clan == null ? "" : clan.toLowerCase()) {
            case "protagonists" -> NamedTextColor.AQUA;
            case "allies" -> NamedTextColor.GREEN;
            case "zam empire" -> NamedTextColor.GOLD;
            case "mafia" -> NamedTextColor.DARK_GRAY;
            case "antagonists" -> NamedTextColor.RED;
            case "null" -> NamedTextColor.DARK_PURPLE;
            case "cindercrest" -> NamedTextColor.GOLD;
            case "kingdom of the caves" -> NamedTextColor.GREEN;
            case "soul keepers" -> NamedTextColor.DARK_PURPLE;
            case "law enforcement" -> NamedTextColor.BLUE;
            case "technicians" -> NamedTextColor.AQUA;
            case "builders" -> NamedTextColor.GREEN;
            default -> NamedTextColor.GRAY;
        };
    }

    private NamedTextColor getCharacterColor(String colorName) {
        return switch (colorName == null ? "" : colorName.toLowerCase()) {
            case "black" -> NamedTextColor.BLACK;
            case "dark_blue", "darkblue" -> NamedTextColor.DARK_BLUE;
            case "dark_green", "darkgreen" -> NamedTextColor.DARK_GREEN;
            case "dark_aqua", "darkaqua" -> NamedTextColor.DARK_AQUA;
            case "dark_red", "darkred" -> NamedTextColor.DARK_RED;
            case "dark_purple", "darkpurple" -> NamedTextColor.DARK_PURPLE;
            case "gold" -> NamedTextColor.GOLD;
            case "gray" -> NamedTextColor.GRAY;
            case "dark_gray", "darkgray" -> NamedTextColor.DARK_GRAY;
            case "blue" -> NamedTextColor.BLUE;
            case "green" -> NamedTextColor.GREEN;
            case "aqua" -> NamedTextColor.AQUA;
            case "red" -> NamedTextColor.RED;
            case "light_purple", "lightpurple" -> NamedTextColor.LIGHT_PURPLE;
            case "yellow" -> NamedTextColor.YELLOW;
            default -> NamedTextColor.WHITE;
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggest clans
            for (String clan : plugin.getCharacterManager().getAllClans()) {
                if (clan.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(clan);
                }
            }
        }

        return completions;
    }
}
