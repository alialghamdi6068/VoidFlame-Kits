package net.voidflame.kits;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class KitCommand implements CommandExecutor {
    private final VoidFlameKitsPlugin plugin;
    public KitCommand(VoidFlameKitsPlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length == 0) {
            player.sendMessage(ChatColor.AQUA + "Available kits: " + String.join(", ", plugin.catalog().kits()));
            return true;
        }
        if (!plugin.kitService().apply(player, args[0])) {
            player.sendMessage(ChatColor.RED + "Unknown kit.");
            return true;
        }
        player.sendMessage(ChatColor.GREEN + "Kit applied: " + args[0]);
        return true;
    }
}
