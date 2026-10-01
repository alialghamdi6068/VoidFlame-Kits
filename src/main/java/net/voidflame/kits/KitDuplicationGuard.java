package net.voidflame.kits;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class KitDuplicationGuard implements Listener {
    private final VoidFlameKitsPlugin plugin;
    private final NamespacedKey kitItemKey;

    public KitDuplicationGuard(VoidFlameKitsPlugin plugin) {
        this.plugin = plugin;
        this.kitItemKey = new NamespacedKey(plugin, "kit_item");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!plugin.getConfig().getBoolean("settings.prevent-duplication", true)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getView().getTitle().startsWith(ChatColor.DARK_PURPLE + "Kit Editor:")) return;
        if (event.getView().getTitle().startsWith(ChatColor.DARK_PURPLE + "Kit Editor:")) return;
        if (event.getClickedInventory() == null) return;
        if (event.getClickedInventory().equals(event.getView().getBottomInventory())) return;

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if (isKitItem(current) || isKitItem(cursor)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!plugin.getConfig().getBoolean("settings.prevent-duplication", true)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!isKitItem(event.getOldCursor())) return;
        if (event.getRawSlots().stream().anyMatch(slot -> slot < event.getView().getTopInventory().getSize())) {
            event.setCancelled(true);
        }
    }

    private boolean isKitItem(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(kitItemKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }
}
