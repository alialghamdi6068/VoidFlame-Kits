package net.voidflame.kits;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class NativeKitAdminMenu {
    private final VoidFlameKitsPlugin plugin;

    public NativeKitAdminMenu(VoidFlameKitsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        if (!authorized(player)) return;
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text("BUILT-IN AND CUSTOM KITS", NamedTextColor.LIGHT_PURPLE), 300));
        body.add(DialogBody.plainMessage(Component.text("Select a kit to manage its status, default selection, or layout.", NamedTextColor.GRAY), 300));
        List<ActionButton> actions = new ArrayList<>();
        for (String id : plugin.catalog().kits()) {
            boolean enabled = plugin.catalog().enabled(id);
            String name = plugin.catalog().displayName(id);
            body.add(DialogBody.item(new ItemStack(plugin.catalog().icon(id)),
                    DialogBody.plainMessage(Component.text(name + "  •  " + (enabled ? "ENABLED" : "DISABLED")
                            + (plugin.defaultKit().equalsIgnoreCase(id) ? "  •  DEFAULT" : ""), enabled ? NamedTextColor.GREEN : NamedTextColor.RED), 220),
                    true, true, 40, 40));
            actions.add(button(name, "Open kit management.", "/kitadmin manage " + id,
                    enabled ? NamedTextColor.GREEN : NamedTextColor.RED));
        }
        actions.add(button("Add Custom Kit", "Create a custom kit using the existing guided flow.", "/kitadmin create", NamedTextColor.GOLD));
        actions.add(button("Close", "Close this screen.", "/vfmenu", NamedTextColor.GRAY));
        show(player, "VoidFlame • Kit Administration", body, actions, 2);
    }

    public void openKit(Player player, String id) {
        if (!authorized(player)) return;
        if (!plugin.catalog().exists(id)) {
            player.sendMessage(Component.text("Unknown kit: " + id, NamedTextColor.RED));
            open(player);
            return;
        }
        String name = plugin.catalog().displayName(id);
        boolean enabled = plugin.catalog().enabled(id);
        boolean isDefault = plugin.defaultKit().equalsIgnoreCase(id);
        List<DialogBody> body = List.of(
                DialogBody.item(new ItemStack(plugin.catalog().icon(id)),
                        DialogBody.plainMessage(Component.text(name + "  •  " + id, NamedTextColor.LIGHT_PURPLE), 220),
                        true, true, 64, 64),
                DialogBody.plainMessage(Component.text("Status: " + (enabled ? "Enabled" : "Disabled"), enabled ? NamedTextColor.GREEN : NamedTextColor.RED), 300),
                DialogBody.plainMessage(Component.text("Default kit: " + (isDefault ? "Yes" : "No"), isDefault ? NamedTextColor.GOLD : NamedTextColor.GRAY), 300)
        );
        show(player, "VoidFlame • " + name, body, List.of(
                button(enabled ? "Disable Kit" : "Enable Kit", "Toggle this kit's availability.", "/kitadmin toggle " + id, enabled ? NamedTextColor.RED : NamedTextColor.GREEN),
                button(isDefault ? "Current Default" : "Set as Default", "Set this kit as the server's default.", "/kitadmin default " + id, NamedTextColor.GOLD),
                button("Edit Layout", "Open the kit layout editor.", "/kitadmin layout " + id, NamedTextColor.AQUA),
                button("Kit Settings", "Open detailed kit configuration.", "/kitadmin settings " + id, NamedTextColor.LIGHT_PURPLE),
                button("Back to Kits", "Return to kit administration.", "/kitadmin", NamedTextColor.GRAY)
        ), 2);
    }

    private boolean authorized(Player player) {
        if (player.hasPermission("voidflame.kits.manage")) return true;
        player.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
        return false;
    }

    private void show(Player player, String title, List<DialogBody> body, List<ActionButton> actions, int columns) {
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(title, NamedTextColor.WHITE))
                        .externalTitle(Component.text("VoidFlame", NamedTextColor.LIGHT_PURPLE))
                        .canCloseWithEscape(true)
                        .body(body)
                        .build())
                .type(DialogType.multiAction(actions).columns(columns).build()));
        player.showDialog(dialog);
    }

    private ActionButton button(String label, String tooltip, String command, NamedTextColor color) {
        return ActionButton.create(Component.text(label, color), Component.text(tooltip, NamedTextColor.GRAY),
                180, DialogAction.commandTemplate(command));
    }
}
