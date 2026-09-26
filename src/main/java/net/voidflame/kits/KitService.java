package net.voidflame.kits;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class KitService {
    private final VoidFlameKitsPlugin plugin;
    private final Map<UUID, String> selected = new HashMap<>();

    public KitService(VoidFlameKitsPlugin plugin) { this.plugin = plugin; }

    public boolean apply(Player player, String id) {
        KitDefinition kit = plugin.catalog().get(id);
        if (kit == null) return false;
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));

        switch (kit.id()) {
            case "sword" -> {
                put(player, 0, Material.DIAMOND_SWORD, 1);
                put(player, 1, Material.GOLDEN_APPLE, 8);
                put(player, 2, Material.COBBLESTONE, 64);
                put(player, 3, Material.WATER_BUCKET, 1);
            }
            case "axe" -> {
                put(player, 0, Material.DIAMOND_AXE, 1);
                put(player, 1, Material.SHIELD, 1);
                put(player, 2, Material.GOLDEN_APPLE, 8);
                put(player, 3, Material.COBBLESTONE, 64);
            }
            case "uhc" -> {
                put(player, 0, Material.DIAMOND_SWORD, 1);
                put(player, 1, Material.BOW, 1);
                put(player, 2, Material.GOLDEN_APPLE, 6);
                put(player, 3, Material.ARROW, 32);
                put(player, 4, Material.WATER_BUCKET, 1);
                put(player, 5, Material.LAVA_BUCKET, 1);
                put(player, 6, Material.COBBLESTONE, 64);
                put(player, 7, Material.FISHING_ROD, 1);
                put(player, 8, Material.COOKED_BEEF, 32);
            }
            case "mace" -> {
                put(player, 0, Material.MACE, 1);
                put(player, 1, Material.ENDER_PEARL, 8);
                put(player, 2, Material.GOLDEN_APPLE, 8);
                put(player, 3, Material.COBBLESTONE, 64);
            }
            case "spear_mace" -> {
                put(player, 0, Material.SPEAR, 1);
                put(player, 1, Material.MACE, 1);
                put(player, 2, Material.ENDER_PEARL, 8);
                put(player, 3, Material.GOLDEN_APPLE, 8);
                put(player, 4, Material.COBBLESTONE, 64);
            }
            case "crystal" -> {
                put(player, 0, Material.NETHERITE_SWORD, 1);
                put(player, 1, Material.END_CRYSTAL, 64);
                put(player, 2, Material.OBSIDIAN, 64);
                put(player, 3, Material.TOTEM_OF_UNDYING, 4);
                put(player, 4, Material.ENDER_PEARL, 16);
                put(player, 5, Material.GOLDEN_APPLE, 16);
            }
            case "netherite_op" -> {
                put(player, 0, Material.NETHERITE_SWORD, 1);
                put(player, 1, Material.NETHERITE_AXE, 1);
                put(player, 2, Material.GOLDEN_APPLE, 16);
                put(player, 3, Material.ENDER_PEARL, 16);
                put(player, 4, Material.COBBLESTONE, 64);
                put(player, 5, Material.WATER_BUCKET, 1);
            }
            default -> { return false; }
        }
        selected.put(player.getUniqueId(), kit.id());
        return true;
    }

    private void put(Player player, int slot, Material material, int amount) {
        player.getInventory().setItem(slot, new ItemStack(material, amount));
    }

    public String selected(Player player) { return selected.get(player.getUniqueId()); }
}
