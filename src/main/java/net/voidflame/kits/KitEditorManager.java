package net.voidflame.kits;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public final class KitEditorManager implements Listener {
    private static final int SIZE = 54;
    private static final int EDITABLE_SLOTS = 41;
    private static final String TITLE_PREFIX = "&5&lVOIDFLAME &8• &dKit Editor &8• &f";

    private final VoidFlameKitsPlugin plugin;
    private final Map<UUID, KitEditorSession> sessions = new ConcurrentHashMap<>();
    private final Set<UUID> saved = ConcurrentHashMap.newKeySet();

    public KitEditorManager(VoidFlameKitsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String kitId, String layoutName) {
        openInternal(player, kitId, layoutName, false);
    }

    public void openAdmin(Player player, String kitId) {
        if (!player.hasPermission("voidflame.kits.manage")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to manage kits.");
            return;
        }
        openInternal(player, kitId, "server", true);
    }

    private void openInternal(Player player, String kitId, String layoutName, boolean admin) {
        if (!plugin.getConfig().getBoolean("settings.editor-enabled", true)) {
            player.sendMessage(ChatColor.RED + "The kit editor is disabled.");
            return;
        }
        if (plugin.catalog().get(kitId) == null) {
            player.sendMessage(ChatColor.RED + "Unknown kit.");
            return;
        }

        String normalized = layoutName == null || layoutName.isBlank()
                ? "default"
                : layoutName.toLowerCase(java.util.Locale.ROOT);

        Inventory inv = Bukkit.createInventory(
                new Holder(),
                SIZE,
                color(TITLE_PREFIX + pretty(kitId) + " &8/ &f" + normalized)
        );

        load(player.getUniqueId(), kitId, normalized, admin).thenAccept(encoded ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;

                    if (encoded != null) deserializeInto(encoded, inv);
                    else copyPlayerInventory(player, inv);

                    decorate(inv, kitId, normalized);
                    sessions.put(player.getUniqueId(),
                            new KitEditorSession(player.getUniqueId(), kitId, normalized, inv, admin));
                    saved.remove(player.getUniqueId());
                    player.openInventory(inv);
                }));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) return;
        if (event.getClickedInventory() == null) return;

        int raw = event.getRawSlot();

        if (raw < 0 || raw >= SIZE) return;

        // The 41 editable slots behave like a real inventory. This fixes the old
        // editor where clicks/dragging were cancelled and items could not actually
        // be rearranged. Only the control row is protected.
        if (raw < EDITABLE_SLOTS) return;

        event.setCancelled(true);

        if (raw == 45) {
            saveSession(player);
        } else if (raw == 49) {
            resetSession(player);
        } else if (raw == 53) {
            saved.remove(player.getUniqueId());
            player.closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) return;
        // Allow normal dragging across editable slots; cancel only if the drag
        // touches the protected control row.
        for (int raw : event.getRawSlots()) {
            if (raw >= EDITABLE_SLOTS) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof Holder)) return;

        KitEditorSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;

        if (saved.remove(player.getUniqueId())) {
            return;
        }

        // Closing without pressing Save cancels the edit. This prevents the command,
        // GUI and editor from silently producing different states.
        player.sendMessage(color("&7Kit editor closed &8• &cChanges were not saved."));
    }

    private void saveSession(Player player) {
        KitEditorSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        try {
            String encoded = serialize(session.inventory());
            save(session, encoded);
            saved.add(player.getUniqueId());
            player.sendMessage(color("&a&lSAVED &8» &f" + pretty(session.kitId())
                    + " &7(" + session.layoutName() + ")"));
            player.closeInventory();
        } catch (IOException ex) {
            player.sendMessage(color("&cCould not save this kit layout."));
            plugin.getLogger().warning("Could not serialize kit layout for "
                    + player.getName() + ": " + ex.getMessage());
        }
    }

    private void resetSession(Player player) {
        KitEditorSession session = sessions.get(player.getUniqueId());
        if (session == null) return;

        load(session.playerId(), session.kitId(), session.layoutName(), session.admin()).thenAccept(encoded ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline() || sessions.get(player.getUniqueId()) != session) return;
                    if (encoded != null) {
                        deserializeInto(encoded, session.inventory());
                    } else {
                        copyPlayerInventory(player, session.inventory());
                    }
                    decorate(session.inventory(), session.kitId(), session.layoutName());
                    player.updateInventory();
                    player.sendMessage(color("&eKit editor reset to the last saved layout."));
                }));
    }

    private void decorate(Inventory inv, String kitId, String layout) {
        ItemStack border = button(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int slot = 41; slot <= 44; slot++) inv.setItem(slot, border.clone());

        inv.setItem(41, button(Material.NAME_TAG, "&d&l" + pretty(kitId),
                "&7Layout: &f" + layout,
                "&7Edit slots &f0-40",
                "&8Move items normally inside the editor."));

        inv.setItem(45, button(Material.LIME_DYE, "&a&lSAVE",
                "&7Save this layout and use it for your next match."));
        inv.setItem(49, button(Material.CLOCK, "&e&lRESET",
                "&7Restore the last saved layout."));
        inv.setItem(53, button(Material.RED_DYE, "&c&lCANCEL",
                "&7Close without saving changes."));
    }

    private ItemStack button(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(name));
            meta.setLore(java.util.Arrays.stream(lore).map(this::color).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private void copyPlayerInventory(Player player, Inventory target) {
        for (int i = 0; i < 36; i++) target.setItem(i, cloneOrNull(player.getInventory().getItem(i)));
        target.setItem(36, cloneOrNull(player.getInventory().getBoots()));
        target.setItem(37, cloneOrNull(player.getInventory().getLeggings()));
        target.setItem(38, cloneOrNull(player.getInventory().getChestplate()));
        target.setItem(39, cloneOrNull(player.getInventory().getHelmet()));
        target.setItem(40, cloneOrNull(player.getInventory().getItemInOffHand()));
    }

    private ItemStack cloneOrNull(ItemStack item) {
        return item == null ? null : item.clone();
    }

    private String serialize(Inventory inventory) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
            ItemStack[] items = new ItemStack[EDITABLE_SLOTS];
            for (int i = 0; i < items.length; i++) items[i] = cloneOrNull(inventory.getItem(i));
            out.writeObject(items);
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    private void deserializeInto(String encoded, Inventory inventory) {
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            try (BukkitObjectInputStream in =
                         new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                Object value = in.readObject();
                if (!(value instanceof ItemStack[] items) || items.length != EDITABLE_SLOTS) {
                    throw new IOException("Invalid layout payload");
                }
                for (int i = 0; i < items.length; i++) inventory.setItem(i, cloneOrNull(items[i]));
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Invalid saved kit layout; opening a fresh editor: "
                    + ex.getMessage());
        }
    }

    private java.util.concurrent.CompletableFuture<String> load(UUID uuid, String kit, String layout, boolean admin) {
        String storageKey = admin ? "admin-layouts." + kit : "layouts." + uuid + "." + kit;
        return plugin.get(storageKey).thenApply(value -> {
            if (value == null || value.isBlank()) return null;
            try {
                byte[] bytes = Base64.getDecoder().decode(value);
                try (BukkitObjectInputStream in =
                             new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                    Object object = in.readObject();
                    if (object instanceof Map<?, ?> map && map.get(layout) instanceof String savedLayout) {
                        return savedLayout;
                    }
                }
            } catch (Exception ex) {
                plugin.getLogger().warning("Invalid saved layout index: " + ex.getMessage());
            }
            return null;
        });
    }

    private void save(KitEditorSession session, String encoded) {
        UUID uuid = session.playerId();
        String kit = session.kitId();
        String layout = session.layoutName();
        String storageKey = session.admin() ? "admin-layouts." + kit : "layouts." + uuid + "." + kit;
        if (!plugin.getConfig().getBoolean("settings.saved-layouts-enabled", true)) return;

        plugin.get(storageKey).thenCompose(existing -> {
            java.util.LinkedHashMap<String, String> layouts = new java.util.LinkedHashMap<>();

            if (existing != null && !existing.isBlank()) {
                try {
                    byte[] bytes = Base64.getDecoder().decode(existing);
                    try (BukkitObjectInputStream in =
                                 new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
                        Object object = in.readObject();
                        if (object instanceof Map<?, ?> map) {
                            map.forEach((k, v) -> {
                                if (k instanceof String key && v instanceof String value) {
                                    layouts.put(key, value);
                                }
                            });
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            layouts.remove(layout);
            layouts.put(layout, encoded);

            int max = Math.max(1,
                    plugin.getConfig().getInt("settings.max-saved-layouts-per-player", 8));
            while (layouts.size() > max) {
                layouts.remove(layouts.keySet().iterator().next());
            }

            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
                    out.writeObject(layouts);
                }
                return plugin.put(storageKey,
                        Base64.getEncoder().encodeToString(bytes.toByteArray()));
            } catch (IOException ex) {
                return java.util.concurrent.CompletableFuture.<Void>failedFuture(ex);
            }
        }).exceptionally(ex -> {
            plugin.getLogger().warning("Failed to persist kit layout: " + ex.getMessage());
            return null;
        });
    }

    private String pretty(String id) {
        return id == null ? "Kit" : id.replace('_', ' ');
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    private static final class Holder implements InventoryHolder {
        @Override public Inventory getInventory() {
            return null;
        }
    }
}
