package it.pintux.life.protection;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface CatcherProtection {
    boolean isProtected(Player player, Location location);
}
