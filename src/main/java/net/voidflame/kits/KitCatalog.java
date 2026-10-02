package net.voidflame.kits;

import org.bukkit.Material;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class KitCatalog {
    public static final List<String> KITS = List.of("sword","axe","uhc","mace","crystal","netherite_pot","smp","spear_mace");

    private final Map<String, KitDefinition> definitions = new LinkedHashMap<>();
    private final Set<String> disabled = ConcurrentHashMap.newKeySet();
    private final Map<String, String> displayNames = new ConcurrentHashMap<>();
    private final Map<String, Material> icons = new ConcurrentHashMap<>();
    private final Map<String, Integer> orders = new ConcurrentHashMap<>();

    public KitCatalog() { registerDefaults(); }

    public synchronized List<String> kits() {
        return definitions.keySet().stream()
                .sorted(Comparator.comparingInt((String id) -> orders.getOrDefault(id, 0))
                        .thenComparing(id -> id))
                .toList();
    }

    public boolean exists(String id) { return id != null && definitions.containsKey(id.toLowerCase(Locale.ROOT)); }
    public KitDefinition get(String id) { return id == null ? null : definitions.get(id.toLowerCase(Locale.ROOT)); }
    public boolean enabled(String id) { return id != null && !disabled.contains(id.toLowerCase(Locale.ROOT)); }
    public String displayName(String id) {
        String normalized = id == null ? "" : id.toLowerCase(Locale.ROOT);
        return displayNames.getOrDefault(normalized, normalized.replace('_', ' '));
    }
    public Material icon(String id) { return icons.getOrDefault(id.toLowerCase(Locale.ROOT), Material.NAME_TAG); }
    public int order(String id) { return orders.getOrDefault(id.toLowerCase(Locale.ROOT), 0); }

    public synchronized boolean add(String id) {
        if (id == null) return false;
        String normalized = id.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (!normalized.matches("[a-z0-9_]{2,32}") || definitions.containsKey(normalized)) return false;
        definitions.put(normalized, KitDefinition.standard(normalized));
        displayNames.put(normalized, normalized.replace('_', ' '));
        icons.put(normalized, Material.NAME_TAG);
        orders.put(normalized, definitions.size());
        disabled.add(normalized);
        return true;
    }

    public synchronized boolean remove(String id) {
        String normalized = id == null ? "" : id.toLowerCase(Locale.ROOT);
        if (!definitions.containsKey(normalized) || KITS.contains(normalized)) return false;
        definitions.remove(normalized);
        disabled.remove(normalized);
        displayNames.remove(normalized);
        icons.remove(normalized);
        orders.remove(normalized);
        return true;
    }

    public void setEnabled(String id, boolean value) {
        if (value) disabled.remove(id.toLowerCase(Locale.ROOT));
        else disabled.add(id.toLowerCase(Locale.ROOT));
    }
    public void setDisplayName(String id, String value) { displayNames.put(id.toLowerCase(Locale.ROOT), value); }
    public void setIcon(String id, Material value) { if (value != null) icons.put(id.toLowerCase(Locale.ROOT), value); }
    public void setOrder(String id, int value) { orders.put(id.toLowerCase(Locale.ROOT), value); }

    private void registerDefaults() {
        for (String id : KITS) {
            definitions.put(id, KitDefinition.standard(id));
            displayNames.put(id, id.replace('_', ' '));
            icons.put(id, Material.NAME_TAG);
            orders.put(id, definitions.size());
        }
        definitions.put("uhc", KitDefinition.uhc());
        definitions.put("crystal", KitDefinition.crystal());
    }
}
