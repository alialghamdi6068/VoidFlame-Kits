package net.voidflame.kits;

import org.bukkit.plugin.ServicePriority;

import java.util.List;

public final class KitCatalog {
    public static final List<String> KITS = List.of(
            "sword", "axe", "uhc", "mace", "spear_mace", "crystal", "netherite_op"
    );

    private final VoidFlameKitsPlugin plugin;

    public KitCatalog(VoidFlameKitsPlugin plugin) {
        this.plugin = plugin;
    }

    public List<String> kits() {
        return KITS;
    }

    public boolean exists(String id) {
        return KITS.contains(id.toLowerCase());
    }
}
