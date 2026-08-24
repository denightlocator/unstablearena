package com.unstable.characters.models;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Kit {
    private String id;
    private String name;
    private String description;
    private long cooldownSeconds;
    private Map<Integer, ItemStack> inventory;
    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;
    private ItemStack icon;
    private List<String> allowedPermissions;
    private boolean enabled;
    private String category;

    public Kit(String id, String name) {
        this.id = id;
        this.name = name;
        this.description = "";
        this.cooldownSeconds = 0;
        this.inventory = new HashMap<>();
        this.helmet = null;
        this.chestplate = null;
        this.leggings = null;
        this.boots = null;
        this.icon = new ItemStack(Material.DIAMOND_SWORD);
        this.allowedPermissions = new ArrayList<>();
        this.enabled = true;
        this.category = "General";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(long cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }

    public Map<Integer, ItemStack> getInventory() { return inventory; }
    public void setInventory(Map<Integer, ItemStack> inventory) { this.inventory = inventory; }

    public ItemStack getHelmet() { return helmet; }
    public void setHelmet(ItemStack helmet) { this.helmet = helmet; }

    public ItemStack getChestplate() { return chestplate; }
    public void setChestplate(ItemStack chestplate) { this.chestplate = chestplate; }

    public ItemStack getLeggings() { return leggings; }
    public void setLeggings(ItemStack leggings) { this.leggings = leggings; }

    public ItemStack getBoots() { return boots; }
    public void setBoots(ItemStack boots) { this.boots = boots; }

    public ItemStack getIcon() { return icon; }
    public void setIcon(ItemStack icon) { this.icon = icon; }

    public List<String> getAllowedPermissions() { return allowedPermissions; }
    public void setAllowedPermissions(List<String> allowedPermissions) { this.allowedPermissions = allowedPermissions; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public void addItem(int slot, ItemStack item) {
        if (slot >= 0 && slot < 36) {
            inventory.put(slot, item.clone());
        }
    }

    public void removeItem(int slot) {
        inventory.remove(slot);
    }

    public void clearInventory() {
        inventory.clear();
    }
}
