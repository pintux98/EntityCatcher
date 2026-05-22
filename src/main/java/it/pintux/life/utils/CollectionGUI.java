package it.pintux.life.utils;

import it.pintux.life.EntityCatcher;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CollectionGUI implements Listener {

    private final EntityCatcher plugin;

    public CollectionGUI(EntityCatcher plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player viewer, Player target) {
        List<CooldownHandler.CaptureRecord> history = plugin.getCooldownHandler().getCaptureHistory(target.getUniqueId(), 45);
        int size = Math.max(9, (int) (Math.ceil((history.size() + 9) / 9.0) * 9));
        if (size > 54) size = 54;

        Inventory inv = Bukkit.createInventory(null, size, MessageData.getValue(MessageData.COLLECTION_TITLE, null, viewer));

        for (int i = 0; i < Math.min(history.size(), size - 9); i++) {
            CooldownHandler.CaptureRecord record = history.get(i);
            ItemStack icon = createEntryItem(record);
            inv.setItem(i, icon);
        }

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = size - 9; i < size; i++) {
            inv.setItem(i, filler);
        }

        ItemStack infoItem = createItem(Material.BOOK,
                MessageData.applyColor("&6&l" + target.getName() + "'s Collection"),
                "&7Total captures: &f" + plugin.getCooldownHandler().getCaptureCount(target.getUniqueId()),
                "&7Total places: &f" + plugin.getCooldownHandler().getPlaceCount(target.getUniqueId()),
                "&7Unique types: &f" + history.stream().map(r -> r.entityType).distinct().count(),
                "",
                "&7This is read-only. You cannot take items.");
        inv.setItem(size - 5, infoItem);

        viewer.openInventory(inv);
    }

    private ItemStack createEntryItem(CooldownHandler.CaptureRecord record) {
        Material mat = Material.SPAWNER;
        try {
            mat = Material.valueOf(record.entityType);
        } catch (IllegalArgumentException ignored) {
        }

        List<String> lore = new ArrayList<>();
        lore.add(MessageData.applyColor("&7Type: &f" + record.entityType));
        if (record.entityName != null && !record.entityName.isEmpty()) {
            lore.add(MessageData.applyColor("&7Name: &f" + record.entityName));
        }
        lore.add(MessageData.applyColor("&7Catcher: &f" + record.catcherType));
        lore.add(MessageData.applyColor("&7World: &f" + record.world));
        lore.add(MessageData.applyColor("&7Captured: &f" + record.getFormattedTime()));

        return createItem(mat, MessageData.applyColor("&e" + record.entityType), lore.toArray(new String[0]));
    }

    private ItemStack createItem(Material material, String displayName, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageData.applyColor(displayName));
        if (loreLines.length > 0) {
            List<String> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(MessageData.applyColor(line));
            }
            meta.setLore(lore);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.contains("Collection")) {
            return;
        }
        event.setCancelled(true);
    }
}
