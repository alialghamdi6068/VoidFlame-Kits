package net.voidflame.kits;

import net.voidflame.core.storage.StorageService;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

public final class VoidFlameKitsPlugin extends JavaPlugin {
    private StorageService storage;
    private KitCatalog catalog;
    private net.voidflame.kits.KitService kitService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!connectStorage()) {
            getLogger().severe("VoidFlame-Core storage service is unavailable.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        catalog = new KitCatalog();
        kitService = new KitService(this);
        getCommand("kit").setExecutor(new KitCommand(this));
        getCommand("kits").setExecutor(new KitCommand(this));
        getServer().getPluginManager().registerEvents(new GoldenHeadListener(this), this);
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

    public KitCatalog catalog() {
        return catalog;
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregister(KitCatalog.class, this);
        getServer().getServicesManager().unregister(net.voidflame.core.api.KitService.class, kitService);
    }
}
