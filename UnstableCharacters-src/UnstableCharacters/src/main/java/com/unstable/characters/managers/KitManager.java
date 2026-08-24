package com.unstable.characters.managers;

import com.unstable.characters.UnstableCharacters;
import com.unstable.characters.models.Kit;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class KitManager {

    private final UnstableCharacters plugin;
    private final Map<String, Kit> kits;
    private final File kitsFile;
    private final File kitsDataFolder;

    // Note: "silence" and "bolt" are real vanilla 1.21.1 trim patterns
    // (matching the Unstable SMP lore trims), so no fallback mapping is needed.

    /** Trim material per faction/lore pattern. */
    private static final Map<String, TrimMaterial> TRIM_MATERIALS = Map.ofEntries(
            Map.entry("eye", TrimMaterial.GOLD),
            Map.entry("coast", TrimMaterial.GOLD),
            Map.entry("sentry", TrimMaterial.GOLD),
            Map.entry("silence", TrimMaterial.NETHERITE),
            Map.entry("host", TrimMaterial.QUARTZ),
            Map.entry("raiser", TrimMaterial.IRON)
    );

    public KitManager(UnstableCharacters plugin) {
        this.plugin = plugin;
        this.kits = new LinkedHashMap<>();
        this.kitsFile = new File(plugin.getDataFolder(), "kits.yml");
        this.kitsDataFolder = new File(plugin.getDataFolder(), "kit_data");

        if (!kitsDataFolder.exists()) {
            kitsDataFolder.mkdirs();
        }

        loadKits();
        createDefaultKits();
    }

    private void loadKits() {
        if (!kitsFile.exists()) {
            try {
                kitsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create kits.yml: " + e.getMessage());
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(kitsFile);
        ConfigurationSection section = config.getConfigurationSection("kits");

        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection kitSection = section.getConfigurationSection(id);
                if (kitSection != null) {
                    Kit kit = new Kit(id, kitSection.getString("name", id));
                    kit.setDescription(kitSection.getString("description", ""));
                    kit.setCooldownSeconds(kitSection.getLong("cooldown", 0));
                    kit.setCategory(kitSection.getString("category", "General"));
                    kit.setEnabled(kitSection.getBoolean("enabled", true));
                    kit.setAllowedPermissions(kitSection.getStringList("permissions"));

                    if (kitSection.contains("icon")) {
                        ItemStack icon = loadItemStack(kitSection.getString("icon"));
                        if (icon != null) {
                            kit.setIcon(icon);
                        }
                    }

                    File invFile = new File(kitsDataFolder, id + ".dat");
                    if (invFile.exists()) {
                        loadKitInventory(kit, invFile);
                    }

                    kits.put(id.toLowerCase(), kit);
                }
            }
        }
    }

    private void loadKitInventory(Kit kit, File file) {
        try (FileInputStream fis = new FileInputStream(file);
             BukkitObjectInputStream ois = new BukkitObjectInputStream(fis)) {

            byte helmetData = ois.readByte();
            byte chestplateData = ois.readByte();
            byte leggingsData = ois.readByte();
            byte bootsData = ois.readByte();

            if (helmetData == 1) kit.setHelmet((ItemStack) ois.readObject());
            if (chestplateData == 1) kit.setChestplate((ItemStack) ois.readObject());
            if (leggingsData == 1) kit.setLeggings((ItemStack) ois.readObject());
            if (bootsData == 1) kit.setBoots((ItemStack) ois.readObject());

            int size = ois.readInt();
            for (int i = 0; i < size; i++) {
                int slot = ois.readInt();
                ItemStack item = (ItemStack) ois.readObject();
                kit.addItem(slot, item);
            }

        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().warning("Could not load kit inventory for " + kit.getId() + ": " + e.getMessage());
        }
    }

    private void saveKitInventory(Kit kit) {
        File file = new File(kitsDataFolder, kit.getId() + ".dat");
        try (FileOutputStream fos = new FileOutputStream(file);
             BukkitObjectOutputStream oos = new BukkitObjectOutputStream(fos)) {

            oos.writeByte(kit.getHelmet() != null ? 1 : 0);
            oos.writeByte(kit.getChestplate() != null ? 1 : 0);
            oos.writeByte(kit.getLeggings() != null ? 1 : 0);
            oos.writeByte(kit.getBoots() != null ? 1 : 0);

            if (kit.getHelmet() != null) oos.writeObject(kit.getHelmet());
            if (kit.getChestplate() != null) oos.writeObject(kit.getChestplate());
            if (kit.getLeggings() != null) oos.writeObject(kit.getLeggings());
            if (kit.getBoots() != null) oos.writeObject(kit.getBoots());

            Map<Integer, ItemStack> inv = kit.getInventory();
            oos.writeInt(inv.size());
            for (Map.Entry<Integer, ItemStack> entry : inv.entrySet()) {
                oos.writeInt(entry.getKey());
                oos.writeObject(entry.getValue());
            }

        } catch (IOException e) {
            plugin.getLogger().warning("Could not save kit inventory for " + kit.getId() + ": " + e.getMessage());
        }
    }

    private void createDefaultKits() {
        if (kits.isEmpty()) {
            plugin.getLogger().info("Creating Unstable SMP character kits with armor trims...");

            // =============================================
            // PROTAGONIST KITS
            // =============================================
            createWemmbuKit();
            createFlameFragsKit();
            createSpokeKit();
            createParrotKit();

            // =============================================
            // ALLY KITS
            // =============================================
            createWifiesKit();
            createMapiccKit();
            createEggchanKit();
            createLomedyKit();

            // =============================================
            // ANTAGONIST KITS (With Faction Trims)
            // =============================================
            createPrinceZamKit();
            createClownPierceKit();
            createJamatoKit();
            createAshswaggKit();
            createJadenKit();

            // =============================================
            // SAPARATA & CINDERCREST KINGDOM
            // =============================================
            createSaparataKit();

            // =============================================
            // NULL ARMY KITS
            // =============================================
            createNullArmyKit();
            createNullHunterKit();

            // =============================================
            // MAFIA KITS
            // =============================================
            createMafiaSoldierKit();
            createMafiaDiamondKit();
            createMafiaGoldKit();

            // =============================================
            // MIST CIVILIZATION
            // =============================================
            createMistCivilizationKit();

            // =============================================
            // KINGDOM OF THE CAVES (Arachnid)
            // =============================================
            createKingdomOfTheCavesKit();

            // =============================================
            // Support & Neutral Kits
            // =============================================
            createMinuteTechKit();
            createManePearKit();
            createPurpledKit();
            createShoebillyKit();
            createLettuceKKit();

            // =============================================
            // Standard Kits
            // =============================================
            createStandardKits();

            saveAll();
            plugin.getLogger().info("Created " + kits.size() + " Unstable SMP character kits!");
        }
    }

    // =============================================
    // PROTAGONIST KITS
    // =============================================

    private void createWemmbuKit() {
        Kit kit = new Kit("wemmbu", "§bWemmbu's Loadout");
        kit.setDescription("The Architect & Empire Builder - Maces, Orbital Strike Cannons, TNT Minecarts");
        kit.setCooldownSeconds(7200);
        kit.setCategory("Protagonists");
        kit.setIcon(createItem(Material.NETHERITE_HELMET, "Wemmbu's Helmet"));

        kit.setHelmet(createNetheriteHelmet(
            Enchantment.PROTECTION, 4,
            Enchantment.RESPIRATION, 3,
            Enchantment.AQUA_AFFINITY, 1,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createNetheriteChestplate(
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setLeggings(createNetheriteLeggings(
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3,
            Enchantment.THORNS, 3
        ));
        kit.setBoots(createNetheriteBoots(
            Enchantment.PROTECTION, 4,
            Enchantment.FROST_WALKER, 2,
            Enchantment.SOUL_SPEED, 3,
            Enchantment.FEATHER_FALLING, 4,
            Enchantment.UNBREAKING, 3
        ));

        // Crucible (Mace)
        kit.addItem(0, createMace("Crucible",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3
        ));
        // Gambit (Mace)
        kit.addItem(1, createMace("Gambit",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3
        ));
        // The Flame (Netherite Sword)
        kit.addItem(2, createNetheriteSword("The Flame",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3,
            Enchantment.KNOCKBACK, 1,
            Enchantment.LOOTING, 3,
            Enchantment.SWEEPING_EDGE, 3
        ));
        kit.addItem(3, createNetheriteAxe("Inferno",
            Enchantment.SHARPNESS, 5,
            Enchantment.EFFICIENCY, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(4, createTrident("Trident",
            Enchantment.LOYALTY, 3,
            Enchantment.SHARPNESS, 5,
            Enchantment.RIPTIDE, 3
        ));

        // Orbital Strike Cannon triggers
        kit.addItem(5, createFishingRod("Nuke Shot"));
        kit.addItem(6, createFishingRod("Stab Shot"));
        kit.addItem(7, createElytra());
        kit.addItem(8, createShield());

        // TNT Minecarts (Signature move)
        kit.addItem(27, new ItemStack(Material.TNT_MINECART, 16));
        kit.addItem(28, new ItemStack(Material.TNT_MINECART, 16));
        kit.addItem(29, new ItemStack(Material.TNT, 64));
        kit.addItem(30, new ItemStack(Material.TNT, 64));

        kit.addItem(31, new ItemStack(Material.COBBLESTONE, 128));
        kit.addItem(32, new ItemStack(Material.COOKED_BEEF, 64));
        kit.addItem(33, new ItemStack(Material.GOLDEN_APPLE, 16));
        kit.addItem(34, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 4));

        kits.put("wemmbu", kit);
    }

    private void createFlameFragsKit() {
        Kit kit = new Kit("flamefrags", "§cFlameFrags' Loadout");
        kit.setDescription("The Combat Elite & Lone Wolf - Fire Aspect, Orbital Dog Cannon");
        kit.setCooldownSeconds(7200);
        kit.setCategory("Protagonists");
        kit.setIcon(createItem(Material.NETHERITE_HELMET, "FlameFrags' Helmet"));

        kit.setHelmet(createNetheriteHelmet(
            Enchantment.PROTECTION, 4,
            Enchantment.RESPIRATION, 3,
            Enchantment.AQUA_AFFINITY, 1,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createNetheriteChestplate(
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setLeggings(createNetheriteLeggings(
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createNetheriteBoots(
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.SOUL_SPEED, 3,
            Enchantment.FEATHER_FALLING, 4,
            Enchantment.UNBREAKING, 3
        ));

        // The Flame (Signature Sword)
        kit.addItem(0, createNetheriteSword("The Flame",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3,
            Enchantment.KNOCKBACK, 1,
            Enchantment.LOOTING, 3,
            Enchantment.SWEEPING_EDGE, 3
        ));
        // Incinerator (Mace)
        kit.addItem(1, createMace("Incinerator",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3
        ));
        // Fragger (Netherite Sword)
        kit.addItem(2, createNetheriteSword("Fragger",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3,
            Enchantment.LOOTING, 3,
            Enchantment.SWEEPING_EDGE, 3
        ));
        // Inferno (Netherite Axe)
        kit.addItem(3, createNetheriteAxe("Inferno",
            Enchantment.SHARPNESS, 5,
            Enchantment.EFFICIENCY, 5,
            Enchantment.UNBREAKING, 3
        ));
        // Carbonizer (Spear)
        kit.addItem(4, createSpear("Carbonizer",
            Enchantment.SHARPNESS, 5,
            Enchantment.KNOCKBACK, 2,
            Enchantment.UNBREAKING, 3
        ));
        // Wolf Rod - Triggers ODC
        kit.addItem(5, createFishingRod("Wolf Rod"));
        // Pyromania (Crossbow)
        kit.addItem(6, createCrossbow("Pyromania",
            Enchantment.PIERCING, 4,
            Enchantment.QUICK_CHARGE, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(7, createShield());
        kit.addItem(8, createElytra());

        kit.addItem(27, new ItemStack(Material.FIRE_CHARGE, 64));
        kit.addItem(28, new ItemStack(Material.BLAZE_POWDER, 32));
        kit.addItem(29, new ItemStack(Material.LAVA_BUCKET, 2));
        kit.addItem(30, new ItemStack(Material.COOKED_BEEF, 64));
        kit.addItem(31, new ItemStack(Material.GOLDEN_APPLE, 16));

        kits.put("flamefrags", kit);
    }

    private void createSpokeKit() {
        Kit kit = new Kit("spokeishere", "§eSpokeIsHere's Loadout");
        kit.setDescription("The Survival Specialist - Redstone Trim Godset, Trap Master");
        kit.setCooldownSeconds(7200);
        kit.setCategory("Protagonists");
        kit.setIcon(createItem(Material.NETHERITE_HELMET, "Spoke's Helmet"));

        // Spoke's Redstone Trim Godset (Vex/Snout/Coast/Eye patterns, redstone material)
        kit.setHelmet(createTrimmedNetheriteHelmet("vex", TrimMaterial.REDSTONE,
            Enchantment.PROTECTION, 4,
            Enchantment.RESPIRATION, 3,
            Enchantment.AQUA_AFFINITY, 1,
            Enchantment.UNBREAKING, 3,
            Enchantment.BLAST_PROTECTION, 4,
            Enchantment.PROJECTILE_PROTECTION, 4,
            Enchantment.FIRE_PROTECTION, 4
        ));
        kit.setChestplate(createTrimmedNetheriteChestplate("snout", TrimMaterial.REDSTONE,
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3,
            Enchantment.BLAST_PROTECTION, 4,
            Enchantment.PROJECTILE_PROTECTION, 4,
            Enchantment.FIRE_PROTECTION, 4
        ));
        kit.setLeggings(createTrimmedNetheriteLeggings("coast", TrimMaterial.REDSTONE,
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.BLAST_PROTECTION, 4,
            Enchantment.PROJECTILE_PROTECTION, 4,
            Enchantment.FIRE_PROTECTION, 4
        ));
        kit.setBoots(createTrimmedNetheriteBoots("eye", TrimMaterial.REDSTONE,
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.SOUL_SPEED, 3,
            Enchantment.BLAST_PROTECTION, 4,
            Enchantment.PROJECTILE_PROTECTION, 4,
            Enchantment.FIRE_PROTECTION, 4
        ));

        kit.addItem(0, createNetheriteSword("Ctrl",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3,
            Enchantment.KNOCKBACK, 1,
            Enchantment.LOOTING, 3,
            Enchantment.SWEEPING_EDGE, 3
        ));
        kit.addItem(1, createNetheriteSword("Alt",
            Enchantment.SHARPNESS, 5,
            Enchantment.UNBREAKING, 3,
            Enchantment.LOOTING, 3,
            Enchantment.SWEEPING_EDGE, 3
        ));
        kit.addItem(2, createNetheriteAxe("Delete",
            Enchantment.SHARPNESS, 5,
            Enchantment.EFFICIENCY, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(3, createNetheritePickaxe("Escape",
            Enchantment.EFFICIENCY, 5,
            Enchantment.SILK_TOUCH, 1,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(4, createShield());
        kit.addItem(5, createElytra());

        // TRAP SUPPLIES (Spoke's specialty)
        kit.addItem(6, new ItemStack(Material.TNT, 128));
        kit.addItem(7, new ItemStack(Material.TNT_MINECART, 32));
        kit.addItem(8, new ItemStack(Material.RESPAWN_ANCHOR, 8));

        kit.addItem(27, new ItemStack(Material.HOPPER, 32));
        kit.addItem(28, new ItemStack(Material.HOPPER_MINECART, 16));
        kit.addItem(29, new ItemStack(Material.OBSERVER, 32));
        kit.addItem(30, new ItemStack(Material.PISTON, 32));
        kit.addItem(31, new ItemStack(Material.STICKY_PISTON, 32));
        kit.addItem(32, new ItemStack(Material.REDSTONE_BLOCK, 64));
        kit.addItem(33, new ItemStack(Material.REDSTONE, 128));
        kit.addItem(34, new ItemStack(Material.REPEATER, 32));
        kit.addItem(35, new ItemStack(Material.COBWEB, 64));

        kit.addItem(26, new ItemStack(Material.COOKED_BEEF, 64));
        kit.addItem(17, new ItemStack(Material.GOLDEN_APPLE, 16));

        kits.put("spokeishere", kit);
    }

    private void createParrotKit() {
        Kit kit = new Kit("parrotx2", "§aParrotX2's Loadout");
        kit.setDescription("The Master Hunter & Strategist - Silence Trim, TNT Minecarts, End Crystals");
        kit.setCooldownSeconds(7200);
        kit.setCategory("Protagonists");
        kit.setIcon(createItem(Material.DIAMOND_HELMET, "Parrot's Helmet"));

        // Parrot's Diamond Armor (Silence trim - legend status)
        kit.setHelmet(createTrimmedDiamondHelmet("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.RESPIRATION, 3,
            Enchantment.AQUA_AFFINITY, 1,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createTrimmedDiamondChestplate("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setLeggings(createTrimmedDiamondLeggings("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createTrimmedDiamondBoots("host",
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.SOUL_SPEED, 3,
            Enchantment.FEATHER_FALLING, 4,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createDiamondSword("Diamond Sword",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3,
            Enchantment.KNOCKBACK, 1,
            Enchantment.LOOTING, 3,
            Enchantment.SWEEPING_EDGE, 3
        ));
        kit.addItem(1, createDiamondAxe("Diamond Axe",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createBow("Bow",
            Enchantment.POWER, 5,
            Enchantment.PUNCH, 2,
            Enchantment.FLAME, 1,
            Enchantment.INFINITY, 1
        ));
        kit.addItem(3, createShield());
        kit.addItem(4, createElytra());

        // TNT Minecarts (Parrot's signature)
        kit.addItem(5, new ItemStack(Material.TNT_MINECART, 32));
        kit.addItem(6, new ItemStack(Material.TNT_MINECART, 32));
        kit.addItem(7, new ItemStack(Material.TNT, 128));
        kit.addItem(8, new ItemStack(Material.TNT, 128));

        kit.addItem(27, new ItemStack(Material.END_CRYSTAL, 8));
        kit.addItem(28, new ItemStack(Material.RESPAWN_ANCHOR, 8));
        kit.addItem(29, new ItemStack(Material.GLOWSTONE, 64));
        kit.addItem(30, new ItemStack(Material.HOPPER, 32));
        kit.addItem(31, new ItemStack(Material.HOPPER_MINECART, 16));
        kit.addItem(32, new ItemStack(Material.OBSERVER, 32));
        kit.addItem(33, new ItemStack(Material.PISTON, 32));
        kit.addItem(34, new ItemStack(Material.COOKED_BEEF, 64));
        kit.addItem(35, new ItemStack(Material.GOLDEN_APPLE, 16));

        kits.put("parrotx2", kit);
    }

    // =============================================
    // ALLY KITS
    // =============================================

    private void createWifiesKit() {
        Kit kit = new Kit("wifies", "§dWifies' Loadout");
        kit.setDescription("Highly influential ally - Flow trim, versatile fighter");
        kit.setCooldownSeconds(5400);
        kit.setCategory("Allies");
        kit.setIcon(createItem(Material.NETHERITE_HELMET, "Wifies' Helmet"));

        // Flow trim
        kit.setHelmet(createTrimmedNetheriteHelmet("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.RESPIRATION, 3,
            Enchantment.AQUA_AFFINITY, 1,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createTrimmedNetheriteChestplate("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setLeggings(createTrimmedNetheriteLeggings("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createTrimmedNetheriteBoots("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.SOUL_SPEED, 3,
            Enchantment.FEATHER_FALLING, 4,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createNetheriteSword("Sword",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3,
            Enchantment.LOOTING, 3,
            Enchantment.SWEEPING_EDGE, 3
        ));
        kit.addItem(1, createNetheriteAxe("Axe",
            Enchantment.SHARPNESS, 5,
            Enchantment.EFFICIENCY, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createBow("Bow",
            Enchantment.POWER, 5,
            Enchantment.PUNCH, 2,
            Enchantment.FLAME, 1
        ));
        kit.addItem(3, createShield());
        kit.addItem(4, createElytra());
        kit.addItem(5, new ItemStack(Material.TNT, 64));
        kit.addItem(6, new ItemStack(Material.COOKED_BEEF, 64));
        kit.addItem(7, new ItemStack(Material.GOLDEN_APPLE, 8));

        kits.put("wifies", kit);
    }

    private void createMapiccKit() {
        Kit kit = new Kit("mapicc", "§9Mapicc's Loadout");
        kit.setDescription("The Master Builder - Large-scale projects and defense");
        kit.setCooldownSeconds(5400);
        kit.setCategory("Allies");
        kit.setIcon(createItem(Material.NETHERITE_HELMET, "Mapicc's Helmet"));

        kit.setHelmet(createNetheriteHelmet(
            Enchantment.PROTECTION, 4,
            Enchantment.RESPIRATION, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createNetheriteChestplate(
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setLeggings(createNetheriteLeggings(
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createNetheriteBoots(
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createNetheritePickaxe("Builder Pick",
            Enchantment.EFFICIENCY, 5,
            Enchantment.SILK_TOUCH, 1,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(1, createNetheriteAxe("Builder Axe",
            Enchantment.EFFICIENCY, 5,
            Enchantment.SILK_TOUCH, 1,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createNetheriteSword("Sword",
            Enchantment.SHARPNESS, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(3, createShield());
        kit.addItem(4, createElytra());

        // Builder supplies
        kit.addItem(5, new ItemStack(Material.OAK_PLANKS, 512));
        kit.addItem(6, new ItemStack(Material.COBBLESTONE, 512));
        kit.addItem(7, new ItemStack(Material.STONE_BRICKS, 256));
        kit.addItem(8, new ItemStack(Material.GLASS, 128));

        kit.addItem(27, new ItemStack(Material.IRON_BLOCK, 128));
        kit.addItem(28, new ItemStack(Material.GOLD_BLOCK, 64));
        kit.addItem(29, new ItemStack(Material.DIAMOND_BLOCK, 32));
        kit.addItem(30, new ItemStack(Material.EMERALD_BLOCK, 32));
        kit.addItem(31, new ItemStack(Material.LAPIS_BLOCK, 64));
        kit.addItem(32, new ItemStack(Material.REDSTONE_BLOCK, 64));
        kit.addItem(33, new ItemStack(Material.TORCH, 128));
        kit.addItem(34, new ItemStack(Material.RESPAWN_ANCHOR, 4));
        kit.addItem(35, new ItemStack(Material.COOKED_BEEF, 64));

        kits.put("mapicc", kit);
    }

    private void createEggchanKit() {
        Kit kit = new Kit("eggchan", "§fEggchan's Loadout");
        kit.setDescription("The Angel Hybrid - Silence trim, loyal ally of Wemmbu");
        kit.setCooldownSeconds(5400);
        kit.setCategory("Allies");
        kit.setIcon(createItem(Material.NETHERITE_HELMET, "Eggchan's Helmet"));

        // Silence trim - legend status
        kit.setHelmet(createTrimmedNetheriteHelmet("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.RESPIRATION, 3,
            Enchantment.AQUA_AFFINITY, 1,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createTrimmedNetheriteChestplate("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setLeggings(createTrimmedNetheriteLeggings("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createTrimmedNetheriteBoots("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.FEATHER_FALLING, 4,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createNetheriteSword("Sword",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3,
            Enchantment.LOOTING, 3
        ));
        kit.addItem(1, createNetheriteAxe("Axe",
            Enchantment.SHARPNESS, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createBow("Bow",
            Enchantment.POWER, 5,
            Enchantment.PUNCH, 2
        ));
        kit.addItem(3, createShield());
        kit.addItem(4, createElytra());
        kit.addItem(5, new ItemStack(Material.TNT, 32));
        kit.addItem(6, new ItemStack(Material.COOKED_BEEF, 64));
        kit.addItem(7, new ItemStack(Material.GOLDEN_APPLE, 8));

        kits.put("eggchan", kit);
    }

    private void createLomedyKit() {
        Kit kit = new Kit("lomedy", "§aLomedy's Loadout");
        kit.setDescription("Human ally - Torchflower Duo partner of FlameFrags");
        kit.setCooldownSeconds(5400);
        kit.setCategory("Allies");
        kit.setIcon(createItem(Material.NETHERITE_HELMET, "Lomedy's Helmet"));

        kit.setHelmet(createNetheriteHelmet(
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createNetheriteChestplate(
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setLeggings(createNetheriteLeggings(
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createNetheriteBoots(
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.FEATHER_FALLING, 4,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createNetheriteSword("Sword",
            Enchantment.SHARPNESS, 5,
            Enchantment.UNBREAKING, 3,
            Enchantment.LOOTING, 3
        ));
        kit.addItem(1, createNetheritePickaxe("Pickaxe",
            Enchantment.EFFICIENCY, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createNetheriteAxe("Axe",
            Enchantment.SHARPNESS, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(3, createShield());
        kit.addItem(4, createElytra());
        kit.addItem(5, new ItemStack(Material.FLOWER_POT, 16));
        kit.addItem(6, new ItemStack(Material.TORCHFLOWER, 32));
        kit.addItem(7, new ItemStack(Material.COOKED_BEEF, 64));
        kit.addItem(8, new ItemStack(Material.GOLDEN_APPLE, 8));

        kits.put("lomedy", kit);
    }

    // =============================================
    // ANTAGONIST KITS (Zam Empire Gold Trim)
    // =============================================

    private void createPrinceZamKit() {
        Kit kit = new Kit("princezam", "§4PrinceZam's Loadout");
        kit.setDescription("Leader of Zam Empire - Gold trim, Zam Blade, Army Commander");
        kit.setCooldownSeconds(7200);
        kit.setCategory("Antagonists");
        kit.setIcon(createItem(Material.NETHERITE_HELMET, "PrinceZam's Helmet"));

        // Zam Empire Gold Trim (Sentry/Eye/Coast patterns)
        kit.setHelmet(createTrimmedNetheriteHelmet("sentry", TrimMaterial.GOLD,
            Enchantment.PROTECTION, 4,
            Enchantment.RESPIRATION, 3,
            Enchantment.AQUA_AFFINITY, 1,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createTrimmedNetheriteChestplate("eye", TrimMaterial.GOLD,
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3,
            Enchantment.THORNS, 3
        ));
        kit.setLeggings(createTrimmedNetheriteLeggings("sentry", TrimMaterial.GOLD,
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createTrimmedNetheriteBoots("coast", TrimMaterial.GOLD,
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.FEATHER_FALLING, 4,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createNetheriteSword("Zam Blade",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3,
            Enchantment.KNOCKBACK, 2,
            Enchantment.LOOTING, 3,
            Enchantment.SWEEPING_EDGE, 3
        ));
        kit.addItem(1, createNetheriteAxe("Conqueror",
            Enchantment.SHARPNESS, 5,
            Enchantment.EFFICIENCY, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createBow("Royal Bow",
            Enchantment.POWER, 5,
            Enchantment.PUNCH, 2,
            Enchantment.FLAME, 1,
            Enchantment.INFINITY, 1
        ));
        kit.addItem(3, createShield());
        kit.addItem(4, createElytra());

        // Army supplies
        kit.addItem(5, new ItemStack(Material.GOLD_INGOT, 128));
        kit.addItem(6, new ItemStack(Material.IRON_SWORD, 32));
        kit.addItem(7, new ItemStack(Material.IRON_HELMET, 16));
        kit.addItem(8, new ItemStack(Material.IRON_CHESTPLATE, 16));

        kit.addItem(27, new ItemStack(Material.TNT, 128));
        kit.addItem(28, new ItemStack(Material.END_CRYSTAL, 16));
        kit.addItem(29, new ItemStack(Material.COOKED_BEEF, 64));
        kit.addItem(30, new ItemStack(Material.GOLDEN_APPLE, 16));
        kit.addItem(31, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 4));

        kits.put("princezam", kit);
    }

    private void createClownPierceKit() {
        Kit kit = new Kit("clownpierce", "§cClownPierce's Loadout");
        kit.setDescription("Legendary Combatant - Silence trim, major threat to protagonists");
        kit.setCooldownSeconds(7200);
        kit.setCategory("Antagonists");
        kit.setIcon(createItem(Material.NETHERITE_HELMET, "ClownPierce's Helmet"));

        // Silence trim - legend status
        kit.setHelmet(createTrimmedNetheriteHelmet("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.RESPIRATION, 3,
            Enchantment.AQUA_AFFINITY, 1,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createTrimmedNetheriteChestplate("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3,
            Enchantment.THORNS, 3
        ));
        kit.setLeggings(createTrimmedNetheriteLeggings("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3,
            Enchantment.THORNS, 2
        ));
        kit.setBoots(createTrimmedNetheriteBoots("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.SOUL_SPEED, 3,
            Enchantment.FEATHER_FALLING, 4,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createNetheriteSword("Clown Blade",
            Enchantment.SHARPNESS, 6,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3,
            Enchantment.KNOCKBACK, 2,
            Enchantment.LOOTING, 4,
            Enchantment.SWEEPING_EDGE, 3
        ));
        kit.addItem(1, createNetheriteAxe("Carnage",
            Enchantment.SHARPNESS, 6,
            Enchantment.EFFICIENCY, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createMace("Nightmare",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(3, createBow("Death Bow",
            Enchantment.POWER, 6,
            Enchantment.PUNCH, 2,
            Enchantment.FLAME, 1,
            Enchantment.INFINITY, 1
        ));
        kit.addItem(4, createShield());
        kit.addItem(5, createElytra());
        kit.addItem(6, new ItemStack(Material.TNT, 64));
        kit.addItem(7, new ItemStack(Material.COOKED_BEEF, 64));
        kit.addItem(8, new ItemStack(Material.GOLDEN_APPLE, 16));

        kits.put("clownpierce", kit);
    }

    private void createJamatoKit() {
        Kit kit = new Kit("jamatop", "§cJamatoP's Loadout");
        kit.setDescription("Recurring Antagonist - Flow trim, challenged protagonists' dominance");
        kit.setCooldownSeconds(5400);
        kit.setCategory("Antagonists");
        kit.setIcon(createItem(Material.NETHERITE_HELMET, "JamatoP's Helmet"));

        // Flow trim
        kit.setHelmet(createTrimmedNetheriteHelmet("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.RESPIRATION, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createTrimmedNetheriteChestplate("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setLeggings(createTrimmedNetheriteLeggings("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createTrimmedNetheriteBoots("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.FEATHER_FALLING, 4,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createNetheriteSword("Jama Blade",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3,
            Enchantment.LOOTING, 3,
            Enchantment.SWEEPING_EDGE, 3
        ));
        kit.addItem(1, createNetheriteAxe("Destroyer",
            Enchantment.SHARPNESS, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createBow("Bow",
            Enchantment.POWER, 5,
            Enchantment.PUNCH, 2
        ));
        kit.addItem(3, createShield());
        kit.addItem(4, createElytra());
        kit.addItem(5, new ItemStack(Material.TNT, 64));
        kit.addItem(6, new ItemStack(Material.COOKED_BEEF, 64));
        kit.addItem(7, new ItemStack(Material.GOLDEN_APPLE, 8));

        kits.put("jamatop", kit);
    }

    private void createAshswaggKit() {
        Kit kit = new Kit("ashswagg", "§8Ashswagg's Loadout");
        kit.setDescription("Notable Antagonist - Flow trim, high-stakes conflicts and betrayals");
        kit.setCooldownSeconds(5400);
        kit.setCategory("Antagonists");
        kit.setIcon(createItem(Material.NETHERITE_HELMET, "Ashswagg's Helmet"));

        kit.setHelmet(createTrimmedNetheriteHelmet("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.RESPIRATION, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createTrimmedNetheriteChestplate("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setLeggings(createTrimmedNetheriteLeggings("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createTrimmedNetheriteBoots("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.FEATHER_FALLING, 4,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createNetheriteSword("Ashes Blade",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3,
            Enchantment.LOOTING, 3
        ));
        kit.addItem(1, createNetheriteAxe("Ash Axe",
            Enchantment.SHARPNESS, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createBow("Bow",
            Enchantment.POWER, 5,
            Enchantment.PUNCH, 2
        ));
        kit.addItem(3, createShield());
        kit.addItem(4, createElytra());
        kit.addItem(5, new ItemStack(Material.TNT, 32));
        kit.addItem(6, new ItemStack(Material.COOKED_BEEF, 64));
        kit.addItem(7, new ItemStack(Material.GOLDEN_APPLE, 8));

        kits.put("ashswagg", kit);
    }

    private void createJadenKit() {
        Kit kit = new Kit("jadenman", "§6Jaden_MAN's Loadout");
        kit.setDescription("Long-standing figure - Pirate Duo partner, Cannonball Duo");
        kit.setCooldownSeconds(5400);
        kit.setCategory("Antagonists");
        kit.setIcon(createItem(Material.NETHERITE_HELMET, "Jaden's Helmet"));

        kit.setHelmet(createNetheriteHelmet(
            Enchantment.PROTECTION, 4,
            Enchantment.RESPIRATION, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createNetheriteChestplate(
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setLeggings(createNetheriteLeggings(
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createNetheriteBoots(
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.FEATHER_FALLING, 4,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createNetheriteSword("Jaden Blade",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3,
            Enchantment.LOOTING, 3
        ));
        kit.addItem(1, createNetheriteAxe("Pirate Axe",
            Enchantment.SHARPNESS, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createBow("Bow",
            Enchantment.POWER, 5,
            Enchantment.PUNCH, 2
        ));
        kit.addItem(3, createShield());
        kit.addItem(4, createElytra());
        kit.addItem(5, new ItemStack(Material.TNT, 32));
        kit.addItem(6, new ItemStack(Material.COOKED_BEEF, 64));
        kit.addItem(7, new ItemStack(Material.GOLDEN_APPLE, 8));

        kits.put("jadenman", kit);
    }

    // =============================================
    // SAPARATA & CINDERCREST KINGDOM (Silence Trim)
    // =============================================

    private void createSaparataKit() {
        Kit kit = new Kit("saparata", "§6Saparata's Loadout");
        kit.setDescription("Leader of Cindercrest - Silence trim, King of Unstable SMP");
        kit.setCooldownSeconds(7200);
        kit.setCategory("Cindercrest");
        kit.setIcon(createItem(Material.NETHERITE_HELMET, "Saparata's Helmet"));

        // Silence trim - leader legend status
        kit.setHelmet(createTrimmedNetheriteHelmet("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.RESPIRATION, 3,
            Enchantment.AQUA_AFFINITY, 1,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createTrimmedNetheriteChestplate("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3,
            Enchantment.THORNS, 3
        ));
        kit.setLeggings(createTrimmedNetheriteLeggings("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createTrimmedNetheriteBoots("host",
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.FEATHER_FALLING, 4,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createNetheriteSword("King's Blade",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3,
            Enchantment.KNOCKBACK, 2,
            Enchantment.LOOTING, 3,
            Enchantment.SWEEPING_EDGE, 3
        ));
        kit.addItem(1, createNetheriteAxe("Cinder Axe",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createBow("Crown Bow",
            Enchantment.POWER, 5,
            Enchantment.PUNCH, 2,
            Enchantment.FLAME, 1
        ));
        kit.addItem(3, createShield());
        kit.addItem(4, createElytra());

        // Cindercrest supplies
        kit.addItem(5, new ItemStack(Material.TNT, 64));
        kit.addItem(6, new ItemStack(Material.BLAZE_POWDER, 32));
        kit.addItem(7, new ItemStack(Material.LAVA_BUCKET, 4));
        kit.addItem(8, new ItemStack(Material.COOKED_BEEF, 64));

        kit.addItem(27, new ItemStack(Material.GOLDEN_APPLE, 16));
        kit.addItem(28, new ItemStack(Material.TOTEM_OF_UNDYING, 2));
        kit.addItem(29, new ItemStack(Material.ENDER_PEARL, 16));

        kits.put("saparata", kit);
    }

    // =============================================
    // NULL ARMY KITS (Flow Trim)
    // =============================================

    private void createNullArmyKit() {
        Kit kit = new Kit("nullarmy", "§5Null Army Soldier");
        kit.setDescription("Mysterious NULL faction - Flow trim, standard null soldier");
        kit.setCooldownSeconds(5400);
        kit.setCategory("Null");
        kit.setIcon(createItem(Material.NETHERITE_HELMET, "Null Soldier Helmet"));

        // Null flow trim pattern
        kit.setHelmet(createTrimmedNetheriteHelmet("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.RESPIRATION, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createTrimmedNetheriteChestplate("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setLeggings(createTrimmedNetheriteLeggings("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createTrimmedNetheriteBoots("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.SOUL_SPEED, 3,
            Enchantment.FEATHER_FALLING, 4,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createNetheriteSword("Null Blade",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3,
            Enchantment.LOOTING, 3
        ));
        kit.addItem(1, createNetheriteAxe("Void Axe",
            Enchantment.SHARPNESS, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createBow("Void Bow",
            Enchantment.POWER, 5,
            Enchantment.PUNCH, 2
        ));
        kit.addItem(3, createShield());
        kit.addItem(4, createElytra());
        kit.addItem(5, new ItemStack(Material.TNT, 32));
        kit.addItem(6, new ItemStack(Material.COOKED_BEEF, 64));
        kit.addItem(7, new ItemStack(Material.GOLDEN_APPLE, 8));
        kit.addItem(8, new ItemStack(Material.TOTEM_OF_UNDYING, 1));

        kits.put("nullarmy", kit);
    }

    private void createNullHunterKit() {
        Kit kit = new Kit("nullhunter", "§5Null Hunter Squad Leader");
        kit.setDescription("NULL elite - Flow/Raiser trim, squad leader variant");
        kit.setCooldownSeconds(7200);
        kit.setCategory("Null");
        kit.setIcon(createItem(Material.NETHERITE_HELMET, "Null Hunter Helmet"));

        // Null squad leader trim (flow + raiser chestplate)
        kit.setHelmet(createTrimmedNetheriteHelmet("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.RESPIRATION, 3,
            Enchantment.AQUA_AFFINITY, 1,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createTrimmedNetheriteChestplate("raiser",
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3,
            Enchantment.THORNS, 3
        ));
        kit.setLeggings(createTrimmedNetheriteLeggings("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createTrimmedNetheriteBoots("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.SOUL_SPEED, 3,
            Enchantment.FEATHER_FALLING, 4,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createNetheriteSword("Null Hunter Blade",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3,
            Enchantment.KNOCKBACK, 1,
            Enchantment.LOOTING, 3,
            Enchantment.SWEEPING_EDGE, 3
        ));
        kit.addItem(1, createNetheriteAxe("Hunter Axe",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createBow("Hunter Bow",
            Enchantment.POWER, 5,
            Enchantment.PUNCH, 2,
            Enchantment.FLAME, 1
        ));
        kit.addItem(3, createMace("Null Mace",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(4, createShield());
        kit.addItem(5, createElytra());
        kit.addItem(6, new ItemStack(Material.TNT, 64));
        kit.addItem(7, new ItemStack(Material.END_CRYSTAL, 4));
        kit.addItem(8, new ItemStack(Material.COOKED_BEEF, 64));

        kit.addItem(27, new ItemStack(Material.GOLDEN_APPLE, 16));
        kit.addItem(28, new ItemStack(Material.TOTEM_OF_UNDYING, 2));
        kit.addItem(29, new ItemStack(Material.ENDER_PEARL, 16));

        kits.put("nullhunter", kit);
    }

    // =============================================
    // MAFIA KITS (Netherite No Trim)
    // =============================================

    private void createMafiaSoldierKit() {
        Kit kit = new Kit("mafiasoldier", "§8Mafia Soldier");
        kit.setDescription("The Invisible Mafia - Netherite no trim (basic soldier)");
        kit.setCooldownSeconds(3600);
        kit.setCategory("Mafia");
        kit.setIcon(createItem(Material.NETHERITE_HELMET, "Mafia Helmet"));

        // Mafia basic - no trim
        kit.setHelmet(createNetheriteHelmet(
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createNetheriteChestplate(
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setLeggings(createNetheriteLeggings(
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createNetheriteBoots(
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.SOUL_SPEED, 3,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createNetheriteSword("Mafia Blade",
            Enchantment.SHARPNESS, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(1, createNetheriteAxe("Enforcer Axe",
            Enchantment.SHARPNESS, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createBow("Silent Bow",
            Enchantment.POWER, 5,
            Enchantment.PUNCH, 2
        ));
        kit.addItem(3, createShield());
        kit.addItem(4, createElytra());
        kit.addItem(5, new ItemStack(Material.TNT, 16));
        kit.addItem(6, new ItemStack(Material.COOKED_BEEF, 64));
        kit.addItem(7, new ItemStack(Material.GOLDEN_APPLE, 4));

        kits.put("mafiasoldier", kit);
    }

    private void createMafiaDiamondKit() {
        Kit kit = new Kit("mafiadiamond", "§bMafia Diamond Soldier");
        kit.setDescription("Mafia elite - Diamond with sentry/eye trim");
        kit.setCooldownSeconds(5400);
        kit.setCategory("Mafia");
        kit.setIcon(createItem(Material.DIAMOND_HELMET, "Mafia Diamond Helmet"));

        kit.setHelmet(createTrimmedDiamondHelmet("sentry",
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createTrimmedDiamondChestplate("eye",
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setLeggings(createTrimmedDiamondLeggings("sentry",
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createTrimmedDiamondBoots("sentry",
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.FEATHER_FALLING, 4,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createDiamondSword("Elite Blade",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3,
            Enchantment.LOOTING, 3
        ));
        kit.addItem(1, createDiamondAxe("Elite Axe",
            Enchantment.SHARPNESS, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createBow("Elite Bow",
            Enchantment.POWER, 5,
            Enchantment.PUNCH, 2,
            Enchantment.FLAME, 1
        ));
        kit.addItem(3, createShield());
        kit.addItem(4, createElytra());
        kit.addItem(5, new ItemStack(Material.TNT, 32));
        kit.addItem(6, new ItemStack(Material.COOKED_BEEF, 64));
        kit.addItem(7, new ItemStack(Material.GOLDEN_APPLE, 8));

        kits.put("mafiadiamond", kit);
    }

    private void createMafiaGoldKit() {
        Kit kit = new Kit("mafiagold", "§6Mafia Gold Soldier");
        kit.setDescription("Mafia gold rank - Eye trim chestplate");
        kit.setCooldownSeconds(5400);
        kit.setCategory("Mafia");
        kit.setIcon(createItem(Material.GOLDEN_HELMET, "Mafia Gold Helmet"));

        // Gold armor with sentry/eye trim
        kit.setHelmet(createTrimmedGoldHelmet("sentry",
            Enchantment.PROTECTION, 4,
            Enchantment.RESPIRATION, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createTrimmedGoldChestplate("eye",
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3,
            Enchantment.THORNS, 2
        ));
        kit.setLeggings(createTrimmedGoldLeggings("sentry",
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createTrimmedGoldBoots("sentry",
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createGoldSword("Gold Blade",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(1, createGoldAxe("Gold Axe",
            Enchantment.SHARPNESS, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createShield());
        kit.addItem(3, createElytra());
        kit.addItem(4, new ItemStack(Material.GOLD_INGOT, 64));
        kit.addItem(5, new ItemStack(Material.COOKED_BEEF, 64));
        kit.addItem(6, new ItemStack(Material.GOLDEN_APPLE, 8));

        kits.put("mafiagold", kit);
    }

    // =============================================
    // MIST CIVILIZATION (Bolt/Dune Trim)
    // =============================================

    private void createMistCivilizationKit() {
        Kit kit = new Kit("mistcivilization", "§3Mist Civilization Soldier");
        kit.setDescription("Mist Civ - Bolt (dune) trim, mist warriors");
        kit.setCooldownSeconds(5400);
        kit.setCategory("Mist");
        kit.setIcon(createItem(Material.NETHERITE_HELMET, "Mist Soldier Helmet"));

        // Mist bolt trim pattern (bolt -> dune fallback)
        kit.setHelmet(createTrimmedNetheriteHelmet("bolt",
            Enchantment.PROTECTION, 4,
            Enchantment.RESPIRATION, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createTrimmedNetheriteChestplate("bolt",
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setLeggings(createTrimmedNetheriteLeggings("bolt",
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createTrimmedNetheriteBoots("bolt",
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.SOUL_SPEED, 3,
            Enchantment.FEATHER_FALLING, 4,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createNetheriteSword("Mist Blade",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3,
            Enchantment.LOOTING, 3
        ));
        kit.addItem(1, createNetheriteAxe("Mist Axe",
            Enchantment.SHARPNESS, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createBow("Mist Bow",
            Enchantment.POWER, 5,
            Enchantment.PUNCH, 2
        ));
        kit.addItem(3, createShield());
        kit.addItem(4, createElytra());
        kit.addItem(5, new ItemStack(Material.SLIME_BALL, 32));
        kit.addItem(6, new ItemStack(Material.COOKED_BEEF, 64));
        kit.addItem(7, new ItemStack(Material.GOLDEN_APPLE, 8));

        kits.put("mistcivilization", kit);
    }

    // =============================================
    // KINGDOM OF THE CAVES (Arachnid - Silence Trim)
    // =============================================

    private void createKingdomOfTheCavesKit() {
        Kit kit = new Kit("kingdomcaves", "§2Kingdom of the Caves Soldier");
        kit.setDescription("Arachn1d's Kingdom - Silence/Host trim, underground warriors");
        kit.setCooldownSeconds(5400);
        kit.setCategory("Kingdom");
        kit.setIcon(createItem(Material.NETHERITE_HELMET, "Kingdom Helmet"));

        kit.setHelmet(createTrimmedNetheriteHelmet("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.RESPIRATION, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createTrimmedNetheriteChestplate("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3,
            Enchantment.THORNS, 3
        ));
        kit.setLeggings(createTrimmedNetheriteLeggings("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createTrimmedNetheriteBoots("host",
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.SOUL_SPEED, 3,
            Enchantment.FEATHER_FALLING, 4,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createNetheriteSword("Cave Blade",
            Enchantment.SHARPNESS, 5,
            Enchantment.UNBREAKING, 3,
            Enchantment.LOOTING, 3
        ));
        kit.addItem(1, createNetheriteAxe("Spider Axe",
            Enchantment.SHARPNESS, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createBow("Web Bow",
            Enchantment.POWER, 5,
            Enchantment.PUNCH, 2
        ));
        kit.addItem(3, createShield());
        kit.addItem(4, createElytra());

        // Cave supplies
        kit.addItem(5, new ItemStack(Material.COBWEB, 64));
        kit.addItem(6, new ItemStack(Material.TNT, 32));
        kit.addItem(7, new ItemStack(Material.COBBLESTONE, 128));
        kit.addItem(8, new ItemStack(Material.COOKED_BEEF, 64));

        kit.addItem(27, new ItemStack(Material.GOLDEN_APPLE, 8));
        kit.addItem(28, new ItemStack(Material.GLOWSTONE, 64));

        kits.put("kingdomcaves", kit);
    }

    // =============================================
    // MORE CHARACTER KITS
    // =============================================

    private void createMinuteTechKit() {
        Kit kit = new Kit("minutetech", "§bMinuteTech's Loadout");
        kit.setDescription("The Technician - Silence trim, complex builds and redstone");
        kit.setCooldownSeconds(3600);
        kit.setCategory("Support");
        kit.setIcon(createItem(Material.NETHERITE_PICKAXE, "MinuteTech's Pickaxe"));

        // Silence trim - legend status
        kit.setHelmet(createTrimmedNetheriteHelmet("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createTrimmedNetheriteChestplate("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setLeggings(createTrimmedNetheriteLeggings("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createTrimmedNetheriteBoots("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createNetheritePickaxe("Tech Pick",
            Enchantment.EFFICIENCY, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(1, createNetheriteAxe("Tech Axe",
            Enchantment.EFFICIENCY, 5,
            Enchantment.SILK_TOUCH, 1,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createNetheriteSword("Sword",
            Enchantment.SHARPNESS, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(3, createShield());
        kit.addItem(4, createElytra());

        // Redstone supplies
        kit.addItem(5, new ItemStack(Material.REDSTONE_BLOCK, 128));
        kit.addItem(6, new ItemStack(Material.REDSTONE, 256));
        kit.addItem(7, new ItemStack(Material.REPEATER, 64));
        kit.addItem(8, new ItemStack(Material.COMPARATOR, 64));

        kit.addItem(27, new ItemStack(Material.PISTON, 64));
        kit.addItem(28, new ItemStack(Material.STICKY_PISTON, 64));
        kit.addItem(29, new ItemStack(Material.OBSERVER, 64));
        kit.addItem(30, new ItemStack(Material.HOPPER, 64));
        kit.addItem(31, new ItemStack(Material.DROPPER, 32));
        kit.addItem(32, new ItemStack(Material.DISPENSER, 32));
        kit.addItem(33, new ItemStack(Material.RAIL, 128));
        kit.addItem(34, new ItemStack(Material.POWERED_RAIL, 64));
        kit.addItem(35, new ItemStack(Material.COOKED_BEEF, 64));

        kits.put("minutetech", kit);
    }

    private void createManePearKit() {
        Kit kit = new Kit("manepear", "§2ManePear's Loadout");
        kit.setDescription("The Builder - Flow trim, Destruction Duo partner of Wemmbu");
        kit.setCooldownSeconds(5400);
        kit.setCategory("Support");
        kit.setIcon(createItem(Material.NETHERITE_PICKAXE, "ManePear's Pickaxe"));

        kit.setHelmet(createTrimmedNetheriteHelmet("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.RESPIRATION, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createTrimmedNetheriteChestplate("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setLeggings(createTrimmedNetheriteLeggings("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createTrimmedNetheriteBoots("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.FEATHER_FALLING, 4,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createNetheritePickaxe("Builder Pick",
            Enchantment.EFFICIENCY, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(1, createNetheriteAxe("Builder Axe",
            Enchantment.EFFICIENCY, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createNetheriteSword("Sword",
            Enchantment.SHARPNESS, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(3, createShield());
        kit.addItem(4, createElytra());

        kit.addItem(5, new ItemStack(Material.OAK_PLANKS, 256));
        kit.addItem(6, new ItemStack(Material.COBBLESTONE, 256));
        kit.addItem(7, new ItemStack(Material.TNT, 64));
        kit.addItem(8, new ItemStack(Material.COOKED_BEEF, 64));

        kits.put("manepear", kit);
    }

    private void createPurpledKit() {
        Kit kit = new Kit("purpled", "§5Purpled's Loadout");
        kit.setDescription("Neutral participant");
        kit.setCooldownSeconds(5400);
        kit.setCategory("Neutral");
        kit.setIcon(createItem(Material.NETHERITE_HELMET, "Purpled's Helmet"));

        kit.setHelmet(createNetheriteHelmet(
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createNetheriteChestplate(
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setLeggings(createNetheriteLeggings(
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createNetheriteBoots(
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.FEATHER_FALLING, 4,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createNetheriteSword("Sword",
            Enchantment.SHARPNESS, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(1, createNetheriteAxe("Axe",
            Enchantment.SHARPNESS, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createBow("Bow",
            Enchantment.POWER, 5,
            Enchantment.PUNCH, 2
        ));
        kit.addItem(3, createShield());
        kit.addItem(4, createElytra());
        kit.addItem(5, new ItemStack(Material.TNT, 32));
        kit.addItem(6, new ItemStack(Material.COOKED_BEEF, 64));
        kit.addItem(7, new ItemStack(Material.GOLDEN_APPLE, 8));

        kits.put("purpled", kit);
    }

    private void createShoebillyKit() {
        Kit kit = new Kit("shoebilly", "§6ShoeBilly's Loadout");
        kit.setDescription("Cindercrest warrior - Led attacks, ally of Saparata");
        kit.setCooldownSeconds(5400);
        kit.setCategory("Cindercrest");
        kit.setIcon(createItem(Material.NETHERITE_HELMET, "ShoeBilly's Helmet"));

        kit.setHelmet(createTrimmedNetheriteHelmet("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.RESPIRATION, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createTrimmedNetheriteChestplate("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setLeggings(createTrimmedNetheriteLeggings("silence",
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createTrimmedNetheriteBoots("host",
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.FEATHER_FALLING, 4,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createNetheriteSword("Billy Blade",
            Enchantment.SHARPNESS, 5,
            Enchantment.FIRE_ASPECT, 2,
            Enchantment.UNBREAKING, 3,
            Enchantment.LOOTING, 3
        ));
        kit.addItem(1, createNetheriteAxe("Shoe Axe",
            Enchantment.SHARPNESS, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createBow("Bow",
            Enchantment.POWER, 5,
            Enchantment.PUNCH, 2
        ));
        kit.addItem(3, createShield());
        kit.addItem(4, createElytra());
        kit.addItem(5, new ItemStack(Material.TNT, 32));
        kit.addItem(6, new ItemStack(Material.COOKED_BEEF, 64));
        kit.addItem(7, new ItemStack(Material.GOLDEN_APPLE, 8));

        kits.put("shoebilly", kit);
    }

    private void createLettuceKKit() {
        Kit kit = new Kit("lettucek", "§aLettuceK's Loadout");
        kit.setDescription("Former king - Injustice Duo with Parrot, hosted The Purge");
        kit.setCooldownSeconds(5400);
        kit.setCategory("Neutral");
        kit.setIcon(createItem(Material.NETHERITE_HELMET, "LettuceK's Helmet"));

        kit.setHelmet(createTrimmedNetheriteHelmet("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.RESPIRATION, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setChestplate(createTrimmedNetheriteChestplate("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.UNBREAKING, 3
        ));
        kit.setLeggings(createTrimmedNetheriteLeggings("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.SWIFT_SNEAK, 3,
            Enchantment.UNBREAKING, 3
        ));
        kit.setBoots(createTrimmedNetheriteBoots("flow",
            Enchantment.PROTECTION, 4,
            Enchantment.DEPTH_STRIDER, 3,
            Enchantment.FEATHER_FALLING, 4,
            Enchantment.UNBREAKING, 3
        ));

        kit.addItem(0, createNetheriteSword("Lettuce Blade",
            Enchantment.SHARPNESS, 5,
            Enchantment.UNBREAKING, 3,
            Enchantment.LOOTING, 3
        ));
        kit.addItem(1, createNetheriteAxe("King Axe",
            Enchantment.SHARPNESS, 5,
            Enchantment.UNBREAKING, 3
        ));
        kit.addItem(2, createBow("Royal Bow",
            Enchantment.POWER, 5,
            Enchantment.PUNCH, 2
        ));
        kit.addItem(3, createShield());
        kit.addItem(4, createElytra());
        kit.addItem(5, new ItemStack(Material.TNT, 32));
        kit.addItem(6, new ItemStack(Material.COOKED_BEEF, 64));
        kit.addItem(7, new ItemStack(Material.GOLDEN_APPLE, 8));

        kits.put("lettucek", kit);
    }

    private void createStandardKits() {
        // Starter Kit
        Kit starter = new Kit("starter", "§7Starter Kit");
        starter.setDescription("Basic kit for new players");
        starter.setCooldownSeconds(3600);
        starter.setCategory("Standard");
        starter.setIcon(new ItemStack(Material.IRON_SWORD));

        starter.addItem(0, new ItemStack(Material.DIAMOND_SWORD));
        starter.addItem(1, new ItemStack(Material.DIAMOND_PICKAXE));
        starter.addItem(2, new ItemStack(Material.DIAMOND_AXE));
        starter.addItem(3, new ItemStack(Material.COOKED_BEEF, 64));
        starter.addItem(4, new ItemStack(Material.OAK_PLANKS, 64));
        starter.addItem(5, new ItemStack(Material.TORCH, 32));

        starter.setHelmet(new ItemStack(Material.IRON_HELMET));
        starter.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        starter.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        starter.setBoots(new ItemStack(Material.IRON_BOOTS));

        kits.put("starter", starter);

        // PvP Kit
        Kit pvp = new Kit("pvp", "§cPvP Kit");
        pvp.setDescription("Optimized for player combat");
        pvp.setCooldownSeconds(1800);
        pvp.setCategory("Standard");
        pvp.setIcon(new ItemStack(Material.DIAMOND_SWORD));

        pvp.addItem(0, new ItemStack(Material.DIAMOND_SWORD));
        pvp.addItem(1, new ItemStack(Material.ENDER_PEARL, 16));
        pvp.addItem(2, new ItemStack(Material.GOLDEN_APPLE, 16));
        pvp.addItem(3, new ItemStack(Material.COBBLESTONE, 64));
        pvp.addItem(4, new ItemStack(Material.COOKED_BEEF, 64));
        pvp.addItem(5, new ItemStack(Material.BOW));
        pvp.addItem(6, new ItemStack(Material.ARROW, 64));
        pvp.addItem(7, new ItemStack(Material.TOTEM_OF_UNDYING));

        pvp.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
        pvp.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
        pvp.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
        pvp.setBoots(new ItemStack(Material.DIAMOND_BOOTS));

        kits.put("pvp", pvp);
    }

    // =============================================
    // ITEM CREATION HELPERS WITH TRIMS
    // =============================================

    private ItemStack createNetheriteHelmet(Object... enchantments) {
        ItemStack item = new ItemStack(Material.NETHERITE_HELMET);
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createNetheriteChestplate(Object... enchantments) {
        ItemStack item = new ItemStack(Material.NETHERITE_CHESTPLATE);
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createNetheriteLeggings(Object... enchantments) {
        ItemStack item = new ItemStack(Material.NETHERITE_LEGGINGS);
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createNetheriteBoots(Object... enchantments) {
        ItemStack item = new ItemStack(Material.NETHERITE_BOOTS);
        applyEnchantments(item, enchantments);
        return item;
    }

    // Trimmed Netherite Armor
    private ItemStack createTrimmedNetheriteHelmet(String pattern, Object... enchantments) {
        ItemStack item = new ItemStack(Material.NETHERITE_HELMET);
        applyTrim(item, pattern);
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createTrimmedNetheriteHelmet(String pattern, TrimMaterial material, Object... enchantments) {
        ItemStack item = new ItemStack(Material.NETHERITE_HELMET);
        applyTrim(item, pattern, material);
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createTrimmedNetheriteChestplate(String pattern, Object... enchantments) {
        ItemStack item = new ItemStack(Material.NETHERITE_CHESTPLATE);
        applyTrim(item, pattern);
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createTrimmedNetheriteChestplate(String pattern, TrimMaterial material, Object... enchantments) {
        ItemStack item = new ItemStack(Material.NETHERITE_CHESTPLATE);
        applyTrim(item, pattern, material);
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createTrimmedNetheriteLeggings(String pattern, Object... enchantments) {
        ItemStack item = new ItemStack(Material.NETHERITE_LEGGINGS);
        applyTrim(item, pattern);
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createTrimmedNetheriteLeggings(String pattern, TrimMaterial material, Object... enchantments) {
        ItemStack item = new ItemStack(Material.NETHERITE_LEGGINGS);
        applyTrim(item, pattern, material);
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createTrimmedNetheriteBoots(String pattern, Object... enchantments) {
        ItemStack item = new ItemStack(Material.NETHERITE_BOOTS);
        applyTrim(item, pattern);
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createTrimmedNetheriteBoots(String pattern, TrimMaterial material, Object... enchantments) {
        ItemStack item = new ItemStack(Material.NETHERITE_BOOTS);
        applyTrim(item, pattern, material);
        applyEnchantments(item, enchantments);
        return item;
    }

    // Trimmed Diamond Armor
    private ItemStack createTrimmedDiamondHelmet(String pattern, Object... enchantments) {
        ItemStack item = new ItemStack(Material.DIAMOND_HELMET);
        applyTrim(item, pattern);
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createTrimmedDiamondChestplate(String pattern, Object... enchantments) {
        ItemStack item = new ItemStack(Material.DIAMOND_CHESTPLATE);
        applyTrim(item, pattern);
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createTrimmedDiamondLeggings(String pattern, Object... enchantments) {
        ItemStack item = new ItemStack(Material.DIAMOND_LEGGINGS);
        applyTrim(item, pattern);
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createTrimmedDiamondBoots(String pattern, Object... enchantments) {
        ItemStack item = new ItemStack(Material.DIAMOND_BOOTS);
        applyTrim(item, pattern);
        applyEnchantments(item, enchantments);
        return item;
    }

    // Trimmed Gold Armor
    private ItemStack createTrimmedGoldHelmet(String pattern, Object... enchantments) {
        ItemStack item = new ItemStack(Material.GOLDEN_HELMET);
        applyTrim(item, pattern);
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createTrimmedGoldChestplate(String pattern, Object... enchantments) {
        ItemStack item = new ItemStack(Material.GOLDEN_CHESTPLATE);
        applyTrim(item, pattern);
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createTrimmedGoldLeggings(String pattern, Object... enchantments) {
        ItemStack item = new ItemStack(Material.GOLDEN_LEGGINGS);
        applyTrim(item, pattern);
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createTrimmedGoldBoots(String pattern, Object... enchantments) {
        ItemStack item = new ItemStack(Material.GOLDEN_BOOTS);
        applyTrim(item, pattern);
        applyEnchantments(item, enchantments);
        return item;
    }

    /**
     * Resolves a trim pattern name to a real registry pattern.
     */
    private TrimPattern resolveTrimPattern(String patternName) {
        Registry<TrimPattern> registry = Bukkit.getRegistry(TrimPattern.class);
        TrimPattern pattern = registry.get(NamespacedKey.minecraft(patternName.toLowerCase(Locale.ROOT)));
        if (pattern == null) {
            plugin.getLogger().warning("Trim pattern '" + patternName + "' was not found in the registry, skipping trim");
        }
        return pattern;
    }

    // Apply armor trim (material auto-selected per faction lore)
    private void applyTrim(ItemStack item, String patternName) {
        applyTrim(item, patternName, TRIM_MATERIALS.getOrDefault(patternName.toLowerCase(Locale.ROOT), TrimMaterial.AMETHYST));
    }

    // Apply armor trim with explicit material
    private void applyTrim(ItemStack item, String patternName, TrimMaterial material) {
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof ArmorMeta armorMeta)) {
            return;
        }
        TrimPattern pattern = resolveTrimPattern(patternName);
        if (pattern == null) {
            return;
        }
        armorMeta.setTrim(new ArmorTrim(material, pattern));
        item.setItemMeta(meta);
    }

    // Weapon creation methods
    private ItemStack createNetheriteSword(String name, Object... enchantments) {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r§f" + name);
            item.setItemMeta(meta);
        }
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createDiamondSword(String name, Object... enchantments) {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r§f" + name);
            item.setItemMeta(meta);
        }
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createGoldSword(String name, Object... enchantments) {
        ItemStack item = new ItemStack(Material.GOLDEN_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r§6" + name);
            item.setItemMeta(meta);
        }
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createNetheriteAxe(String name, Object... enchantments) {
        ItemStack item = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r§f" + name);
            item.setItemMeta(meta);
        }
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createDiamondAxe(String name, Object... enchantments) {
        ItemStack item = new ItemStack(Material.DIAMOND_AXE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r§f" + name);
            item.setItemMeta(meta);
        }
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createGoldAxe(String name, Object... enchantments) {
        ItemStack item = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r§6" + name);
            item.setItemMeta(meta);
        }
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createNetheritePickaxe(String name, Object... enchantments) {
        ItemStack item = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r§f" + name);
            item.setItemMeta(meta);
        }
        applyEnchantments(item, enchantments);
        return item;
    }

    /**
     * Maces only exist from 1.21.2 onwards. On 1.21.1 this falls back to a
     * netherite sword so the kits still load and look right.
     */
    private Material maceMaterial() {
        Material mace = Material.matchMaterial("MACE");
        return mace != null ? mace : Material.NETHERITE_SWORD;
    }

    private ItemStack createMace(String name, Object... enchantments) {
        ItemStack item = new ItemStack(maceMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r§f" + name);
            item.setItemMeta(meta);
        }
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createSpear(String name, Object... enchantments) {
        ItemStack item = new ItemStack(Material.TRIDENT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r§f" + name);
            item.setItemMeta(meta);
        }
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createTrident(String name, Object... enchantments) {
        ItemStack item = new ItemStack(Material.TRIDENT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r§f" + name);
            item.setItemMeta(meta);
        }
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createBow(String name, Object... enchantments) {
        ItemStack item = new ItemStack(Material.BOW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r§f" + name);
            item.setItemMeta(meta);
        }
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createCrossbow(String name, Object... enchantments) {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r§f" + name);
            item.setItemMeta(meta);
        }
        applyEnchantments(item, enchantments);
        return item;
    }

    private ItemStack createFishingRod(String name) {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r§f" + name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createShield() {
        ItemStack item = new ItemStack(Material.SHIELD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r§fShield");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createElytra() {
        ItemStack item = new ItemStack(Material.ELYTRA);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r§fElytra");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§r" + name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void applyEnchantments(ItemStack item, Object... entries) {
        if (entries == null) return;
        for (int i = 0; i + 1 < entries.length; i += 2) {
            if (entries[i] instanceof Enchantment ench && entries[i + 1] instanceof Integer level) {
                item.addUnsafeEnchantment(ench, level);
            }
        }
    }

    private ItemStack loadItemStack(String data) {
        if (data == null || data.isEmpty()) return null;
        try {
            String[] parts = data.split(":");
            Material mat = Material.getMaterial(parts[0].toUpperCase());
            if (mat != null) {
                ItemStack item = new ItemStack(mat);
                if (parts.length > 1) {
                    item.setAmount(Integer.parseInt(parts[1]));
                }
                return item;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public void saveAll() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(kitsFile);
        config.set("kits", null);

        for (Kit kit : kits.values()) {
            String path = "kits." + kit.getId();
            config.set(path + ".name", kit.getName());
            config.set(path + ".description", kit.getDescription());
            config.set(path + ".cooldown", kit.getCooldownSeconds());
            config.set(path + ".category", kit.getCategory());
            config.set(path + ".enabled", kit.isEnabled());
            config.set(path + ".permissions", kit.getAllowedPermissions());

            ItemStack icon = kit.getIcon();
            if (icon != null && icon.getType() != Material.AIR) {
                config.set(path + ".icon", icon.getType().name() + ":" + icon.getAmount());
            }

            saveKitInventory(kit);
        }

        try {
            config.save(kitsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save kits.yml: " + e.getMessage());
        }
    }

    public Kit getKit(String id) {
        return kits.get(id.toLowerCase());
    }

    public Map<String, Kit> getKits() {
        return new LinkedHashMap<>(kits);
    }

    public List<Kit> getKitsByCategory(String category) {
        return kits.values().stream()
            .filter(k -> k.getCategory().equalsIgnoreCase(category))
            .toList();
    }

    public List<String> getCategories() {
        return kits.values().stream()
            .map(Kit::getCategory)
            .distinct()
            .sorted()
            .toList();
    }

    public Kit createKit(String id, String name) {
        Kit kit = new Kit(id, name);
        kits.put(id.toLowerCase(), kit);
        saveAll();
        return kit;
    }

    public boolean deleteKit(String id) {
        Kit kit = kits.remove(id.toLowerCase());
        if (kit != null) {
            File invFile = new File(kitsDataFolder, id + ".dat");
            if (invFile.exists()) {
                invFile.delete();
            }
            saveAll();
            return true;
        }
        return false;
    }

    public void updateKit(Kit kit) {
        kits.put(kit.getId().toLowerCase(), kit);
        saveAll();
    }
}
