package net.voidflame.kits;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class GoldenHeadListener implements Listener {
    private final NamespacedKey goldenHeadKey;

    public GoldenHeadListener(VoidFlameKitsPlugin plugin) {
        this.goldenHeadKey = new NamespacedKey(plugin, "golden_head");
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (!isGoldenHead(item)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        item.setAmount(item.getAmount() - 1);
        if (event.getHand() == EquipmentSlot.HAND) {
            player.getInventory().setItemInMainHand(item.getAmount() > 0 ? item : null);
        } else if (event.getHand() == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(item.getAmount() > 0 ? item : null);
        }

        // Golden Head: +4 hearts immediate healing and +2 absorption hearts.
        player.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 1, false, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 0, false, true, true));
    }

    private boolean isGoldenHead(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(goldenHeadKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }
}
