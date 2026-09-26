package net.voidflame.kits;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class KitCatalog {
    public static final List<String> KITS = List.of("sword","axe","uhc","mace","spear_mace","crystal","netherite_pot");

    private final Map<String, KitDefinition> definitions = new LinkedHashMap<>();

    public KitCatalog() {
        registerDefaults();
    }

    public List<String> kits() { return List.copyOf(definitions.keySet()); }

    public boolean exists(String id) { return id != null && definitions.containsKey(id.toLowerCase()); }

    public KitDefinition get(String id) { return id == null ? null : definitions.get(id.toLowerCase()); }

    private void registerDefaults() {
        for (String id : KITS) {
            definitions.put(id, KitDefinition.standard(id));
        }
        definitions.put("uhc", KitDefinition.uhc());
        definitions.put("crystal", KitDefinition.crystal());
    }
}
