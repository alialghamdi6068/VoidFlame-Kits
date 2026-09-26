package net.voidflame.kits;

import org.bukkit.Material;
import java.util.List;
import java.util.Map;

public record KitDefinition(
        String id,
        String displayName,
        int hotbarSize,
        int inventorySize,
        List<Integer> armorSlots,
        Map<Integer, Material> reservedSlots,
        boolean allowOffhand
) {
    public KitDefinition {
        reservedSlots = Map.copyOf(reservedSlots);
        armorSlots = List.copyOf(armorSlots);
    }

    static KitDefinition standard(String id) {
        return new KitDefinition(id, id.replace('_',' '), 9, 36, List.of(36,37,38,39), Map.of(), true);
    }

    static KitDefinition uhc() {
        return new KitDefinition("uhc", "UHC", 9, 36, List.of(36,37,38,39), Map.of(8, Material.WATER_BUCKET), true);
    }

    static KitDefinition crystal() {
        return new KitDefinition("crystal", "Crystal", 9, 36, List.of(36,37,38,39),
                Map.of(7, Material.OBSIDIAN, 8, Material.END_CRYSTAL), true);
    }

}
