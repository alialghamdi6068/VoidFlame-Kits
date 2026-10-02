package net.voidflame.kits;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class KitAdminMenu implements Listener {
    private static final String TITLE = "§8VoidFlame §7• §cKit Admin";
    private final VoidFlameKitsPlugin plugin;

    public KitAdminMenu(VoidFlameKitsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        if (!player.hasPermission("voidflame.kits.manage")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return;
        }

        Inventory inv = Bukkit.createInventory(new Holder(), 54, TITLE);
        ItemStack border = item(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < 54; slot++) {
            int row = slot / 9;
            int col = slot % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) inv.setItem(slot, border);
        }

        List<String> kits = plugin.catalog().kits();
        int slot = 10;
        for (String kit : kits) {
            inv.setItem(slot, kitIcon(kit));
            slot++;
            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;
        }

        inv.setItem(49, item(Material.BARRIER, "§c§lClose", "§7Close the admin editor."));
        player.openInventory(inv);
    }

    private ItemStack kitIcon(String kit) {
        Material material = Material.NAME_TAG;
        var section = plugin.getConfig().getConfigurationSection("kits." + kit + ".items");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Material candidate = Material.matchMaterial(section.getString(key + ".material", "NAME_TAG"));
                if (candidate != null && candidate != Material.AIR) {
                    material = candidate;
                    break;
                }
            }
        }
        return item(material, "§d§l" + pretty(kit),
                "§7Edit the server layout.",
                "§7Changes are stored in VoidFlame-Core storage.",
                "§eClick §8» §fOpen editor");
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder)) return;

        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;

        int slot = event.getRawSlot();
        if (slot == 49) {
            player.closeInventory();
            return;
        }

        List<String> kits = plugin.catalog().kits();
        int index = gridIndex(slot);
        if (index < 0 || index >= kits.size()) return;

        plugin.editor().openAdmin(player, kits.get(index));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    private int gridIndex(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        if (row < 1 || row > 4 || col < 1 || col > 7) return -1;
        return (row - 1) * 7 + (col - 1);
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> lines = new ArrayList<>();
            for (String line : lore) lines.add(line);
            meta.setLore(lines);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String pretty(String id) {
        return id.replace('_', ' ');
    }

    private static final class Holder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }
}
