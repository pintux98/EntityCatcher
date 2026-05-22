package it.pintux.life.protection;

import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.land.Area;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class LandsProtection implements CatcherProtection {

    private final LandsIntegration landsIntegration;

    public LandsProtection(org.bukkit.plugin.Plugin plugin) {
        this.landsIntegration = LandsIntegration.of(plugin);
    }

    @Override
    public boolean isProtected(Player player, Location location) {
        Area area = landsIntegration.getArea(location);
        if (area == null) {
            return true;
        }
        return area.isTrusted(player.getUniqueId());
    }
}
