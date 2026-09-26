package net.voidflame.kits;

import java.util.Arrays;
import java.util.List;

public record KitLayout(String id, int hotbarStart, int hotbarEnd, int helmetSlot, int chestplateSlot, int leggingsSlot, int bootsSlot) {
    public KitLayout {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id");
        if (hotbarStart < 0 || hotbarEnd > 8 || hotbarStart > hotbarEnd) throw new IllegalArgumentException("hotbar");
    }

    public List<Integer> armorSlots() {
        return Arrays.asList(helmetSlot, chestplateSlot, leggingsSlot, bootsSlot);
    }
}
