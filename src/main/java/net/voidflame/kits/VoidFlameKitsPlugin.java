package net.voidflame.kits;

import net.voidflame.core.storage.StorageService;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import org.bukkit.Material;
import java.util.Arrays;

public final class VoidFlameKitsPlugin extends JavaPlugin {
    private StorageService storage;
    private KitCatalog catalog;
    private net.voidflame.kits.KitService kitService;
    private KitEditorManager editor;
    private KitAdminMenu adminMenu;
    private KitAdminSettingsMenu settingsMenu;
    private KitGuiSettingsMenu guiSettingsMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!connectStorage()) {
            getLogger().severe("VoidFlame-Core storage service is unavailable.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        catalog = new KitCatalog();
        loadCatalog();
        kitService = new KitService(this);
        editor = new KitEditorManager(this);
        adminMenu = new KitAdminMenu(this);
        settingsMenu = new KitAdminSettingsMenu(this);
        guiSettingsMenu = new KitGuiSettingsMenu(this);
        var validationErrors = kitService.validateConfiguration();
        if (!validationErrors.isEmpty()) {
            validationErrors.forEach(error -> getLogger().severe("[Config] " + error));
            if (getConfig().getBoolean("settings.strict-validation", true)) {
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }
        KitCommand commandHandler = new KitCommand(this);
        getCommand("kit").setExecutor(commandHandler);
        getCommand("kit").setTabCompleter(commandHandler);
        getCommand("kits").setExecutor(commandHandler);
        getCommand("kits").setTabCompleter(commandHandler);
        getCommand("kitadmin").setExecutor((sender, command, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player player)) return true;
            if (!player.hasPermission("voidflame.kits.manage")) {
                player.sendMessage(org.bukkit.ChatColor.RED + "You do not have permission.");
                return true;
            }
            if (args.length > 0 && args[0].equalsIgnoreCase("gui")) {
                guiSettingsMenu.open(player);
            } else {
                adminMenu.open(player);
            }
            return true;
        });
        getServer().getPluginManager().registerEvents(new GoldenHeadListener(this), this);
        getServer().getPluginManager().registerEvents(kitService, this);
        kitService.loadServerLayouts();
        getServer().getPluginManager().registerEvents(editor, this);
        getServer().getPluginManager().registerEvents(adminMenu, this);
        getServer().getPluginManager().registerEvents(settingsMenu, this);
        getServer().getPluginManager().registerEvents(guiSettingsMenu, this);
        getServer().getPluginManager().registerEvents(new KitDuplicationGuard(this), this);
        getServer().getServicesManager().register(KitCatalog.class, catalog, this, ServicePriority.Normal);
        getServer().getServicesManager().register(net.voidflame.core.api.KitService.class, kitService, this, ServicePriority.Normal);
        getLogger().info("VoidFlame-Kits enabled with canonical kit catalog.");
    }

    private boolean connectStorage() {
        RegisteredServiceProvider<StorageService> registration =
                getServer().getServicesManager().getRegistration(StorageService.class);
        if (registration == null || registration.getProvider() == null) return false;
        storage = registration.getProvider();
        return true;
    }


    private void loadCatalog() {
        get("admin-catalog").thenAccept(raw -> {
            if (raw == null || raw.isBlank()) return;
            for (String line : raw.split("\\n")) {
                String[] p = line.split("\\|", -1);
                if (p.length < 5) continue;
                String id = p[0].toLowerCase(java.util.Locale.ROOT);
                if (id.equals("smp") || id.equals("netherite_pot")) continue;
                if (!catalog.exists(id)) catalog.add(id);
                catalog.setDisplayName(id, p[1].isBlank() ? id.replace('_', ' ') : p[1]);
                Material icon = Material.matchMaterial(p[2]);
                if (icon != null) catalog.setIcon(id, icon);
                catalog.setEnabled(id, Boolean.parseBoolean(p[3]));
                try { catalog.setOrder(id, Integer.parseInt(p[4])); } catch (NumberFormatException ignored) {}
                if (p.length >= 6) catalog.setSlot(id, parseInt(p[5], catalog.slot(id)));
                if (p.length >= 7) catalog.setLore(id, Arrays.asList(p[6].split("~", -1)));
            }
        });
    }

    public CompletableFuture<Void> saveCatalog() {
        StringBuilder out = new StringBuilder();
        for (String id : catalog.kits()) {
            out.append(id).append('|')
                    .append(catalog.displayName(id).replace("|", "/")).append('|')
                    .append(catalog.icon(id).name()).append('|')
                    .append(catalog.enabled(id)).append('|')
                    .append(catalog.order(id)).append('\n');
        }
        return put("admin-catalog", out.toString());
    }

    public String defaultKit() {
        return getConfig().getString("settings.default-kit", "sword").toLowerCase(java.util.Locale.ROOT);
    }

    public CompletableFuture<Void> setDefaultKit(String kit) {
        getConfig().set("settings.default-kit", kit.toLowerCase(java.util.Locale.ROOT));
        saveConfig();
        return put("settings.default-kit", kit.toLowerCase(java.util.Locale.ROOT));
    }

    private int parseInt(String value, int fallback) { try { return Integer.parseInt(value); } catch (NumberFormatException ignored) { return fallback; } }

    public CompletableFuture<Void> put(String key, String value) {
        return storage.put("kits", key, value);
    }

    public CompletableFuture<String> get(String key) {
        return storage.get("kits", key);
    }

    public StorageService storage() {
        return storage;
    }

    public net.voidflame.kits.KitService kitService() { return kitService; }

    public KitEditorManager editor() { return editor; }

    public KitAdminMenu adminMenu() { return adminMenu; }
    public KitAdminSettingsMenu settingsMenu() { return settingsMenu; }
    public KitGuiSettingsMenu guiSettingsMenu() { return guiSettingsMenu; }

    public KitCatalog catalog() {
        return catalog;
    }

    @Override
    public void onDisable() {
        if (kitService != null) kitService.clearSelected();
        getServer().getServicesManager().unregister(KitCatalog.class, this);
        getServer().getServicesManager().unregister(net.voidflame.core.api.KitService.class, kitService);
    }
}
