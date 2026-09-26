package net.voidflame.kits;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public final class VoidFlameKitsPlugin extends JavaPlugin {
    private Object storage;
    private Method put;
    private Method get;
    private KitCatalog catalog;
    private KitService kitService;

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
        getServer().getServicesManager().register(KitCatalog.class, catalog, this, ServicePriority.Normal);
        getLogger().info("VoidFlame-Kits enabled with canonical seven-kit catalog.");
    }

    private boolean connectStorage() {
        try {
            Class<?> type = Class.forName("net.voidflame.core.storage.StorageService");
            RegisteredServiceProvider<?> registration = getServer().getServicesManager().getRegistration(type);
            if (registration == null) return false;
            storage = registration.getProvider();
            put = type.getMethod("put", String.class, String.class, String.class);
            get = type.getMethod("get", String.class, String.class);
            return true;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }

    public CompletableFuture<Void> put(String key, String value) {
        try {
            return (CompletableFuture<Void>) put.invoke(storage, "kits", key, value);
        } catch (ReflectiveOperationException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    public CompletableFuture<String> get(String key) {
        try {
            return (CompletableFuture<String>) get.invoke(storage, "kits", key);
        } catch (ReflectiveOperationException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    public KitService kitService() { return kitService; }

    public KitCatalog catalog() {
        return catalog;
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregister(KitCatalog.class, this);
    }
}
