package net.voidflame.kits;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KitGuiSettingsMenu implements Listener {
    private final VoidFlameKitsPlugin plugin;
    private final Map<UUID, String> pending = new ConcurrentHashMap<>();

    public KitGuiSettingsMenu(VoidFlameKitsPlugin plugin) { this.plugin = plugin; }

    public void open(Player p) {
        if (!p.hasPermission("voidflame.kits.manage")) return;
        int rows = Math.max(3, Math.min(6, plugin.getConfig().getInt("gui.rows", 6)));
        Inventory inv = Bukkit.createInventory(new Holder(), rows * 9, "§8VoidFlame §7• §5Kit GUI Settings");
        Material filler = material(plugin.getConfig().getString("gui.filler", "BLACK_STAINED_GLASS_PANE"), Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, item(filler, " "));
        inv.setItem(10, item(Material.NAME_TAG, "§d§lGUI TITLE", "§7Current: §f" + plugin.getConfig().getString("gui.title", "&8VoidFlame &7• &dKits"), "§eClick §8» §fEdit in chat"));
        inv.setItem(12, item(Material.ENDER_PEARL, "§d§lROWS", "§7Current: §f" + rows, "§eClick §8» §fCycle 3 → 4 → 5 → 6"));
        inv.setItem(14, item(filler, "§d§lFILLER", "§7Current: §f" + filler.name(), "§eClick §8» §fOpen material picker"));
        inv.setItem(16, item(Material.CLOCK, "§e§lRELOAD", "§7Reload GUI settings from config/storage.", "§eClick §8» §fReload"));
        inv.setItem(inv.getSize() - 2, item(Material.LIME_DYE, "§a§lSAVE", "§7Save all GUI settings."));
        inv.setItem(inv.getSize() - 1, item(Material.BARRIER, "§c§lCLOSE"));
        p.openInventory(inv);
    }

    @EventHandler
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof Holder)) return;
        if (e.getClickedInventory() != e.getView().getTopInventory()) { e.setCancelled(true); return; }
        e.setCancelled(true);
        int slot = e.getRawSlot();
        int last = e.getView().getTopInventory().getSize() - 1;
        if (slot == 10) {
            pending.put(p.getUniqueId(), "title");
            p.closeInventory();
            p.sendMessage("§5§lVOIDFLAME §8» §fType the GUI title in chat. Use & colors. Type §ccancel §7to stop.");
        } else if (slot == 12) {
            int rows = plugin.getConfig().getInt("gui.rows", 6);
            plugin.getConfig().set("gui.rows", rows >= 6 ? 3 : rows + 1);
            plugin.saveConfig();
            open(p);
        } else if (slot == 14) {
            picker(p);
        } else if (slot == 16) {
            plugin.reloadConfig();
            p.sendMessage("§aKit GUI settings reloaded.");
            open(p);
        } else if (slot == e.getView().getTopInventory().getSize() - 2) {
            plugin.saveConfig();
            p.sendMessage("§aKit GUI settings saved.");
            open(p);
        } else if (slot == last) {
            p.closeInventory();
        }
    }

    @EventHandler
    public void chat(AsyncPlayerChatEvent e) {
        String mode = pending.remove(e.getPlayer().getUniqueId());
        if (mode == null) return;
        e.setCancelled(true);
        String value = e.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!e.getPlayer().hasPermission("voidflame.kits.manage")) return;
            if (value.equalsIgnoreCase("cancel")) { open(e.getPlayer()); return; }
            if (value.isBlank()) { e.getPlayer().sendMessage("§cTitle cannot be empty."); open(e.getPlayer()); return; }
            plugin.getConfig().set("gui.title", value);
            plugin.saveConfig();
            e.getPlayer().sendMessage("§aKit GUI title updated.");
            open(e.getPlayer());
        });
    }

    private void picker(Player p) {
        Inventory inv = Bukkit.createInventory(new PickerHolder(), 54, "§8VoidFlame §7• §5GUI Filler");
        for (int i = 0; i < 54; i++) inv.setItem(i, item(Material.BLACK_STAINED_GLASS_PANE, " "));
        int slot = 10;
        for (Material m : Material.values()) {
            if (!m.isItem() || m == Material.AIR) continue;
            inv.setItem(slot++, new ItemStack(m));
            if (slot >= 44) break;
        }
        inv.setItem(49, item(Material.ARROW, "§7§lBACK"));
        p.openInventory(inv);
    }

    @EventHandler
    public void pickerClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof PickerHolder)) return;
        e.setCancelled(true);
        if (e.getRawSlot() == 49) { open(p); return; }
        ItemStack item = e.getCurrentItem();
        if (item == null || !item.getType().isItem()) return;
        plugin.getConfig().set("gui.filler", item.getType().name());
        plugin.saveConfig();
        open(p);
    }

    @EventHandler
    public void drag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Holder ||
                e.getView().getTopInventory().getHolder() instanceof PickerHolder) e.setCancelled(true);
    }

    private Material material(String value, Material fallback) {
        Material m = Material.matchMaterial(value == null ? "" : value);
        return m == null ? fallback : m;
    }

    private ItemStack item(Material m, String name, String... lore) {
        ItemStack x = new ItemStack(m);
        ItemMeta meta = x.getItemMeta();
        if (meta != null) { meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name)); meta.setLore(java.util.Arrays.stream(lore).map(s -> ChatColor.translateAlternateColorCodes('&', s)).toList()); x.setItemMeta(meta); }
        return x;
    }

    private record Holder() implements InventoryHolder { public Inventory getInventory() { return null; } }
    private record PickerHolder() implements InventoryHolder { public Inventory getInventory() { return null; } }
}
