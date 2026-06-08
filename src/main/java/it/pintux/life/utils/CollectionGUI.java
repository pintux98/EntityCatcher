package it.pintux.life.utils;

import it.pintux.life.EntityCatcher;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CollectionGUI implements Listener {

    private final EntityCatcher plugin;

    public CollectionGUI(EntityCatcher plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player viewer, OfflinePlayer target) {
        String targetName = target.getName() != null ? target.getName() : "Unknown";
        UUID uuid = target.getUniqueId();
        List<CooldownHandler.CaptureRecord> history = plugin.getCooldownHandler().getCaptureHistory(uuid, 45);
        int size = Math.max(9, (int) (Math.ceil((history.size() + 9) / 9.0) * 9));
        if (size > 54) size = 54;

        CollectionHolder holder = new CollectionHolder();
        Inventory inv = Bukkit.createInventory(holder, size, MessageData.getValue(MessageData.COLLECTION_TITLE, null, viewer));
        holder.setInventory(inv);

        for (int i = 0; i < Math.min(history.size(), size - 9); i++) {
            inv.setItem(i, createEntryItem(history.get(i), viewer));
        }

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", Collections.emptyList());
        for (int i = size - 9; i < size; i++) {
            inv.setItem(i, filler);
        }

        long unique = history.stream().map(r -> r.entityType).distinct().count();
        Map<String, Object> infoRepl = Map.of(
                "{player}", targetName,
                "{captures}", plugin.getCooldownHandler().getCaptureCount(uuid),
                "{places}", plugin.getCooldownHandler().getPlaceCount(uuid),
                "{unique}", unique);
        String infoName = MessageData.getValueNoPrefix(MessageData.COLLECTION_INFO_NAME, infoRepl, viewer);
        List<String> infoLore = MessageData.getList(MessageData.COLLECTION_INFO_LORE, infoRepl, viewer);
        inv.setItem(size - 5, createItem(Material.BOOK, infoName, infoLore));

        viewer.openInventory(inv);
    }

    private ItemStack createEntryItem(CooldownHandler.CaptureRecord record, Player viewer) {
        // Entity names are not Material names; use the matching spawn egg as the icon.
        Material mat = Material.matchMaterial(record.entityType + "_SPAWN_EGG");
        if (mat == null) {
            mat = Material.SPAWNER;
        }

        String name = (record.entityName != null && !record.entityName.isEmpty()) ? record.entityName : "-";
        Map<String, Object> repl = Map.of(
                "{type}", record.entityType,
                "{name}", name,
                "{catcher}", record.catcherType,
                "{world}", record.world != null ? record.world : "",
                "{time}", record.getFormattedTime());

        String displayName = MessageData.getValueNoPrefix(MessageData.COLLECTION_ENTRY_NAME, repl, viewer);
        List<String> lore = MessageData.getList(MessageData.COLLECTION_ENTRY_LORE, repl, viewer);
        return createItem(mat, displayName, lore);
    }

    private ItemStack createItem(Material material, String displayName, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageData.applyColor(displayName));
        if (!loreLines.isEmpty()) {
            meta.setLore(loreLines);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Match by holder, not by title text, so a renamed/localized title still locks the GUI.
        if (event.getInventory().getHolder() instanceof CollectionHolder) {
            event.setCancelled(true);
        }
    }

    /** Marker holder used to identify the read-only collection inventory. */
    private static final class CollectionHolder implements InventoryHolder {
        private Inventory inventory;

        void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
