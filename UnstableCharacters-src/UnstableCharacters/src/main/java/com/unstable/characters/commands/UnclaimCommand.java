package com.unstable.characters.commands;

import com.unstable.characters.UnstableCharacters;
import com.unstable.characters.models.Character;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UnclaimCommand implements CommandExecutor {

    private final UnstableCharacters plugin;

    public UnclaimCommand(UnstableCharacters plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("unstable.characters.unclaim")) {
            player.sendMessage(Component.text("You don't have permission to unclaim characters!", NamedTextColor.RED));
            return true;
        }

        String claimedCharacter = plugin.getPlayerDataManager().getClaimedCharacter(player.getUniqueId());

        if (claimedCharacter == null || claimedCharacter.isEmpty()) {
            player.sendMessage(Component.text()
                .append(Component.text("⚠ ", NamedTextColor.YELLOW))
                .append(Component.text("You don't have a claimed character!", NamedTextColor.RED))
                .build());
            player.sendMessage(Component.text()
                .append(Component.text("Use /claimcharacter <name> ", NamedTextColor.GRAY))
                .append(Component.text("to claim one.", NamedTextColor.YELLOW))
                .build());
            return true;
        }

        Character character = plugin.getCharacterManager().getCharacter(claimedCharacter);
        String characterName = character != null ? character.getDisplayName() : claimedCharacter;

        // Unclaim the character
        plugin.getPlayerDataManager().unclaimCharacter(player.getUniqueId());

        // Reset display (tab, nametag, chat)
        plugin.getPlayerListener().updatePlayerDisplay(player);

        // Success message
        player.sendMessage(Component.text()
            .append(Component.text("✓ ", NamedTextColor.GREEN))
            .append(Component.text("You have unclaimed ", NamedTextColor.GRAY))
            .append(Component.text(characterName, NamedTextColor.YELLOW))
            .build());

        player.sendMessage(Component.text(
            "Your character name has been reset.", NamedTextColor.GRAY));

        return true;
    }
}
