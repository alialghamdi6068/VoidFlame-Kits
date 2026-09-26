package net.voidflame.kits;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class KitCatalog {
    public static final List<String> KITS = List.of(
            "sword", "axe", "uhc", "mace", "spear_mace", "crystal", "netherite_op"
    );

    private final VoidFlameKitsPlugin plugin;
    private final Map<String, KitLayout> layouts = new LinkedHashMap<>();

    public KitCatalog(VoidFlameKitsPlugin plugin) {
        this.plugin = plugin;
        registerCanonicalLayouts();
    }

    public List<String> kits() {
        return KITS;
    }

    public boolean exists(String id) {
        return id != null && KITS.contains(id.toLowerCase());
    }

    public KitLayout layout(String id) {
        return id == null ? null : layouts.get(id.toLowerCase());
    }

    private void registerCanonicalLayouts() {
        layouts.put("sword", new KitLayout("sword", 0, 8, 36, 37, 38, 39));
        layouts.put("axe", new KitLayout("axe", 0, 8, 36, 37, 38, 39));
        layouts.put("uhc", new KitLayout("uhc", 0, 8, 9, 36, 37, 38, 39));
        layouts.put("mace", new KitLayout("mace", 0, 8, 36, 37, 38, 39));
        layouts.put("spear_mace", new KitLayout("spear_mace", 0, 8, 36, 37, 38, 39));
        layouts.put("crystal", new KitLayout("crystal", 0, 8, 36, 37, 38, 39));
        layouts.put("netherite_op", new KitLayout("netherite_op", 0, 8, 36, 37, 38, 39));
    }
}
