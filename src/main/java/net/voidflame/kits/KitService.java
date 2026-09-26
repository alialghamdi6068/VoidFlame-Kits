package net.voidflame.kits;

import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.enchantments.Enchantment;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class KitService {
    private final VoidFlameKitsPlugin plugin;
    private final Map<UUID, String> selected = new HashMap<>();

    public KitService(VoidFlameKitsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean apply(Player player, String id) {
        if (player == null || id == null) return false;
        String kitId = id.toLowerCase(Locale.ROOT);
        if (plugin.catalog().get(kitId) == null) return false;

        ConfigurationSection root = plugin.getConfig().getConfigurationSection("kits." + kitId);
        if (root == null) {
            plugin.getLogger().warning("Missing loadout configuration for kit '" + kitId + "'.");
            return false;
        }

        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        player.setItemOnCursor(new ItemStack(Material.AIR));

        ConfigurationSection items = root.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                int slot;
                try {
                    slot = Integer.parseInt(key);
                } catch (NumberFormatException ignored) {
                    continue;
                }
                if (slot < 0 || slot > 40) continue;
                ItemStack item = readItem(items.getConfigurationSection(key));
                if (item == null) continue;
                setSlot(player, slot, item);
            }
        }

        ItemStack offhand = readItem(root.getConfigurationSection("offhand"));
        if (offhand != null) player.getInventory().setItemInOffHand(offhand);

        selected.put(player.getUniqueId(), kitId);
        return true;
    }

    private ItemStack readItem(ConfigurationSection section) {
        if (section == null) return null;
        String materialName = section.getString("material", "AIR");
        Material material = Material.matchMaterial(materialName);
        if (material == null || material == Material.AIR) return null;

        int amount = Math.max(1, section.getInt("amount", 1));
        ItemStack item = new ItemStack(material, Math.min(amount, material.getMaxStackSize()));

        applyPotion(item, section.getString("potion", null));
        applyEnchantments(item, section.getConfigurationSection("enchants"));
        applyShulkerContents(item, section.getConfigurationSection("contents"));
        return item;
    }

    private void applyPotion(ItemStack item, String potionName) {
        if (potionName == null || !(item.getItemMeta() instanceof PotionMeta meta)) return;
        try {
            PotionType type = PotionType.valueOf(potionName.toUpperCase(Locale.ROOT));
            meta.setBasePotionType(type);
            item.setItemMeta(meta);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown potion type '" + potionName + "' in kits config.");
        }
    }

    private void applyEnchantments(ItemStack item, ConfigurationSection enchants) {
        if (enchants == null) return;
        for (String enchantName : enchants.getKeys(false)) {
            Enchantment enchantment = Enchantment.getByName(enchantName.toUpperCase(Locale.ROOT));
            if (enchantment == null) {
                plugin.getLogger().warning("Unknown enchantment '" + enchantName + "' in kits config.");
                continue;
            }
            item.addUnsafeEnchantment(enchantment, Math.max(1, enchants.getInt(enchantName, 1)));
        }
    }

    private void applyShulkerContents(ItemStack item, ConfigurationSection contents) {
        if (contents == null || !(item.getItemMeta() instanceof BlockStateMeta meta)) return;
        if (!(meta.getBlockState() instanceof ShulkerBox shulker)) return;

        for (String key : contents.getKeys(false)) {
            int slot;
            try {
                slot = Integer.parseInt(key);
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (slot < 0 || slot >= shulker.getInventory().getSize()) continue;
            ItemStack nested = readItem(contents.getConfigurationSection(key));
            if (nested != null) shulker.getInventory().setItem(slot, nested);
        }

        meta.setBlockState(shulker);
        item.setItemMeta(meta);
    }

    private void setSlot(Player player, int slot, ItemStack item) {
        if (slot < 36) {
            player.getInventory().setItem(slot, item);
            return;
        }
        switch (slot) {
            case 36 -> player.getInventory().setBoots(item);
            case 37 -> player.getInventory().setLeggings(item);
            case 38 -> player.getInventory().setChestplate(item);
            case 39 -> player.getInventory().setHelmet(item);
            case 40 -> player.getInventory().setItemInOffHand(item);
            default -> { }
        }
    }

    public String selected(Player player) {
        return player == null ? null : selected.get(player.getUniqueId());
    }
}
