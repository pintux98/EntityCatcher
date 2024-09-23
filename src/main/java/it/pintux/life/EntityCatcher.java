package it.pintux.life;

import it.pintux.life.catcher.CatcherManager;
import it.pintux.life.catcher.EntityListener;
import it.pintux.life.cmds.CatcherCommand;
import it.pintux.life.protection.GriefPreventionProtection;
import it.pintux.life.protection.HuskClaimsProtection;
import it.pintux.life.protection.WorldGuardProtection;
import it.pintux.life.utils.CooldownHandler;
import it.pintux.life.utils.MessageData;
import it.pintux.life.utils.Metrics;
import it.pintux.life.utils.ProtectionManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class EntityCatcher extends JavaPlugin {

    private CatcherManager catcherManager;
    private CooldownHandler cooldownHandler;
    private boolean isPlaceholderAPI;
    private ProtectionManager protectionManager;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new EntityListener(this), this);
        getCommand("entitycatcher").setExecutor(new CatcherCommand(this));
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            isPlaceholderAPI = true;
        }
        new Metrics(this, 0);
        saveDefaultConfig();
        reloadData();
        this.protectionManager = new ProtectionManager();
        if (getServer().getPluginManager().getPlugin("HuskClaims") != null) {
            protectionManager.addHandler(new HuskClaimsProtection());
        }
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            protectionManager.addHandler(new WorldGuardProtection());
        }
        if (getServer().getPluginManager().getPlugin("GriefPrevention") != null) {
            protectionManager.addHandler(new GriefPreventionProtection());
        }
    }

    @Override
    public void onDisable() {
        cooldownHandler.closeConnection();
    }

    public void reloadData() {
        this.saveResource("messages.yml", false);
        new MessageData(this, "messages.yml");
        reloadConfig();
        this.cooldownHandler = new CooldownHandler(this);
        this.catcherManager = new CatcherManager(this);
    }

    public CatcherManager getCatcherManager() {
        return catcherManager;
    }

    public CooldownHandler getCooldownHandler() {
        return cooldownHandler;
    }

    public boolean isPlaceholderAPI() {
        return isPlaceholderAPI;
    }

    public ProtectionManager getProtectionManager() {
        return protectionManager;
    }
}
