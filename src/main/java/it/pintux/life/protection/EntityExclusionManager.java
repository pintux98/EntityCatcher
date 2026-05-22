package it.pintux.life.protection;

import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.entity.Entity;

import java.util.Set;

public class EntityExclusionManager {

    private final boolean mythicMobsAvailable;
    private final boolean citizensAvailable;
    private final Set<String> denylistedMythicMobs;
    private io.lumine.mythic.bukkit.BukkitAPIHelper mythicHelper;

    public EntityExclusionManager(boolean mythicMobsPresent, boolean citizensPresent, Set<String> denylistedMobs) {
        this.mythicMobsAvailable = mythicMobsPresent;
        this.citizensAvailable = citizensPresent;
        this.denylistedMythicMobs = denylistedMobs;
        if (mythicMobsAvailable) {
            try {
                this.mythicHelper = new io.lumine.mythic.bukkit.BukkitAPIHelper();
            } catch (NoClassDefFoundError e) {
                this.mythicHelper = null;
            }
        }
    }

    public boolean isExcluded(Entity entity) {
        if (citizensAvailable && CitizensAPI.getNPCRegistry() != null
                && CitizensAPI.getNPCRegistry().isNPC(entity)) {
            return true;
        }

        if (mythicMobsAvailable && mythicHelper != null && mythicHelper.isMythicMob(entity)) {
            String mobType = mythicHelper.getMythicMobInstance(entity).getMobType();
            if (denylistedMythicMobs.contains("*") || denylistedMythicMobs.contains(mobType)) {
                return true;
            }
        }

        return false;
    }

    public String getExclusionReason(Entity entity) {
        if (citizensAvailable && CitizensAPI.getNPCRegistry() != null
                && CitizensAPI.getNPCRegistry().isNPC(entity)) {
            return "NPC";
        }

        if (mythicMobsAvailable && mythicHelper != null && mythicHelper.isMythicMob(entity)) {
            String mobType = mythicHelper.getMythicMobInstance(entity).getMobType();
            if (denylistedMythicMobs.contains("*") || denylistedMythicMobs.contains(mobType)) {
                return "MythicMob:" + mobType;
            }
        }

        return null;
    }
}
