package it.pintux.life.protection;

import net.william278.huskclaims.api.HuskClaimsAPI;
import net.william278.huskclaims.libraries.cloplib.operation.OperationType;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.user.OnlineUser;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class HuskClaimsProtection implements CatcherProtection {
    @Override
    public boolean isProtected(Player player, Location location) {
        if (location.getWorld() == null) {
            return true;
        }
        HuskClaimsAPI api = HuskClaimsAPI.getInstance();
        OnlineUser user = api.getOnlineUser(player.getUniqueId());
        // Check at the actual capture/place location, not the player's current position.
        Position position = api.getPosition(location.getX(), location.getY(), location.getZ(),
                location.getWorld().getName());
        return api.isOperationAllowed(user, OperationType.ENTITY_INTERACT, position) ||
               api.isOperationAllowed(user, OperationType.BLOCK_PLACE, position);
    }
}
