package it.pintux.life.catcher;

import de.tr7zw.changeme.nbtapi.NBTItem;
import it.pintux.life.utils.MessageData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class CatcherType {
    private final String name;
    private final String displayName;
    private final String emptyMaterial;
    private final String capturedMaterial;
    private final List<String> emptyLore;
    private final List<String> captureLore;
    private final String allowedTypes;
    private final boolean captureCustomName;
    private final boolean captureHealth;
    private final boolean captureVariant;
    private final boolean captureArmor;
    private final boolean captureEquipment;
    private final boolean removeAI;
    private final boolean setInvisible;
    private final boolean setGlowing;
    private final boolean setOnFire;
    private final boolean setInvincible;
    private final List<String> shape;
    private final ConfigurationSection ingredients;
    private final String capturePermission;
    private final String placePermission;

    public CatcherType(String name, String displayName, String emptyMaterial, String capturedMaterial, List<String> emptyLore, List<String> captureLore, String allowedTypes, boolean captureCustomName, boolean captureHealth,
                       boolean captureVariant, boolean captureArmor, boolean captureEquipment, boolean removeAI,
                       boolean setInvisible, boolean setGlowing, boolean setOnFire, boolean setInvincible, List<String> shape,
                       ConfigurationSection ingredients, String capturePermission, String placePermission) {
        this.name = name;
        this.displayName = displayName;
        this.emptyMaterial = emptyMaterial;
        this.capturedMaterial = capturedMaterial;
        this.emptyLore = emptyLore;
        this.captureLore = captureLore;
        this.allowedTypes = allowedTypes;
        this.captureCustomName = captureCustomName;
        this.captureHealth = captureHealth;
        this.captureVariant = captureVariant;
        this.captureArmor = captureArmor;
        this.captureEquipment = captureEquipment;
        this.removeAI = removeAI;
        this.setInvisible = setInvisible;
        this.setGlowing = setGlowing;
        this.setOnFire = setOnFire;
        this.setInvincible = setInvincible;
        this.shape = shape;
        this.ingredients = ingredients;
        this.capturePermission = capturePermission;
        this.placePermission = placePermission;
    }

    public ItemStack createEmptyCatcherItem() {
        ItemStack bucket = new ItemStack(getEmptyMaterial());
        ItemMeta meta = bucket.getItemMeta();
        meta.setDisplayName(MessageData.applyColor(displayName));
        if (!emptyLore.isEmpty()) {
            List<String> lore = new ArrayList<>();
            emptyLore.forEach(s -> lore.add(MessageData.replaceVariables(s, null, null)));
            meta.setLore(lore);
        }
        bucket.setItemMeta(meta);
        NBTItem nbtItem = new NBTItem(bucket);
        nbtItem.setString("catcherType", name);
        return nbtItem.getItem();
    }

    public ItemStack createFullCatcherItem() {
        ItemStack bucket = new ItemStack(getEmptyMaterial());
        ItemMeta meta = bucket.getItemMeta();
        meta.setDisplayName(MessageData.applyColor(displayName));
        if (!captureLore.isEmpty()) {
            List<String> lore = new ArrayList<>();
            captureLore.forEach(s -> lore.add(MessageData.applyColor(MessageData.replaceVariables(s, null, null))));
            meta.setLore(lore);
        }
        bucket.setItemMeta(meta);
        NBTItem nbtItem = new NBTItem(bucket);
        nbtItem.setString("catcherType", name);
        return nbtItem.getItem();
    }

    public boolean shouldCaptureCustomName() {
        return captureCustomName;
    }

    public boolean shouldCaptureHealth() {
        return captureHealth;
    }

    public boolean shouldCaptureVariant() {
        return captureVariant;
    }

    public boolean shouldCaptureArmor() {
        return captureArmor;
    }

    public boolean shouldCaptureEquipment() {
        return captureEquipment;
    }

    public boolean shouldRemoveAI() {
        return removeAI;
    }

    public boolean shouldSetInvisible() {
        return setInvisible;
    }

    public boolean shouldSetGlowing() {
        return setGlowing;
    }

    public boolean shouldSetOnFire() {
        return setOnFire;
    }

    public boolean shouldBeInvincible() {
        return setInvincible;
    }

    public List<String> getShape() {
        return shape;
    }

    public ConfigurationSection getIngredients() {
        return ingredients;
    }

    public String getCapturePermission() {
        return capturePermission;
    }

    public String getPlacePermission() {
        return placePermission;
    }

    public String getAllowedTypes() {
        return allowedTypes;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getEmptyLore() {
        return emptyLore;
    }

    public List<String> getCaptureLore() {
        return captureLore;
    }

    public Material getEmptyMaterial() {
        Material mat = Material.getMaterial(emptyMaterial);
        return mat == null ? Material.BUCKET : mat;
    }

    public Material getCapturedMaterial() {
        Material mat = Material.getMaterial(capturedMaterial);
        return mat == null ? Material.BUCKET : mat;
    }
}