package it.pintux.life.protection;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardProtection implements CatcherProtection {
    @Override
    public boolean isProtected(Player player, Location location) {
        com.sk89q.worldedit.util.Location loc = new com.sk89q.worldedit.util.Location(BukkitAdapter.adapt(location).getExtent(), BukkitAdapter.adapt(location).toVector()); // can also be adapted from Bukkit, as mentioned above
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(loc);

        return set.testState(WorldGuardPlugin.inst().wrapPlayer(player), Flags.BUILD);
    }
}
