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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KitAdminSettingsMenu implements Listener {
    private final VoidFlameKitsPlugin plugin;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public KitAdminSettingsMenu(VoidFlameKitsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player p, String kit) {
        if (!p.hasPermission("voidflame.kits.manage") || !plugin.catalog().exists(kit)) return;
        Inventory inv = Bukkit.createInventory(new Holder(kit), 54,
                "§8VoidFlame §7• §5Kit Editor §8• §f" + plugin.catalog().displayName(kit));
        for (int i = 0; i < 54; i++) inv.setItem(i, item(Material.BLACK_STAINED_GLASS_PANE, " "));
        button(inv, 10, Material.CHEST, "§d§lLAYOUT", "§7Edit every saved inventory slot.", "§eClick §8» §fOpen editor");
        button(inv, 11, Material.NAME_TAG, "§d§lNAME", "§7Current: §f" + plugin.catalog().displayName(kit), "§eClick §8» §fEdit in chat");
        button(inv, 12, Material.WRITABLE_BOOK, "§d§lLORE", "§7" + (plugin.catalog().lore(kit).isEmpty() ? "No lore" : String.join(" §8/ §7", plugin.catalog().lore(kit))), "§eClick §8» §fEdit in chat");
        button(inv, 13, plugin.catalog().icon(kit), "§d§lICON", "§7Choose any item icon.", "§eClick §8» §fOpen icon picker");
        button(inv, 14, Material.COMPARATOR, "§d§lORDER", "§7Current: §f" + plugin.catalog().order(kit), "§eClick §8» §fEdit in chat");
        button(inv, 15, Material.ENDER_PEARL, "§d§lGUI SLOT", "§7Current: §f" + plugin.catalog().slot(kit), "§eClick §8» §fEdit slot 0-53");
        button(inv, 16, Material.CHEST_MINECART, "§b§lDUPLICATE", "§7Make a full metadata copy.", "§eClick §8» §fEnter new ID");
        button(inv, 19, plugin.catalog().enabled(kit) ? Material.LIME_DYE : Material.GRAY_DYE,
                plugin.catalog().enabled(kit) ? "§a§lENABLED" : "§c§lDISABLED", "§eClick §8» §fToggle");
        button(inv, 20, Material.GOLDEN_APPLE, "§6§lDEFAULT", "§7Default kit: §f" + plugin.catalog().displayName(plugin.defaultKit()), "§eClick §8» §fSet this as default");
        button(inv, 21, Material.PAPER, "§e§lPREVIEW", "§7Show all metadata in chat.");
        button(inv, 22, Material.REDSTONE_BLOCK, "§c§lDELETE", "§7Delete this custom kit.", "§cClick §8» §fDelete");
        button(inv, 49, Material.ARROW, "§7§lBACK");
        button(inv, 53, Material.BARRIER, "§c§lCLOSE");
        p.openInventory(inv);
    }

    @EventHandler
    public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof Holder h)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;

        String k = h.kit();
        switch (e.getRawSlot()) {
            case 10 -> plugin.editor().openAdmin(p, k);
            case 11 -> prompt(p, k, "name", plugin.catalog().displayName(k));
            case 12 -> prompt(p, k, "lore", String.join("|", plugin.catalog().lore(k)));
            case 13 -> icons(p, k);
            case 14 -> prompt(p, k, "order", Integer.toString(plugin.catalog().order(k)));
            case 15 -> prompt(p, k, "slot", Integer.toString(plugin.catalog().slot(k)));
            case 16 -> prompt(p, k, "duplicate", k + "_copy");
            case 19 -> { plugin.catalog().setEnabled(k, !plugin.catalog().enabled(k)); plugin.saveCatalog(); open(p, k); }
            case 20 -> { plugin.setDefaultKit(k); p.sendMessage("§aDefault kit set to §f" + plugin.catalog().displayName(k) + "§a."); open(p, k); }
            case 21 -> preview(p, k);
            case 22 -> {
                if (KitCatalog.KITS.contains(k)) {
                    p.sendMessage("§cBuilt-in kits cannot be deleted.");
                } else if (plugin.catalog().remove(k)) {
                    plugin.saveCatalog();
                    plugin.adminMenu().open(p);
                }
            }
            case 49 -> plugin.adminMenu().open(p);
            case 53 -> p.closeInventory();
            default -> {}
        }
    }

    private void prompt(Player p, String kit, String mode, String current) {
        pending.put(p.getUniqueId(), new Pending(kit, mode));
        p.closeInventory();
        p.sendMessage("§5§lVOIDFLAME §8» §fType the new §d" + mode + " §fin chat.");
        p.sendMessage("§7Current: §f" + current + " §8• §7Type §ccancel §7to stop.");
    }

    @EventHandler
    public void chat(AsyncPlayerChatEvent e) {
        Pending request = pending.remove(e.getPlayer().getUniqueId());
        if (request == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        String value = e.getMessage().trim();
        Bukkit.getScheduler().runTask(plugin, () -> apply(p, request, value));
    }

    private void apply(Player p, Pending request, String value) {
        if (!p.hasPermission("voidflame.kits.manage")) return;
        if (value.equalsIgnoreCase("cancel")) {
            open(p, request.kit());
            return;
        }
        String k = request.kit();
        switch (request.mode()) {
            case "name" -> plugin.catalog().setDisplayName(k, value);
            case "lore" -> plugin.catalog().setLore(k, java.util.Arrays.stream(value.split("\\|")).map(String::trim).filter(s -> !s.isBlank()).limit(12).toList());
            case "order" -> {
                try { plugin.catalog().setOrder(k, Integer.parseInt(value)); }
                catch (NumberFormatException ex) { p.sendMessage("§cOrder must be a number."); open(p, k); return; }
            }
            case "slot" -> {
                try {
                    int slot = Integer.parseInt(value);
                    if (slot < 0 || slot > 53) throw new NumberFormatException();
                    plugin.catalog().setSlot(k, slot);
                } catch (NumberFormatException ex) {
                    p.sendMessage("§cGUI slot must be between 0 and 53.");
                    open(p, k);
                    return;
                }
            }
            case "duplicate" -> {
                String id = value.toLowerCase(Locale.ROOT).replace(' ', '_');
                if (!plugin.catalog().add(id)) {
                    p.sendMessage("§cInvalid or duplicate kit ID.");
                    open(p, k);
                    return;
                }
                plugin.catalog().setDisplayName(id, plugin.catalog().displayName(k) + " Copy");
                plugin.catalog().setIcon(id, plugin.catalog().icon(k));
                plugin.catalog().setLore(id, plugin.catalog().lore(k));
                plugin.catalog().setOrder(id, plugin.catalog().order(k) + 1);
                plugin.catalog().setSlot(id, plugin.catalog().slot(k));
                plugin.catalog().setEnabled(id, false);
                plugin.saveCatalog();
                p.sendMessage("§aCreated disabled copy: §f" + id);
                open(p, id);
                return;
            }
            default -> {}
        }
        plugin.saveCatalog();
        p.sendMessage("§aSaved §f" + request.mode() + " §afor §f" + plugin.catalog().displayName(k) + "§a.");
        open(p, k);
    }

    private void icons(Player p, String kit) {
        Inventory i = Bukkit.createInventory(new IconHolder(kit), 54, "§8VoidFlame §7• §5Choose Kit Icon");
        for (int s = 0; s < 54; s++) i.setItem(s, item(Material.BLACK_STAINED_GLASS_PANE, " "));
        int slot = 10;
        for (Material m : Material.values()) {
            if (!m.isItem() || m == Material.AIR) continue;
            i.setItem(slot++, new ItemStack(m));
            if (slot == 44) break;
        }
        i.setItem(49, item(Material.ARROW, "§7§lBACK"));
        p.openInventory(i);
    }

    @EventHandler
    public void iconClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof IconHolder h)) return;
        e.setCancelled(true);
        if (e.getRawSlot() == 49) { open(p, h.kit()); return; }
        ItemStack x = e.getCurrentItem();
        if (x == null || !x.getType().isItem()) return;
        plugin.catalog().setIcon(h.kit(), x.getType());
        plugin.saveCatalog();
        open(p, h.kit());
    }

    @EventHandler
    public void drag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Holder ||
                e.getView().getTopInventory().getHolder() instanceof IconHolder) e.setCancelled(true);
    }

    private void preview(Player p, String k) {
        p.sendMessage("§5§lKIT §8» §f" + plugin.catalog().displayName(k));
        p.sendMessage("§7ID: §f" + k + " §8| §7Enabled: §f" + plugin.catalog().enabled(k));
        p.sendMessage("§7Icon: §f" + plugin.catalog().icon(k).name() + " §8| §7Order: §f" + plugin.catalog().order(k) + " §8| §7GUI slot: §f" + plugin.catalog().slot(k));
        p.sendMessage("§7Lore: §f" + (plugin.catalog().lore(k).isEmpty() ? "none" : String.join(" §8/ §f", plugin.catalog().lore(k))));
    }

    private void button(Inventory i, int s, Material m, String n, String... l) { i.setItem(s, item(m, n, l)); }
    private ItemStack item(Material m, String n, String... l) {
        ItemStack x = new ItemStack(m);
        ItemMeta z = x.getItemMeta();
        if (z != null) { z.setDisplayName(n); z.setLore(List.of(l)); x.setItemMeta(z); }
        return x;
    }

    private record Pending(String kit, String mode) {}
    private record Holder(String kit) implements InventoryHolder { public Inventory getInventory() { return null; } }
    private record IconHolder(String kit) implements InventoryHolder { public Inventory getInventory() { return null; } }
}
