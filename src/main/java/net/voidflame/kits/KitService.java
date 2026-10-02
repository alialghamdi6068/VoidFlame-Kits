package net.voidflame.kits;


import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionType;
import org.bukkit.enchantments.Enchantment;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.util.io.BukkitObjectInputStream;

public final class KitService implements net.voidflame.core.api.KitService, Listener {
    private final VoidFlameKitsPlugin plugin;
    private final Map<UUID, String> selected = new HashMap<>();
    private final Map<String, ItemStack[]> serverLayouts = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, ItemStack[]>> personalLayouts = new ConcurrentHashMap<>();
    private final NamespacedKey goldenHeadKey;
    private final NamespacedKey kitItemKey;

    public KitService(VoidFlameKitsPlugin plugin) {
        this.plugin = plugin;
        this.goldenHeadKey = new NamespacedKey(plugin, "golden_head");
        this.kitItemKey = new NamespacedKey(plugin, "kit_item");
    }

    @Override
    public boolean apply(Player player, String id) {
        if (player == null || id == null) return false;
        String kitId = id.toLowerCase(Locale.ROOT);
        if (plugin.catalog().get(kitId) == null || !plugin.catalog().enabled(kitId)) return false;

        ItemStack[] custom = null;
        Map<String, ItemStack[]> personal = personalLayouts.get(player.getUniqueId());
        if (personal != null) custom = personal.get(kitId);
        if (custom == null) custom = serverLayouts.get(kitId);

        if (custom != null) {
            clearInventory(player);
            for (int slot = 0; slot < Math.min(41, custom.length); slot++) {
                ItemStack item = custom[slot];
                if (item != null && item.getType() != Material.AIR) setSlot(player, slot, item.clone());
            }
            selected.put(player.getUniqueId(), kitId);
            logApply(player, kitId);
            return true;
        }

        ConfigurationSection root = plugin.getConfig().getConfigurationSection("kits." + kitId);
        if (root == null) {
            plugin.getLogger().warning("Missing loadout configuration for kit '" + kitId + "'.");
            return false;
        }

        clearInventory(player);

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
        logApply(player, kitId);
        if (registration != null && registration.getProvider() != null) {
            registration.getProvider().log(player.getUniqueId().toString(), "KIT_APPLY", player.getName(), "kit=" + kitId);
        }
        return true;
    }

    private void clearInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        player.setItemOnCursor(new ItemStack(Material.AIR));
    }

    private void logApply(Player player, String kitId) {
        var registration = org.bukkit.Bukkit.getServicesManager().getRegistration(net.voidflame.core.api.AuditLogService.class);
        if (registration != null && registration.getProvider() != null) {
            registration.getProvider().log(player.getUniqueId().toString(), "KIT_APPLY", player.getName(), "kit=" + kitId);
        }
    }

    public void cacheServerLayout(String kitId, String encoded) {
        ItemStack[] decoded = decodeLayout(encoded);
        if (decoded != null) serverLayouts.put(kitId.toLowerCase(Locale.ROOT), decoded);
    }

    public void cachePersonalLayout(UUID playerId, String kitId, String encoded) {
        ItemStack[] decoded = decodeLayout(encoded);
        if (decoded != null) {
            personalLayouts.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                    .put(kitId.toLowerCase(Locale.ROOT), decoded);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String defaultKit = plugin.defaultKit();
        if (plugin.catalog().exists(defaultKit) && plugin.catalog().enabled(defaultKit)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> apply(event.getPlayer(), defaultKit), 1L);
        }
        UUID uuid = event.getPlayer().getUniqueId();
        for (String kit : plugin.catalog().kits()) {
            final String key = "layouts." + uuid + "." + kit;
            plugin.get(key).thenAccept(value -> {
                if (value == null || value.isBlank()) return;
                try {
                    byte[] bytes = Base64.getDecoder().decode(value);
                    try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                        Object object = in.readObject();
                        if (object instanceof Map<?, ?> map && map.get("default") instanceof String encoded) {
                            cachePersonalLayout(uuid, kit, encoded);
                        }
                    }
                } catch (Exception ignored) {
                }
            });
        }
    }

    public void loadServerLayouts() {
        for (String kit : plugin.catalog().kits()) {
            plugin.get("admin-layouts." + kit).thenAccept(value -> {
                if (value == null || value.isBlank()) return;
                try {
                    byte[] bytes = Base64.getDecoder().decode(value);
                    try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                        Object object = in.readObject();
                        if (object instanceof Map<?, ?> map && map.get("server") instanceof String encoded) {
                            cacheServerLayout(kit, encoded);
                        }
                    }
                } catch (Exception ignored) {
                }
            });
        }
    }

    private ItemStack[] decodeLayout(String encoded) {
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                Object value = in.readObject();
                if (!(value instanceof ItemStack[] items) || items.length != 41) return null;
                ItemStack[] copy = new ItemStack[41];
                for (int i = 0; i < 41; i++) copy[i] = items[i] == null ? null : items[i].clone();
                return copy;
            }
        } catch (IOException | ClassNotFoundException | IllegalArgumentException ex) {
            return null;
        }
    }

    public List<String> validateConfiguration() {
        List<String> errors = new ArrayList<>();
        for (String kitId : KitCatalog.KITS) {
            ConfigurationSection root = plugin.getConfig().getConfigurationSection("kits." + kitId);
            if (root == null) {
                errors.add("Missing kit configuration: " + kitId);
                continue;
            }
            ConfigurationSection items = root.getConfigurationSection("items");
            if (items == null) {
                errors.add("Missing items section: " + kitId);
                continue;
            }
            for (String key : items.getKeys(false)) {
                ConfigurationSection item = items.getConfigurationSection(key);
                if (item == null) continue;
                Material material = Material.matchMaterial(item.getString("material", "AIR"));
                if (material == null) {
                    plugin.getLogger().warning("[Config] " + kitId + " slot " + key
                            + ": unsupported material '" + item.getString("material", "AIR")
                            + "'; the item will be skipped instead of disabling VoidFlame-Kits.");
                    continue;
                }
                String potion = item.getString("potion");
                if (potion != null) {
                    try { PotionType.valueOf(potion.toUpperCase(Locale.ROOT)); }
                    catch (IllegalArgumentException ex) { errors.add(kitId + " slot " + key + ": unknown potion " + potion); }
                    if (plugin.getConfig().getBoolean("settings.require-splash-potions", true)
                            && material != Material.SPLASH_POTION) {
                        errors.add(kitId + " slot " + key + ": potion must use SPLASH_POTION");
                    }
                }
                ConfigurationSection enchants = item.getConfigurationSection("enchants");
                if (enchants != null) {
                    for (String enchant : enchants.getKeys(false)) {
                        if (Enchantment.getByName(enchant.toUpperCase(Locale.ROOT)) == null) {
                            plugin.getLogger().warning("[Config] " + kitId + " slot " + key
                                    + ": unknown enchantment " + enchant + "; it will be ignored.");
                        }
                    }
                }
            }
        }
        return List.copyOf(errors);
    }

    @Override
    public boolean exists(String id) {
        return id != null && plugin.catalog().get(id.toLowerCase(Locale.ROOT)) != null;
    }

    private ItemStack readItem(ConfigurationSection section) {
        if (section == null) return null;
        String materialName = section.getString("material", "AIR");
        boolean goldenHead = section.getBoolean("golden-head", false);
        Material material = goldenHead ? Material.PLAYER_HEAD : Material.matchMaterial(materialName);
        if (material == null || material == Material.AIR) {
            plugin.getLogger().warning("Skipping unsupported material '" + materialName + "'.");
            return null;
        }

        int amount = Math.max(1, section.getInt("amount", 1));
        ItemStack item = new ItemStack(material, Math.min(amount, material.getMaxStackSize()));

        markKitItem(item);
        if (goldenHead) markGoldenHead(item);
        applyPotion(item, section.getString("potion", null));
        applyCustomPotionEffect(item, section.getString("custom-potion-effect", null), section.getInt("custom-potion-duration-ticks", 0));
        applyChargedProjectile(item, section.getConfigurationSection("charged-projectile"));
        applyEnchantments(item, section.getConfigurationSection("enchants"));
        applyShulkerContents(item, section.getConfigurationSection("contents"));
        return item;
    }

    private void markKitItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(kitItemKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    private void markGoldenHead(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.setDisplayName("Golden Head");
        meta.getPersistentDataContainer().set(goldenHeadKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
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

    private void applyCustomPotionEffect(ItemStack item, String effectName, int durationTicks) {
        if (effectName == null || durationTicks <= 0 || !(item.getItemMeta() instanceof PotionMeta meta)) return;
        PotionEffectType type = PotionEffectType.getByName(effectName.toUpperCase(Locale.ROOT));
        if (type == null) {
            plugin.getLogger().warning("Unknown custom potion effect '" + effectName + "'.");
            return;
        }
        meta.addCustomEffect(new PotionEffect(type, durationTicks, 0, false, true, true), true);
        item.setItemMeta(meta);
    }

    private void applyChargedProjectile(ItemStack item, ConfigurationSection projectile) {
        if (projectile == null || !(item.getItemMeta() instanceof CrossbowMeta meta)) return;
        ItemStack charged = readItem(projectile);
        if (charged == null || charged.getType() == Material.AIR) return;
        meta.setChargedProjectiles(java.util.List.of(charged));
        item.setItemMeta(meta);
    }

    private void applyEnchantments(ItemStack item, ConfigurationSection enchants) {
        if (enchants == null) return;
        for (String enchantName : enchants.getKeys(false)) {
            Enchantment enchantment = Enchantment.getByName(enchantName.toUpperCase(Locale.ROOT));
            if (enchantment == null) {
                plugin.getLogger().warning("Unknown enchantment '" + enchantName + "' in kits config; skipping.");
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

    @Override
    public boolean openEditor(Player player, String kitId, String layoutName) {
        if (player == null || kitId == null) return false;
        if (!exists(kitId)) return false;
        plugin.editor().open(player, kitId, layoutName == null || layoutName.isBlank() ? "default" : layoutName);
        return true;
    }

    public void clearSelected() { selected.clear(); personalLayouts.clear(); serverLayouts.clear(); }

    public String selected(Player player) {
        return player == null ? null : selected.get(player.getUniqueId());
    }
}
