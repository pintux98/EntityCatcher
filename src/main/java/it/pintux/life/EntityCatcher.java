package it.pintux.life;

import it.pintux.life.catcher.CatcherManager;
import it.pintux.life.catcher.EntityListener;
import it.pintux.life.cmds.CatcherCommand;
import it.pintux.life.protection.EntityExclusionManager;
import it.pintux.life.protection.GriefPreventionProtection;
import it.pintux.life.protection.HuskClaimsProtection;
import it.pintux.life.protection.LandsProtection;
import it.pintux.life.protection.WorldGuardProtection;
import it.pintux.life.utils.CollectionGUI;
import it.pintux.life.utils.CooldownHandler;
import it.pintux.life.utils.MessageData;
import it.pintux.life.utils.ProtectionManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class EntityCatcher extends JavaPlugin {

    private CatcherManager catcherManager;
    private CooldownHandler cooldownHandler;
    private boolean isPlaceholderAPI;
    private ProtectionManager protectionManager;
    private EntityExclusionManager entityExclusionManager;
    private CollectionGUI collectionGUI;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new EntityListener(this), this);
        getCommand("entitycatcher").setExecutor(new CatcherCommand(this));
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            isPlaceholderAPI = true;
        }
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
        if (getServer().getPluginManager().getPlugin("Lands") != null) {
            protectionManager.addHandler(new LandsProtection(this));
        }

        boolean mythicMobsPresent = getServer().getPluginManager().getPlugin("MythicMobs") != null;
        boolean citizensPresent = getServer().getPluginManager().getPlugin("Citizens") != null;
        Set<String> denylistedMobs = loadDenylistedMobs();
        this.entityExclusionManager = new EntityExclusionManager(mythicMobsPresent, citizensPresent, denylistedMobs);
        this.collectionGUI = new CollectionGUI(this);
    }

    private Set<String> loadDenylistedMobs() {
        ConfigurationSection section = getConfig().getConfigurationSection("exclusions.mythicmobs");
        if (section == null) {
            return Collections.emptySet();
        }
        Set<String> mobs = new HashSet<>();
        for (String key : section.getKeys(false)) {
            if (section.getBoolean(key)) {
                mobs.add(key);
            }
        }
        return mobs;
    }

    @Override
    public void onDisable() {
        if (cooldownHandler != null) {
            cooldownHandler.closeConnection();
        }
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

    public EntityExclusionManager getEntityExclusionManager() {
        return entityExclusionManager;
    }

    public CollectionGUI getCollectionGUI() {
        return collectionGUI;
    }
}
