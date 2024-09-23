package it.pintux.life.protection;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class GriefPreventionProtection implements CatcherProtection {
    @Override
    public boolean isProtected(Player player, Location location) {
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, true, null);
        if (claim == null) {
            return true;
        }
        return claim.allowBuild(player, location.getBlock().getType()) == null;
    }
}
