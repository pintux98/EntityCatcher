package it.pintux.life.catcher;

import it.pintux.life.EntityCatcher;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class EntityListener implements Listener {

    private final EntityCatcher plugin;

    public EntityListener(EntityCatcher plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        ItemStack bucket = player.getInventory().getItemInMainHand();
        CatcherManager catcherManager = plugin.getCatcherManager();
        CatcherType catcherType = catcherManager.getBucketTypeFromItem(bucket);
        if (catcherType == null) {
            return;
        }
        event.setCancelled(true);
        catcherManager.handleCapture(player, entity, bucket, catcherType);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack bucket = player.getInventory().getItemInMainHand();
        CatcherManager catcherManager = plugin.getCatcherManager();
        CatcherType catcherType = catcherManager.getBucketTypeFromItem(bucket);
        if (catcherType == null) {
            return;
        }
        catcherManager.handlePlace(player, bucket, catcherType);
    }
}
