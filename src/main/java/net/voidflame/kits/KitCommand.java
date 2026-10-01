package net.voidflame.kits;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.Locale;
import org.bukkit.entity.Player;

public final class KitCommand implements CommandExecutor, TabCompleter {
    private final VoidFlameKitsPlugin plugin;
    public KitCommand(VoidFlameKitsPlugin plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (!plugin.getConfig().getBoolean("settings.enabled", true)) {
            player.sendMessage(ChatColor.RED + "The kit system is disabled.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(ChatColor.AQUA + "Available kits: " + String.join(", ", plugin.catalog().kits()));
            return true;
        }
        if (args[0].equalsIgnoreCase("list")) {
            player.sendMessage(ChatColor.AQUA + "Available kits: " + String.join(", ", plugin.catalog().kits()));
            return true;
        }
        if (args[0].equalsIgnoreCase("editor")) {
            if (!player.hasPermission("voidflame.kits.edit") || !plugin.getConfig().getBoolean("settings.editor-enabled", true)) {
                player.sendMessage(ChatColor.RED + "Kit editor is disabled or you do not have permission.");
                return true;
            }
            player.sendMessage(ChatColor.YELLOW + "The layout editor is managed by the server's kit menu.");
            return true;
        }
        if (!plugin.kitService().apply(player, args[0])) {
            player.sendMessage(ChatColor.RED + "Unknown kit.");
            return true;
        }
        player.sendMessage(ChatColor.GREEN + "Kit applied: " + args[0]);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            java.util.ArrayList<String> values = new java.util.ArrayList<>(plugin.catalog().kits());
            values.add("list");
            if (sender.hasPermission("voidflame.kits.edit")) values.add("editor");
            String input = args[0].toLowerCase(Locale.ROOT);
            return values.stream().filter(v -> v.toLowerCase(Locale.ROOT).startsWith(input)).toList();
        }
        return List.of();
    }
}
