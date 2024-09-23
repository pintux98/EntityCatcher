package it.pintux.life.utils;

import it.pintux.life.protection.CatcherProtection;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ProtectionManager {
    private final List<CatcherProtection> handlers;

    public ProtectionManager() {
        handlers = new ArrayList<>();
    }

    public void addHandler(CatcherProtection handler) {
        handlers.add(handler);
    }

    public boolean isProtected(Player player, Location location) {
        for (CatcherProtection handler : handlers) {
            return handler.isProtected(player, location);
        }
        return true;
    }
}
