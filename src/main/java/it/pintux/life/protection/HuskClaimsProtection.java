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
        OnlineUser user = HuskClaimsAPI.getInstance().getOnlineUser(player.getUniqueId());
        Position position = user.getPosition();
        return HuskClaimsAPI.getInstance().isOperationAllowed(user, OperationType.ENTITY_INTERACT, position) ||
               HuskClaimsAPI.getInstance().isOperationAllowed(user, OperationType.BLOCK_PLACE, position);
    }
}
