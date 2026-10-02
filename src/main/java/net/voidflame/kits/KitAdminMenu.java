package net.voidflame.kits;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class KitAdminMenu implements Listener {
    private static final String TITLE = "§8VoidFlame §7• §5Kit Administration";
    private final VoidFlameKitsPlugin plugin;
    private final Map<UUID, String> pendingCreate = new ConcurrentHashMap<>();

    public KitAdminMenu(VoidFlameKitsPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        if (!player.hasPermission("voidflame.kits.manage")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }

        Inventory inv = Bukkit.createInventory(new Holder(), 54, TITLE);
        ItemStack border = item(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < 54; slot++) {
            int row = slot / 9, col = slot % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) inv.setItem(slot, border.clone());
        }

        List<String> kits = plugin.catalog().kits();
        for (int i = 0; i < kits.size() && i < 28; i++) {
            int slot = 10 + (i / 7) * 9 + (i % 7);
            inv.setItem(slot, kitIcon(kits.get(i)));
        }

        inv.setItem(46, item(Material.EMERALD, "§a§lADD KIT",
                "§7Create a new kit from inside the server.",
                "§eClick §8» §fEnter its ID in chat."));
        inv.setItem(48, item(Material.COMPASS, "§b§lDEFAULT KIT",
                "§7Current: §f" + plugin.catalog().displayName(plugin.defaultKit()),
                "§eMiddle-click a kit §8» §fSet as default."));
        inv.setItem(49, item(Material.BARRIER, "§c§lCLOSE"));
        inv.setItem(52, item(Material.HOPPER, "§e§lCONTROLS",
                "§fLeft click §8» §7Edit layout",
                "§fRight click §8» §7Enable/Disable",
                "§fShift click §8» §7Delete custom kit",
                "§fMiddle click §8» §7Set default kit"));
        player.openInventory(inv);
    }

    private ItemStack kitIcon(String kit) {
        Material material = plugin.catalog().icon(kit);
        String state = plugin.catalog().enabled(kit) ? "§aENABLED" : "§cDISABLED";
        String def = plugin.defaultKit().equalsIgnoreCase(kit) ? " §6★ DEFAULT" : "";
        return item(material, "§d§l" + plugin.catalog().displayName(kit),
                "§7ID: §f" + kit,
                "§7Status: " + state + def,
                "§8Left §7edit §8• §7Right toggle §8• §7Shift delete",
                "§8Middle-click §7set default");
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot == 46) {
            player.closeInventory();
            pendingCreate.put(player.getUniqueId(), "create");
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Enter the new kit ID in chat. Use only letters, numbers and underscores.");
            return;
        }
        if (slot == 49) { player.closeInventory(); return; }
        if (slot == 48) { player.sendMessage(ChatColor.GRAY + "Middle-click a kit to make it the default."); return; }

        int index = gridIndex(slot);
        List<String> kits = plugin.catalog().kits();
        if (index < 0 || index >= kits.size()) return;
        String kit = kits.get(index);

        if (event.getClick() == ClickType.MIDDLE) {
            plugin.setDefaultKit(kit);
            player.sendMessage(ChatColor.GREEN + "Default kit set to " + plugin.catalog().displayName(kit) + ".");
            open(player);
            return;
        }
        if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
            if (!KitCatalog.KITS.contains(kit) && plugin.catalog().remove(kit)) {
                plugin.saveCatalog();
                player.sendMessage(ChatColor.GREEN + "Kit deleted: " + kit);
            } else {
                player.sendMessage(ChatColor.RED + "Built-in kits cannot be deleted.");
            }
            open(player);
            return;
        }
        if (event.getClick() == ClickType.RIGHT) {
            boolean enabled = !plugin.catalog().enabled(kit);
            plugin.catalog().setEnabled(kit, enabled);
            plugin.saveCatalog();
            player.sendMessage((enabled ? ChatColor.GREEN : ChatColor.RED) + "Kit " + kit + " is now " + (enabled ? "enabled." : "disabled."));
            open(player);
            return;
        }

        plugin.settingsMenu().open(player, kit);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) event.setCancelled(true);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!"create".equals(pendingCreate.remove(player.getUniqueId()))) return;
        event.setCancelled(true);
        String id = event.getMessage().trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.hasPermission("voidflame.kits.manage")) return;
            if (!plugin.catalog().add(id)) {
                player.sendMessage(ChatColor.RED + "Invalid or duplicate kit ID.");
                open(player);
                return;
            }
            plugin.catalog().setEnabled(id, false);
            plugin.saveCatalog();
            player.sendMessage(ChatColor.GREEN + "Created kit " + id + " as DISABLED. Configure it before enabling.");
            plugin.editor().openAdmin(player, id);
        });
    }

    private int gridIndex(int slot) {
        int row = slot / 9, col = slot % 9;
        if (row < 1 || row > 4 || col < 1 || col > 7) return -1;
        return (row - 1) * 7 + (col - 1);
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static final class Holder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
}
