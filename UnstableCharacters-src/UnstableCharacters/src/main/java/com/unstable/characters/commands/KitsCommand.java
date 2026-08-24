package com.unstable.characters.commands;

import com.unstable.characters.UnstableCharacters;
import com.unstable.characters.models.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KitsCommand implements CommandExecutor, TabCompleter {

    private final UnstableCharacters plugin;

    public KitsCommand(UnstableCharacters plugin) {
        this.plugin = plugin;
        plugin.getCommand("kits").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("unstable.kits.manage") && !sender.hasPermission("unstable.kits.admin")) {
            sender.sendMessage(Component.text("You don't have permission to manage kits!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "edit" -> handleEdit(sender, args);
            case "give" -> handleGive(sender, args);
            case "list" -> handleList(sender);
            case "cooldown" -> handleCooldown(sender, args);
            case "category" -> handleCategory(sender, args);
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage(Component.text("✓ Configuration reloaded!", NamedTextColor.GREEN));
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text()
                .append(Component.text("Usage: /kits create <id> <name>\n", NamedTextColor.RED))
                .append(Component.text("Example: /kits create legendary Legendary Kit", NamedTextColor.GRAY))
                .build());
            return;
        }

        String kitId = args[1].toLowerCase();
        String kitName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        if (plugin.getKitManager().getKit(kitId) != null) {
            sender.sendMessage(Component.text("⚠ Kit '" + kitId + "' already exists!", NamedTextColor.RED));
            return;
        }

        Kit kit = plugin.getKitManager().createKit(kitId, kitName);

        sender.sendMessage(Component.text()
            .append(Component.text("✓ Created kit: ", NamedTextColor.GREEN))
            .append(Component.text(kitName, NamedTextColor.GOLD))
            .append(Component.text(" (ID: " + kitId + ")", NamedTextColor.GRAY))
            .build());

        sender.sendMessage(Component.text(
            "Now use /kits edit " + kitId + " to configure it", NamedTextColor.YELLOW));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text()
                .append(Component.text("Usage: /kits delete <kitid>\n", NamedTextColor.RED))
                .append(Component.text("Example: /kits delete starter", NamedTextColor.GRAY))
                .build());
            return;
        }

        String kitId = args[1].toLowerCase();

        if (plugin.getKitManager().deleteKit(kitId)) {
            sender.sendMessage(Component.text()
                .append(Component.text("✓ Deleted kit: ", NamedTextColor.GREEN))
                .append(Component.text(kitId, NamedTextColor.RED))
                .build());
        } else {
            sender.sendMessage(Component.text("⚠ Kit '" + kitId + "' not found!", NamedTextColor.RED));
        }
    }

    private void handleEdit(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendEditHelp(sender);
            return;
        }

        String kitId = args[1].toLowerCase();
        Kit kit = plugin.getKitManager().getKit(kitId);

        if (kit == null) {
            sender.sendMessage(Component.text("⚠ Kit '" + kitId + "' not found!", NamedTextColor.RED));
            return;
        }

        if (args.length < 3) {
            sendEditMenu(sender, kit);
            return;
        }

        String editAction = args[2].toLowerCase();

        switch (editAction) {
            case "name" -> handleEditName(sender, kit, args);
            case "desc", "description" -> handleEditDescription(sender, kit, args);
            case "cooldown" -> handleEditCooldown(sender, kit, args);
            case "category" -> handleEditCategory(sender, kit, args);
            case "icon" -> handleEditIcon(sender, kit);
            case "add" -> handleAddItem(sender, kit);
            case "remove" -> handleRemoveItem(sender, kit, args);
            case "clear" -> handleClearItems(sender, kit);
            case "armor" -> handleSetArmor(sender, kit, args);
            case "perm", "permission" -> handleEditPermission(sender, kit, args);
            case "enable" -> handleToggleEnabled(sender, kit, true);
            case "disable" -> handleToggleEnabled(sender, kit, false);
            case "save" -> {
                plugin.getKitManager().saveAll();
                sender.sendMessage(Component.text("✓ Kit saved!", NamedTextColor.GREEN));
            }
            default -> sendEditMenu(sender, kit);
        }
    }

    private void handleEditName(CommandSender sender, Kit kit, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text()
                .append(Component.text("Usage: /kits edit " + kit.getId() + " name <new name>\n", NamedTextColor.RED))
                .append(Component.text("Example: /kits edit " + kit.getId() + " name Super Kit", NamedTextColor.GRAY))
                .build());
            return;
        }

        String newName = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        kit.setName(newName);
        plugin.getKitManager().updateKit(kit);

        sender.sendMessage(Component.text()
            .append(Component.text("✓ Kit name updated to: ", NamedTextColor.GREEN))
            .append(Component.text(newName, NamedTextColor.GOLD))
            .build());
    }

    private void handleEditDescription(CommandSender sender, Kit kit, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text(
                "Usage: /kits edit " + kit.getId() + " desc <description>", NamedTextColor.RED));
            return;
        }

        String description = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        kit.setDescription(description);
        plugin.getKitManager().updateKit(kit);

        sender.sendMessage(Component.text("✓ Kit description updated!", NamedTextColor.GREEN));
    }

    private void handleEditCooldown(CommandSender sender, Kit kit, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text()
                .append(Component.text("Usage: /kits edit " + kit.getId() + " cooldown <seconds>\n", NamedTextColor.RED))
                .append(Component.text("Example: /kits edit " + kit.getId() + " cooldown 3600", NamedTextColor.GRAY))
                .append(Component.text(" (3600 seconds = 1 hour)", NamedTextColor.DARK_GRAY))
                .build());
            return;
        }

        try {
            long cooldown = Long.parseLong(args[3]);
            kit.setCooldownSeconds(cooldown);
            plugin.getKitManager().updateKit(kit);

            sender.sendMessage(Component.text()
                .append(Component.text("✓ Kit cooldown set to: ", NamedTextColor.GREEN))
                .append(Component.text(formatMillis(cooldown * 1000), NamedTextColor.GOLD))
                .build());
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("⚠ Invalid number: " + args[3], NamedTextColor.RED));
        }
    }

    private void handleEditCategory(CommandSender sender, Kit kit, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text()
                .append(Component.text("Usage: /kits edit " + kit.getId() + " category <name>\n", NamedTextColor.RED))
                .append(Component.text("Example: /kits edit " + kit.getId() + " category Combat", NamedTextColor.GRAY))
                .build());
            return;
        }

        String category = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        kit.setCategory(category);
        plugin.getKitManager().updateKit(kit);

        sender.sendMessage(Component.text()
            .append(Component.text("✓ Kit category set to: ", NamedTextColor.GREEN))
            .append(Component.text(category, NamedTextColor.GOLD))
            .build());
    }

    private void handleEditIcon(CommandSender sender, Kit kit) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command requires a player!", NamedTextColor.RED));
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            sender.sendMessage(Component.text("⚠ Hold an item in your hand to set as the kit icon!", NamedTextColor.RED));
            return;
        }

        kit.setIcon(hand.clone());
        plugin.getKitManager().updateKit(kit);

        sender.sendMessage(Component.text()
            .append(Component.text("✓ Kit icon set to: ", NamedTextColor.GREEN))
            .append(Component.text(hand.getType().name(), NamedTextColor.GOLD))
            .build());
    }

    private void handleAddItem(CommandSender sender, Kit kit) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command requires a player!", NamedTextColor.RED));
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            sender.sendMessage(Component.text("⚠ Hold an item in your hand to add to the kit!", NamedTextColor.RED));
            return;
        }

        // Find next empty slot
        int slot = -1;
        for (int i = 0; i < 36; i++) {
            if (kit.getInventory().get(i) == null) {
                slot = i;
                break;
            }
        }

        if (slot == -1) {
            sender.sendMessage(Component.text("⚠ Kit inventory is full! Remove some items first.", NamedTextColor.RED));
            return;
        }

        kit.addItem(slot, hand.clone());
        plugin.getKitManager().updateKit(kit);

        sender.sendMessage(Component.text()
            .append(Component.text("✓ Added item to slot " + slot + ": ", NamedTextColor.GREEN))
            .append(Component.text(hand.getType().name() + " x" + hand.getAmount(), NamedTextColor.GOLD))
            .build());
    }

    private void handleRemoveItem(CommandSender sender, Kit kit, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text()
                .append(Component.text("Usage: /kits edit " + kit.getId() + " remove <slot>\n", NamedTextColor.RED))
                .append(Component.text("Example: /kits edit " + kit.getId() + " remove 0", NamedTextColor.GRAY))
                .build());
            return;
        }

        try {
            int slot = Integer.parseInt(args[3]);
            if (slot < 0 || slot > 35) {
                sender.sendMessage(Component.text("⚠ Invalid slot! Use 0-35 for inventory items.", NamedTextColor.RED));
                return;
            }

            ItemStack removed = kit.getInventory().remove(slot);
            plugin.getKitManager().updateKit(kit);

            if (removed != null) {
                sender.sendMessage(Component.text()
                    .append(Component.text("✓ Removed item from slot " + slot + ": ", NamedTextColor.GREEN))
                    .append(Component.text(removed.getType().name(), NamedTextColor.GOLD))
                    .build());
            } else {
                sender.sendMessage(Component.text("⚠ Slot " + slot + " was already empty!", NamedTextColor.YELLOW));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("⚠ Invalid slot number!", NamedTextColor.RED));
        }
    }

    private void handleClearItems(CommandSender sender, Kit kit) {
        kit.clearInventory();
        kit.setHelmet(null);
        kit.setChestplate(null);
        kit.setLeggings(null);
        kit.setBoots(null);
        plugin.getKitManager().updateKit(kit);

        sender.sendMessage(Component.text("✓ Cleared all items from kit!", NamedTextColor.GREEN));
    }

    private void handleSetArmor(CommandSender sender, Kit kit, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command requires a player!", NamedTextColor.RED));
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(Component.text()
                .append(Component.text("Usage: /kits edit " + kit.getId() + " armor <helmet|chestplate|leggings|boots>\n", NamedTextColor.RED))
                .append(Component.text("Hold the armor piece in your hand and use the command.", NamedTextColor.GRAY))
                .build());
            return;
        }

        String armorType = args[3].toLowerCase();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType() == Material.AIR) {
            sender.sendMessage(Component.text("⚠ Hold an armor item in your hand!", NamedTextColor.RED));
            return;
        }

        switch (armorType) {
            case "helmet" -> {
                kit.setHelmet(hand.clone());
                sender.sendMessage(Component.text("✓ Helmet set!", NamedTextColor.GREEN));
            }
            case "chestplate" -> {
                kit.setChestplate(hand.clone());
                sender.sendMessage(Component.text("✓ Chestplate set!", NamedTextColor.GREEN));
            }
            case "leggings" -> {
                kit.setLeggings(hand.clone());
                sender.sendMessage(Component.text("✓ Leggings set!", NamedTextColor.GREEN));
            }
            case "boots" -> {
                kit.setBoots(hand.clone());
                sender.sendMessage(Component.text("✓ Boots set!", NamedTextColor.GREEN));
            }
            default -> {
                sender.sendMessage(Component.text("⚠ Invalid armor type! Use: helmet, chestplate, leggings, or boots", NamedTextColor.RED));
                return;
            }
        }

        plugin.getKitManager().updateKit(kit);
    }

    private void handleEditPermission(CommandSender sender, Kit kit, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text()
                .append(Component.text("Usage: /kits edit " + kit.getId() + " perm <permission|clear>\n", NamedTextColor.RED))
                .append(Component.text("Example: /kits edit " + kit.getId() + " perm kit.legendary", NamedTextColor.GRAY))
                .append(Component.text("Use 'clear' to allow all players.", NamedTextColor.DARK_GRAY))
                .build());
            return;
        }

        String perm = args[3];
        if (perm.equalsIgnoreCase("clear")) {
            kit.getAllowedPermissions().clear();
            plugin.getKitManager().updateKit(kit);
            sender.sendMessage(Component.text("✓ Permissions cleared - all players can use this kit!", NamedTextColor.GREEN));
        } else {
            if (!kit.getAllowedPermissions().contains(perm)) {
                kit.getAllowedPermissions().add(perm);
                plugin.getKitManager().updateKit(kit);
            }
            sender.sendMessage(Component.text()
                .append(Component.text("✓ Added permission: ", NamedTextColor.GREEN))
                .append(Component.text(perm, NamedTextColor.GOLD))
                .build());
        }
    }

    private void handleToggleEnabled(CommandSender sender, Kit kit, boolean enabled) {
        kit.setEnabled(enabled);
        plugin.getKitManager().updateKit(kit);

        sender.sendMessage(Component.text("✓ Kit " + (enabled ? "enabled" : "disabled") + "!", NamedTextColor.GREEN));
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text()
                .append(Component.text("Usage: /kits give <kitid> [player] [clear]\n", NamedTextColor.RED))
                .append(Component.text("Example: /kits give starter", NamedTextColor.GRAY))
                .append(Component.text("Example: /kits give starter Notch true", NamedTextColor.DARK_GRAY))
                .build());
            return;
        }

        String kitId = args[1].toLowerCase();
        Kit kit = plugin.getKitManager().getKit(kitId);

        if (kit == null) {
            sender.sendMessage(Component.text("⚠ Kit '" + kitId + "' not found!", NamedTextColor.RED));
            return;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(Component.text("⚠ Player '" + args[2] + "' not found!", NamedTextColor.RED));
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(Component.text("Specify a player!", NamedTextColor.RED));
            return;
        }

        boolean clear = args.length >= 4 && args[3].equalsIgnoreCase("true");

        if (clear) {
            target.getInventory().clear();
        }

        // Give inventory items
        for (Map.Entry<Integer, ItemStack> entry : kit.getInventory().entrySet()) {
            int slot = entry.getKey();
            if (slot >= 0 && slot < 36) {
                target.getInventory().setItem(slot, entry.getValue().clone());
            }
        }

        // Give armor
        if (kit.getHelmet() != null) target.getInventory().setHelmet(kit.getHelmet().clone());
        if (kit.getChestplate() != null) target.getInventory().setChestplate(kit.getChestplate().clone());
        if (kit.getLeggings() != null) target.getInventory().setLeggings(kit.getLeggings().clone());
        if (kit.getBoots() != null) target.getInventory().setBoots(kit.getBoots().clone());

        sender.sendMessage(Component.text()
            .append(Component.text("✓ Gave kit '", NamedTextColor.GREEN))
            .append(Component.text(kit.getName(), NamedTextColor.GOLD))
            .append(Component.text("' to ", NamedTextColor.GREEN))
            .append(Component.text(target.getName(), NamedTextColor.YELLOW))
            .append(Component.text(clear ? " (inventory cleared)" : "", NamedTextColor.GRAY))
            .build());

        target.sendMessage(Component.text()
            .append(Component.text("✓ You received the kit: ", NamedTextColor.GREEN))
            .append(Component.text(kit.getName(), NamedTextColor.GOLD))
            .build());
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage(Component.text("╔════════════════════════════════════════╗", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("              ALL KITS", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("╚════════════════════════════════════════╝", NamedTextColor.GOLD));

        for (Kit kit : plugin.getKitManager().getKits().values()) {
            NamedTextColor statusColor = kit.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED;
            String status = kit.isEnabled() ? "✓" : "✗";

            String cooldown = kit.getCooldownSeconds() > 0 ? " | ⏱ " + formatMillis(kit.getCooldownSeconds() * 1000) : "";
            String perm = kit.getAllowedPermissions().isEmpty() ? " | All can use" : " | Restricted";

            sender.sendMessage(Component.text()
                .append(Component.text(status + " ", statusColor))
                .append(Component.text(kit.getId(), NamedTextColor.GOLD))
                .append(Component.text(" - " + kit.getName(), NamedTextColor.WHITE))
                .append(Component.text(cooldown + perm, NamedTextColor.DARK_GRAY))
                .hoverEvent(HoverEvent.showText(Component.text()
                    .append(Component.text(kit.getName(), NamedTextColor.GOLD))
                    .append(Component.text("\n" + kit.getDescription(), NamedTextColor.GRAY))
                    .append(Component.text("\nCategory: " + kit.getCategory(), NamedTextColor.DARK_GRAY))
                    .append(Component.text("\nItems: " + kit.getInventory().size(), NamedTextColor.DARK_GRAY))
                    .build()))
                .clickEvent(ClickEvent.suggestCommand("/kits edit " + kit.getId()))
                .build());
        }

        sender.sendMessage(Component.text("\nClick a kit or use /kits edit <id> to modify", NamedTextColor.GRAY));
    }

    private void handleCooldown(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text()
                .append(Component.text("Usage: /kits cooldown <player> [clear [kitid]]\n", NamedTextColor.RED))
                .append(Component.text("Example: /kits cooldown Notch", NamedTextColor.GRAY))
                .append(Component.text("Example: /kits cooldown Notch clear", NamedTextColor.DARK_GRAY))
                .build());
            return;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            sender.sendMessage(Component.text("⚠ Player '" + targetName + "' not found!", NamedTextColor.RED));
            return;
        }

        if (args.length >= 3 && args[2].equalsIgnoreCase("clear")) {
            if (args.length >= 4) {
                String kitId = args[3].toLowerCase();
                plugin.getPlayerDataManager().getPlayerData(target.getUniqueId()).clearKitCooldown(kitId);
                plugin.getPlayerDataManager().saveAll();
                sender.sendMessage(Component.text()
                    .append(Component.text("✓ Cleared ", NamedTextColor.GREEN))
                    .append(Component.text(kitId, NamedTextColor.GOLD))
                    .append(Component.text(" cooldown for " + target.getName(), NamedTextColor.GREEN))
                    .build());
            } else {
                plugin.getPlayerDataManager().clearCooldowns(target.getUniqueId());
                sender.sendMessage(Component.text()
                    .append(Component.text("✓ Cleared all cooldowns for ", NamedTextColor.GREEN))
                    .append(Component.text(target.getName(), NamedTextColor.GOLD))
                    .build());
            }
            return;
        }

        // Show cooldowns for target
        sender.sendMessage(Component.text("Cooldowns for " + target.getName() + ":", NamedTextColor.YELLOW));

        boolean any = false;
        for (Kit kit : plugin.getKitManager().getKits().values()) {
            long cooldown = plugin.getPlayerDataManager().getKitCooldown(target.getUniqueId(), kit.getId());
            if (cooldown > System.currentTimeMillis()) {
                any = true;
                long remaining = cooldown - System.currentTimeMillis();
                sender.sendMessage(Component.text()
                    .append(Component.text("  " + kit.getName() + ": ", NamedTextColor.GRAY))
                    .append(Component.text(formatMillis(remaining), NamedTextColor.YELLOW))
                    .build());
            }
        }
        if (!any) {
            sender.sendMessage(Component.text("  (no active cooldowns)", NamedTextColor.DARK_GRAY));
        }
    }

    private void handleCategory(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Categories:", NamedTextColor.YELLOW));
            for (String cat : plugin.getKitManager().getCategories()) {
                sender.sendMessage(Component.text("  ◆ " + cat, NamedTextColor.GRAY));
            }
            return;
        }

        String category = args[1];
        sender.sendMessage(Component.text("Kits in " + category + ":", NamedTextColor.YELLOW));

        List<Kit> kits = plugin.getKitManager().getKitsByCategory(category);
        if (kits.isEmpty()) {
            sender.sendMessage(Component.text("  (none)", NamedTextColor.DARK_GRAY));
            return;
        }
        for (Kit kit : kits) {
            sender.sendMessage(Component.text("  ◆ " + kit.getName(), NamedTextColor.GOLD));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("╔════════════════════════════════════════╗", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("           KITS ADMIN COMMANDS", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("╚════════════════════════════════════════╝", NamedTextColor.GOLD));

        String[][] commands = {
            {"create <id> <name>", "Create a new kit"},
            {"delete <id>", "Delete a kit"},
            {"edit <id>", "Edit kit settings (opens menu)"},
            {"give <id> [player] [clear]", "Give a kit to a player"},
            {"list", "List all kits"},
            {"cooldown <player> [clear [kitid]]", "Manage cooldowns"},
            {"category [name]", "List kits by category"},
            {"reload", "Reload configuration"}
        };

        for (String[] cmd : commands) {
            sender.sendMessage(Component.text()
                .append(Component.text("/kits " + cmd[0], NamedTextColor.YELLOW))
                .append(Component.text(" - " + cmd[1], NamedTextColor.GRAY))
                .build());
        }
    }

    private void sendEditHelp(CommandSender sender) {
        sender.sendMessage(Component.text("╔════════════════════════════════════════╗", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("           KIT EDIT COMMANDS", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("╚════════════════════════════════════════╝", NamedTextColor.GOLD));

        String[][] options = {
            {"name <newname>", "Change kit name"},
            {"desc <description>", "Set description"},
            {"cooldown <seconds>", "Set cooldown time"},
            {"category <name>", "Set category"},
            {"icon", "Set icon (hold item)"},
            {"add", "Add held item to kit"},
            {"remove <slot>", "Remove item from slot"},
            {"armor <helmet|chestplate|leggings|boots>", "Set armor (hold item)"},
            {"perm <permission|clear>", "Set required permission"},
            {"enable | disable", "Toggle kit availability"},
            {"clear", "Clear all kit items"},
            {"save", "Save changes"}
        };

        for (String[] opt : options) {
            sender.sendMessage(Component.text()
                .append(Component.text(opt[0], NamedTextColor.GOLD))
                .append(Component.text(" - " + opt[1], NamedTextColor.GRAY))
                .build());
        }
    }

    private void sendEditMenu(CommandSender sender, Kit kit) {
        sender.sendMessage(Component.text("╔════════════════════════════════════════╗", NamedTextColor.GOLD));
        sender.sendMessage(Component.text()
            .append(Component.text("         EDITING KIT: ", NamedTextColor.YELLOW))
            .append(Component.text(kit.getName(), NamedTextColor.GOLD))
            .build());
        sender.sendMessage(Component.text("╚════════════════════════════════════════╝", NamedTextColor.GOLD));

        sender.sendMessage(Component.text()
            .append(Component.text("Name: ", NamedTextColor.GRAY))
            .append(Component.text(kit.getName(), NamedTextColor.GOLD))
            .build());

        sender.sendMessage(Component.text()
            .append(Component.text("Description: ", NamedTextColor.GRAY))
            .append(Component.text(kit.getDescription().isEmpty() ? "None" : kit.getDescription(), NamedTextColor.DARK_GRAY))
            .build());

        sender.sendMessage(Component.text()
            .append(Component.text("Category: ", NamedTextColor.GRAY))
            .append(Component.text(kit.getCategory(), NamedTextColor.GOLD))
            .build());

        sender.sendMessage(Component.text()
            .append(Component.text("Cooldown: ", NamedTextColor.GRAY))
            .append(Component.text(formatMillis(kit.getCooldownSeconds() * 1000), NamedTextColor.YELLOW))
            .build());

        sender.sendMessage(Component.text()
            .append(Component.text("Status: ", NamedTextColor.GRAY))
            .append(Component.text(kit.isEnabled() ? "✓ Enabled" : "✗ Disabled",
                kit.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED))
            .build());

        int itemCount = kit.getInventory().size()
            + (kit.getHelmet() != null ? 1 : 0)
            + (kit.getChestplate() != null ? 1 : 0)
            + (kit.getLeggings() != null ? 1 : 0)
            + (kit.getBoots() != null ? 1 : 0);

        sender.sendMessage(Component.text()
            .append(Component.text("Items: ", NamedTextColor.GRAY))
            .append(Component.text(String.valueOf(itemCount), NamedTextColor.GOLD))
            .append(Component.text(" (use 'add' while holding items)", NamedTextColor.DARK_GRAY))
            .build());

        sender.sendMessage(Component.text()
            .append(Component.text("Permissions: ", NamedTextColor.GRAY))
            .append(Component.text(kit.getAllowedPermissions().isEmpty() ? "All players" : String.join(", ", kit.getAllowedPermissions()), NamedTextColor.DARK_GRAY))
            .build());

        sender.sendMessage(Component.text(
            "➤ Type /kits edit " + kit.getId() + " <option> to edit", NamedTextColor.YELLOW));
    }

    private String formatMillis(long millis) {
        if (millis <= 0) return "None";

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
        String[] editOptions = {"name", "desc", "description", "cooldown", "category", "icon", "add", "remove", "armor", "perm", "permission", "enable", "disable", "clear", "save"};
        String[] armorTypes = {"helmet", "chestplate", "leggings", "boots"};

        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "delete", "edit", "give", "list", "cooldown", "category", "reload"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("edit") || args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("give")) {
                completions.addAll(plugin.getKitManager().getKits().keySet());
            } else if (args[0].equalsIgnoreCase("category")) {
                completions.addAll(plugin.getKitManager().getCategories());
            } else if (args[0].equalsIgnoreCase("cooldown")) {
                Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("edit")) {
            completions.addAll(Arrays.asList(editOptions));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("edit") && args[2].equalsIgnoreCase("armor")) {
            completions.addAll(Arrays.asList(armorTypes));
        } else if (args[0].equalsIgnoreCase("give") && args.length == 3) {
            Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            completions.add("true");
            completions.add("false");
        }

        String partial = args[args.length - 1].toLowerCase();
        return completions.stream()
            .distinct()
            .filter(s -> s.toLowerCase().startsWith(partial))
            .collect(Collectors.toList());
    }
}
