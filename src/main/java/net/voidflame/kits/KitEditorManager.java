package net.voidflame.kits;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public final class KitEditorManager implements Listener {
    private static final int SIZE = 54;
    private final VoidFlameKitsPlugin plugin;
    private final Map<UUID, KitEditorSession> sessions = new ConcurrentHashMap<>();

    public KitEditorManager(VoidFlameKitsPlugin plugin) { this.plugin = plugin; }

    public void open(Player player, String kitId, String layoutName) {
        if (!plugin.getConfig().getBoolean("settings.editor-enabled", true)) {
            player.sendMessage(ChatColor.RED + "The kit editor is disabled.");
            return;
        }
        if (plugin.catalog().get(kitId) == null) {
            player.sendMessage(ChatColor.RED + "Unknown kit.");
            return;
        }
        String normalized = layoutName == null || layoutName.isBlank() ? "default" : layoutName.toLowerCase(java.util.Locale.ROOT);
        Inventory inv = Bukkit.createInventory(new Holder(), SIZE, ChatColor.DARK_PURPLE + "Kit Editor: " + kitId + " / " + normalized);
        load(player.getUniqueId(), kitId, normalized).thenAccept(encoded -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;
            if (encoded != null) deserializeInto(encoded, inv);
            else copyPlayerInventory(player, inv);
            sessions.put(player.getUniqueId(), new KitEditorSession(player.getUniqueId(), kitId, normalized, inv));
            player.openInventory(inv);
        }));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) return;
        if (event.getClickedInventory() == event.getView().getBottomInventory()) return;
        if (event.getRawSlot() >= SIZE) return;
        if (event.getClick().isDoubleClick()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) return;
        if (event.getRawSlots().stream().anyMatch(slot -> slot < SIZE)) event.setCancelled(false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof Holder)) return;
        KitEditorSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        String encoded;
        try { encoded = serialize(session.inventory()); }
        catch (IOException ex) {
            player.sendMessage(ChatColor.RED + "Failed to save the kit layout.");
            plugin.getLogger().warning("Could not serialize kit layout for " + player.getName() + ": " + ex.getMessage());
            return;
        }
        save(player.getUniqueId(), session.kitId(), session.layoutName(), encoded);
        player.sendMessage(ChatColor.GREEN + "Kit layout saved: " + session.kitId() + "/" + session.layoutName());
    }

    private void copyPlayerInventory(Player player, Inventory target) {
        for (int i = 0; i < 36; i++) target.setItem(i, cloneOrNull(player.getInventory().getItem(i)));
        target.setItem(36, cloneOrNull(player.getInventory().getHelmet()));
        target.setItem(37, cloneOrNull(player.getInventory().getChestplate()));
        target.setItem(38, cloneOrNull(player.getInventory().getLeggings()));
        target.setItem(39, cloneOrNull(player.getInventory().getBoots()));
        target.setItem(40, cloneOrNull(player.getInventory().getItemInOffHand()));
    }

    private ItemStack cloneOrNull(ItemStack item) { return item == null ? null : item.clone(); }

    private String serialize(Inventory inventory) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
            ItemStack[] items = new ItemStack[41];
            for (int i = 0; i < items.length; i++) items[i] = cloneOrNull(inventory.getItem(i));
            out.writeObject(items);
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    private void deserializeInto(String encoded, Inventory inventory) {
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                Object value = in.readObject();
                if (!(value instanceof ItemStack[] items) || items.length != 41) throw new IOException("Invalid layout payload");
                for (int i = 0; i < items.length; i++) inventory.setItem(i, cloneOrNull(items[i]));
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Invalid saved kit layout; opening a fresh editor: " + ex.getMessage());
        }
    }

    private java.util.concurrent.CompletableFuture<String> load(UUID uuid, String kit, String layout) {
        return plugin.get("layouts." + uuid + "." + kit).thenApply(value -> {
            if (value == null || value.isBlank()) return null;
            try {
                byte[] bytes = Base64.getDecoder().decode(value);
                try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                    Object object = in.readObject();
                    if (object instanceof Map<?, ?> map && map.get(layout) instanceof String saved) return saved;
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Invalid saved layout index: " + ex.getMessage());
            }
            return null;
        });
    }

    private void save(UUID uuid, String kit, String layout, String encoded) {
        if (!plugin.getConfig().getBoolean("settings.saved-layouts-enabled", true)) return;
        plugin.get("layouts." + uuid + "." + kit).thenCompose(existing -> {
            java.util.LinkedHashMap<String, String> layouts = new java.util.LinkedHashMap<>();
            if (existing != null && !existing.isBlank()) {
                try {
                    byte[] bytes = Base64.getDecoder().decode(existing);
                    try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                        Object object = in.readObject();
                        if (object instanceof Map<?, ?> map) {
                            map.forEach((k, v) -> {
                                if (k instanceof String key && v instanceof String value) layouts.put(key, value);
                            });
                        }
                    }
                } catch (Exception ignored) {
                }
            }
            layouts.remove(layout);
            layouts.put(layout, encoded);
            int max = Math.max(1, plugin.getConfig().getInt("settings.max-saved-layouts-per-player", 8));
            while (layouts.size() > max) layouts.remove(layouts.keySet().iterator().next());
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
                    out.writeObject(layouts);
                }
                return plugin.put("layouts." + uuid + "." + kit, Base64.getEncoder().encodeToString(bytes.toByteArray()));
            } catch (IOException ex) {
                return java.util.concurrent.CompletableFuture.<Void>failedFuture(ex);
            }
        }).exceptionally(ex -> {
            plugin.getLogger().warning("Failed to persist kit layout: " + ex.getMessage());
            return null;
        });
    }

    private static final class Holder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
}
