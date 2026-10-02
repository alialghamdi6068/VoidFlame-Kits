package net.voidflame.kits;

import org.bukkit.inventory.Inventory;

import java.util.UUID;

public record KitEditorSession(UUID playerId, String kitId, String layoutName, Inventory inventory, boolean admin) { }
