package com.unstable.characters.commands;

import com.unstable.characters.UnstableCharacters;
import com.unstable.characters.models.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KitCommand implements CommandExecutor, TabCompleter {

    private final UnstableCharacters plugin;

    public KitCommand(UnstableCharacters plugin) {
        this.plugin = plugin;
        plugin.getCommand("kit").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("unstable.kits.use")) {
            player.sendMessage(Component.text("You don't have permission to use kits!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendKitList(player);
            return true;
        }

        String kitId = args[0].toLowerCase();
        Kit kit = plugin.getKitManager().getKit(kitId);

        if (kit == null) {
            player.sendMessage(Component.text("Kit '" + args[0] + "' not found! Use /kit to see all kits.", NamedTextColor.RED));
            return true;
        }

        if (!kit.isEnabled()) {
            player.sendMessage(Component.text("This kit is currently disabled!", NamedTextColor.RED));
            return true;
        }

        if (!canUseKit(player, kit)) {
            player.sendMessage(Component.text("You don't have permission to use this kit!", NamedTextColor.RED));
            return true;
        }

        boolean cooldownsEnabled = plugin.getConfig().getBoolean("kits.enable-cooldowns", true);
        if (cooldownsEnabled && kit.getCooldownSeconds() > 0
                && plugin.getPlayerDataManager().isOnCooldown(player.getUniqueId(), kit.getId())) {
            long remaining = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).getRemainingCooldown(kit.getId());
            player.sendMessage(Component.text()
                .append(Component.text("⏱ ", NamedTextColor.YELLOW))
                .append(Component.text("Kit on cooldown! Wait ", NamedTextColor.GRAY))
                .append(Component.text(formatMillis(remaining), NamedTextColor.YELLOW))
                .build());
            return true;
        }

        giveKit(player, kit);

        if (cooldownsEnabled && kit.getCooldownSeconds() > 0) {
            plugin.getPlayerDataManager().setKitCooldown(player.getUniqueId(), kit.getId(),
                System.currentTimeMillis() + kit.getCooldownSeconds() * 1000);
        }

        return true;
    }

    private boolean canUseKit(Player player, Kit kit) {
        List<String> perms = kit.getAllowedPermissions();
        if (perms.isEmpty() || perms.contains("*")) return true;
        for (String perm : perms) {
            if (player.hasPermission(perm)) return true;
        }
        return false;
    }

    private void giveKit(Player player, Kit kit) {
        // Armor: only replace pieces the kit actually provides (drop what gets replaced)
        if (kit.getHelmet() != null) {
            dropIfPresent(player, player.getInventory().getHelmet());
            player.getInventory().setHelmet(kit.getHelmet().clone());
        }
        if (kit.getChestplate() != null) {
            dropIfPresent(player, player.getInventory().getChestplate());
            player.getInventory().setChestplate(kit.getChestplate().clone());
        }
        if (kit.getLeggings() != null) {
            dropIfPresent(player, player.getInventory().getLeggings());
            player.getInventory().setLeggings(kit.getLeggings().clone());
        }
        if (kit.getBoots() != null) {
            dropIfPresent(player, player.getInventory().getBoots());
            player.getInventory().setBoots(kit.getBoots().clone());
        }

        // Inventory items: fill empty slots, stack onto same items, drop what doesn't fit
        for (Map.Entry<Integer, ItemStack> entry : kit.getInventory().entrySet()) {
            int slot = entry.getKey();
            if (slot < 0 || slot >= 36) continue;
            ItemStack item = entry.getValue().clone();
            ItemStack current = player.getInventory().getItem(slot);
            if (current == null || current.getType() == Material.AIR) {
                player.getInventory().setItem(slot, item);
            } else if (current.isSimilar(item)) {
                int total = current.getAmount() + item.getAmount();
                int max = item.getMaxStackSize();
                if (total <= max) {
                    current.setAmount(total);
                } else {
                    current.setAmount(max);
                    player.getWorld().dropItemNaturally(player.getLocation(), new ItemStack(item.getType(), total - max));
                }
            } else {
                for (ItemStack leftover : player.getInventory().addItem(item).values()) {
                    if (leftover.getAmount() > 0) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }
                }
            }
        }

        player.sendMessage(Component.text()
            .append(Component.text("✓ You received the kit: ", NamedTextColor.GREEN))
            .append(Component.text(kit.getName(), NamedTextColor.GOLD))
            .build());
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
    }

    private void dropIfPresent(Player player, ItemStack item) {
        if (item != null && item.getType() != Material.AIR) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
    }

    private void sendKitList(Player player) {
        Map<String, List<Kit>> byCategory = new LinkedHashMap<>();
        for (Kit kit : plugin.getKitManager().getKits().values()) {
            if (!kit.isEnabled()) continue;
            byCategory.computeIfAbsent(kit.getCategory(), c -> new ArrayList<>()).add(kit);
        }

        player.sendMessage(Component.text("════════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.text("           AVAILABLE KITS", NamedTextColor.YELLOW)
            .decoration(TextDecoration.BOLD, TextDecoration.State.TRUE));
        player.sendMessage(Component.text("════════════════════════════════════", NamedTextColor.GOLD));

        for (Map.Entry<String, List<Kit>> categoryEntry : byCategory.entrySet()) {
            player.sendMessage(Component.text("◆ " + categoryEntry.getKey(), NamedTextColor.GRAY));
            for (Kit kit : categoryEntry.getValue()) {
                boolean allowed = canUseKit(player, kit);
                boolean onCooldown = plugin.getPlayerDataManager().isOnCooldown(player.getUniqueId(), kit.getId());

                Component line = Component.text()
                    .append(Component.text("  └─ ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(kit.getName(), allowed ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY))
                    .append(Component.text(" (" + kit.getId() + ")", NamedTextColor.DARK_GRAY))
                    .append(!allowed ? Component.text(" [NO PERM]", NamedTextColor.DARK_RED) : Component.empty())
                    .append(onCooldown ? Component.text(" [COOLDOWN]", NamedTextColor.YELLOW) : Component.empty())
                    .hoverEvent(HoverEvent.showText(Component.text()
                        .append(Component.text(kit.getDescription(), NamedTextColor.GRAY))
                        .append(Component.text("\n", NamedTextColor.GRAY))
                        .append(Component.text(kit.getCooldownSeconds() > 0
                                ? "Cooldown: " + formatMillis(kit.getCooldownSeconds() * 1000L)
                                : "No cooldown", NamedTextColor.DARK_GRAY))
                        .append(allowed
                                ? Component.text("\n➤ Click to receive!", NamedTextColor.GREEN)
                                : Component.empty())
                        .build()))
                    .clickEvent(allowed ? ClickEvent.suggestCommand("/kit " + kit.getId()) : null)
                    .build();

                player.sendMessage(line);
            }
        }

        player.sendMessage(Component.text("Use /kit <name> to receive a kit", NamedTextColor.GRAY));
    }

    private String formatMillis(long millis) {
        if (millis <= 0) return "0s";
        long hours = millis / 3600000;
        long minutes = (millis % 3600000) / 60000;
        long seconds = (millis % 60000) / 1000;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (Kit kit : plugin.getKitManager().getKits().values()) {
                if (kit.isEnabled() && kit.getId().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(kit.getId());
                }
            }
        }

        return completions;
    }
}
