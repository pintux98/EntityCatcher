package it.pintux.life.catcher;

import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTEntity;
import de.tr7zw.changeme.nbtapi.NBTItem;
import io.papermc.paper.entity.Bucketable;
import it.pintux.life.utils.CooldownHandler;
import it.pintux.life.EntityCatcher;
import it.pintux.life.utils.MessageData;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.logging.Level;

public class CatcherManager {
    private static final long ANTI_EXPLOIT_THRESHOLD_MS = 2000;

    private final EntityCatcher plugin;
    private final Map<String, CatcherType> catcherTypes;
    private final CooldownHandler cooldownHandler;

    public CatcherManager(EntityCatcher plugin) {
        this.plugin = plugin;
        this.cooldownHandler = plugin.getCooldownHandler();
        this.catcherTypes = new HashMap<>();
        loadBucketTypes();
    }

    private void loadBucketTypes() {
        ConfigurationSection bucketsSection = plugin.getConfig().getConfigurationSection("catchers");
        if (bucketsSection == null) return;
        for (String bucketKey : bucketsSection.getKeys(false)) {
            ConfigurationSection bucketConfig = bucketsSection.getConfigurationSection(bucketKey);
            if (bucketConfig == null) {
                plugin.getLogger().warning("Skipping malformed catcher '" + bucketKey + "' (not a section).");
                continue;
            }
            String allowedTypes = bucketConfig.getString("capture.allowed_types", "ANYTHING");
            boolean captureCustomName = bucketConfig.getBoolean("capture_data.capture_custom_name", true);
            boolean captureHealth = bucketConfig.getBoolean("capture_data.capture_health", true);
            boolean captureVariant = bucketConfig.getBoolean("capture_data.capture_variant", false);
            boolean captureArmor = bucketConfig.getBoolean("capture_data.capture_armor", false);
            boolean captureEquipment = bucketConfig.getBoolean("capture_data.capture_equipment", false);
            boolean removeAI = bucketConfig.getBoolean("place_behavior.remove_ai", false);
            boolean setInvisible = bucketConfig.getBoolean("place_behavior.set_invisible", false);
            boolean setGlowing = bucketConfig.getBoolean("place_behavior.set_glowing", false);
            boolean setOnFire = bucketConfig.getBoolean("place_behavior.set_on_fire", false);
            boolean setInvincible = bucketConfig.getBoolean("place_behavior.set_invincible", false);
            List<String> shape = bucketConfig.getStringList("recipe.shape");
            ConfigurationSection ingredients = bucketConfig.getConfigurationSection("recipe.ingredients");
            String capturePermission = bucketConfig.getString("capture.permissions.capture", "");
            String placePermission = bucketConfig.getString("capture.permissions.place", "");
            double captureChance = bucketConfig.getDouble("capture.capture_chance", 1.0);
            String displayName = bucketConfig.getString("display_name", "");
            String emptyMaterial = bucketConfig.getString("description.empty.material", "BUCKET");
            String fullMaterial = bucketConfig.getString("description.captured.material", "BUCKET");
            List<String> loreEmpty = bucketConfig.getStringList("description.empty.lore");
            List<String> loreCaptured = bucketConfig.getStringList("description.captured.lore");
            CatcherType catcherType = new CatcherType(bucketKey, displayName, emptyMaterial, fullMaterial, loreEmpty, loreCaptured, allowedTypes,
                    captureCustomName, captureHealth, captureVariant, captureArmor, captureEquipment, removeAI,
                    setInvisible, setGlowing, setOnFire, setInvincible, shape, ingredients, capturePermission, placePermission, captureChance);
            catcherTypes.put(bucketKey, catcherType);
            registerRecipe(catcherType);
        }
    }

    private void registerRecipe(CatcherType catcherType) {
        List<String> shape = catcherType.getShape();
        if (shape == null || shape.size() != 3) {
            plugin.getLogger().warning("Catcher '" + catcherType.getName() + "' has no valid 3-row recipe shape; skipping recipe.");
            return;
        }
        ConfigurationSection ingredients = catcherType.getIngredients();
        if (ingredients == null) {
            plugin.getLogger().warning("Catcher '" + catcherType.getName() + "' has no recipe ingredients; skipping recipe.");
            return;
        }

        ItemStack item = catcherType.createEmptyCatcherItem();
        NamespacedKey key = new NamespacedKey(plugin, catcherType.getName().toLowerCase() + "_bucket");
        ShapedRecipe recipe = new ShapedRecipe(key, item);
        recipe.shape(shape.get(0), shape.get(1), shape.get(2));

        Set<Character> defined = new HashSet<>();
        for (String keyChar : ingredients.getKeys(false)) {
            Material material = Material.getMaterial(ingredients.getString(keyChar));
            if (material != null) {
                recipe.setIngredient(keyChar.charAt(0), material);
                defined.add(keyChar.charAt(0));
            }
        }

        // Bukkit rejects the recipe if any non-space char in the shape lacks an ingredient.
        for (String row : shape) {
            for (char c : row.toCharArray()) {
                if (c != ' ' && !defined.contains(c)) {
                    plugin.getLogger().warning("Catcher '" + catcherType.getName() + "' recipe references undefined ingredient '" + c + "'; skipping recipe.");
                    return;
                }
            }
        }

        if (plugin.getServer().getRecipe(key) != null) {
            plugin.getServer().removeRecipe(key);
        }
        try {
            plugin.getServer().addRecipe(recipe);
        } catch (IllegalStateException e) {
            plugin.getLogger().warning("Failed to register recipe for catcher '" + catcherType.getName() + "': " + e.getMessage());
        }
    }

    public CatcherType getBucketTypeFromItem(ItemStack item) {
        // Catcher items can be any material (configured per type), so identify them by
        // their NBT tag, not by Material. Cheap pre-checks first to avoid building an
        // NBTItem for every plain block/tool right-click.
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;

        NBTItem nbtItem = new NBTItem(item);
        if (!nbtItem.hasKey("catcherType")) return null;

        return catcherTypes.get(nbtItem.getString("catcherType"));
    }

    public ItemStack getBucketItem(String bucketKey) {
        CatcherType catcherType = catcherTypes.get(bucketKey);
        return catcherType != null ? catcherType.createEmptyCatcherItem() : null;
    }

    public void handleCapture(Player player, Entity entity, ItemStack bucket, CatcherType catcherType) {
        UUID playerUUID = player.getUniqueId();

        if (isWorldDisabled(player.getWorld().getName())) {
            player.sendMessage(MessageData.getValue(MessageData.CAPTURE_WORLD_DISABLED));
            return;
        }

        boolean bypassExclusion = isBypassCatcher(catcherType.getName());
        if (!bypassExclusion && plugin.getEntityExclusionManager().isExcluded(entity)) {
            player.sendMessage(MessageData.getValue(MessageData.CAPTURE_EXCLUDED));
            return;
        }

        String capturePerm = catcherType.getCapturePermission();
        if (!capturePerm.isEmpty() && !player.hasPermission(capturePerm)) {
            player.sendMessage(MessageData.getValue(MessageData.NO_PEX));
            return;
        }

        if (isOnCooldown(player, playerUUID, "capture")) return;

        if (!plugin.getProtectionManager().isProtected(player, entity.getLocation())) {
            player.sendMessage(MessageData.getValue(MessageData.CAPTURE_PROTECTION));
            return;
        }

        NBTItem nbtItem = new NBTItem(bucket);

        if (nbtItem.getBoolean("hasCapture")) {
            player.sendMessage(MessageData.getValue(MessageData.CAPTURE_FULL_CATCHER));
            return;
        }

        if (!isAllowedEntityType(entity, catcherType.getAllowedTypes())) {
            player.sendMessage(MessageData.getValue(MessageData.CAPTURE_TYPE_WRONG, Map.of("{type}", catcherType.getAllowedTypes()), player));
            return;
        }

        double chance = catcherType.getCaptureChance();
        if (chance < 1.0 && Math.random() > chance) {
            player.sendMessage(MessageData.getValue(MessageData.CAPTURE_FAILED_CHANCE, Map.of("{chance}", (int) (chance * 100)), player));
            return;
        }

        String entityTypeName = entity.getType().toString();
        String entityCustomName = entity.getCustomName();

        captureEntity(player, entity, bucket, catcherType);

        // Cooldown is applied only after a successful capture, so failed attempts
        // (wrong type, full catcher, missed chance) do not burn the cooldown.
        applyCooldown(player, playerUUID, "capture");
        cooldownHandler.incrementCaptureCount(playerUUID);
        cooldownHandler.recordActionTime(playerUUID, "capture");
        cooldownHandler.recordCapture(playerUUID, entityTypeName, entityCustomName, catcherType.getName(), player.getWorld().getName());

        player.sendMessage(MessageData.getValue(MessageData.CAPTURE_CATCHED, Map.of("{entity_type}", entityTypeName), player));
    }

    public void handlePlace(Player player, ItemStack bucket, CatcherType catcherType) {
        UUID playerUUID = player.getUniqueId();

        NBTItem nbtItem = new NBTItem(bucket);

        // An empty catcher does nothing on right-click; check this first so it never
        // sets a cooldown or runs protection lookups.
        if (!nbtItem.getBoolean("hasCapture")) {
            return;
        }

        if (cooldownHandler.isActionTooRecent(playerUUID, "capture", ANTI_EXPLOIT_THRESHOLD_MS)) {
            player.sendMessage(MessageData.getValue(MessageData.COOLDOWN, Map.of("{time}", 2, "{action}", "place"), player));
            return;
        }

        String placePerm = catcherType.getPlacePermission();
        if (!placePerm.isEmpty() && !player.hasPermission(placePerm)) {
            player.sendMessage(MessageData.getValue(MessageData.NO_PEX));
            return;
        }

        if (isOnCooldown(player, playerUUID, "place")) return;

        if (!plugin.getProtectionManager().isProtected(player, player.getLocation())) {
            player.sendMessage(MessageData.getValue(MessageData.PLACE_PROTECTION));
            return;
        }

        String entityTypeName = nbtItem.getString("capturedEntityType");

        placeEntity(player, bucket, catcherType);

        applyCooldown(player, playerUUID, "place");
        cooldownHandler.incrementPlaceCount(playerUUID);
        cooldownHandler.recordActionTime(playerUUID, "place");

        player.sendMessage(MessageData.getValue(MessageData.PLACE_PLACED, Map.of("{entity_type}", entityTypeName), player));
    }

    public void captureEntity(Player player, Entity entity, ItemStack bucket, CatcherType catcherType) {
        ItemStack item = catcherType.createFullCatcherItem();

        boolean isBaby = entity instanceof Ageable && !((Ageable) entity).isAdult();
        String variant = readVariantName(entity);

        // Resolve the dynamic display name/lore and apply the meta BEFORE writing any
        // NBT-API keys. Bukkit's setItemMeta rebuilds the item tag from the meta, which
        // would otherwise wipe custom NBT keys (catcherType, hasCapture, snapshot, ...).
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageData.applyColor(catcherType.getDisplayName()));
        if (!catcherType.getCaptureLore().isEmpty()) {
            List<String> lore = new ArrayList<>();
            catcherType.getCaptureLore().forEach(s -> lore.add(MessageData.applyColor(s).replace("{name}", entity.getName())
                    .replace("{type}", entity.getType().toString()).replace("{age}", isBaby ? "Baby" : "Adult")
                    .replace("{variant}", variant != null ? variant : "")));
            meta.setLore(lore);
        }
        item.setItemMeta(meta);

        // All NBT-API writes happen last.
        NBTItem nbtItem = new NBTItem(item);
        nbtItem.setString("catcherType", catcherType.getName());
        nbtItem.setString("capturedEntityType", entity.getType().toString());

        // Store a full NBT snapshot of the entity so every attribute (tamed owner,
        // profession, anger, effects, equipment, variant, ...) survives the round-trip.
        try {
            NBTContainer snapshot = new NBTContainer(new NBTEntity(entity).toString());
            stripVolatileKeys(snapshot);
            // capture_data flags act as opt-out filters on the snapshot.
            if (!catcherType.shouldCaptureCustomName()) {
                snapshot.removeKey("CustomName");
                snapshot.removeKey("CustomNameVisible");
            }
            if (!catcherType.shouldCaptureHealth()) {
                snapshot.removeKey("Health");
            }
            if (!catcherType.shouldCaptureArmor()) {
                snapshot.removeKey("ArmorItems");
                snapshot.removeKey("ArmorDropChances");
            }
            if (!catcherType.shouldCaptureEquipment()) {
                snapshot.removeKey("HandItems");
                snapshot.removeKey("HandDropChances");
            }
            if (!catcherType.shouldCaptureVariant()) {
                snapshot.removeKey("Color");
                snapshot.removeKey("variant");
                snapshot.removeKey("Variant");
            }
            nbtItem.setString("entitySnapshot", snapshot.toString());
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Failed to snapshot entity " + entity.getType(), t);
        }

        // Lightweight fields kept purely for lore placeholders.
        nbtItem.setBoolean("isBaby", isBaby);
        if (variant != null) {
            nbtItem.setString("variant", variant);
        }
        nbtItem.setBoolean("hasCapture", true);

        ItemStack finalItem = nbtItem.getItem();
        entity.remove();
        consumeAndGive(player, bucket, finalItem);
    }

    /**
     * Removes positional/identity keys that must not be carried onto a freshly spawned
     * entity, otherwise it would teleport, clash UUIDs, or fail to spawn.
     */
    private void stripVolatileKeys(NBTContainer nbt) {
        for (String key : new String[]{"UUID", "UUIDMost", "UUIDLeast", "Pos", "Motion", "Rotation",
                "Leash", "Leashed", "FallDistance", "Air", "PortalCooldown", "Dimension",
                "WorldUUIDMost", "WorldUUIDLeast", "Passengers"}) {
            nbt.removeKey(key);
        }
    }

    private String readVariantName(Entity entity) {
        if (entity instanceof Cat) return ((Cat) entity).getCatType().name();
        if (entity instanceof Llama) return ((Llama) entity).getColor().name();
        if (entity instanceof Horse) return ((Horse) entity).getColor().name();
        if (entity instanceof Sheep) return ((Sheep) entity).getColor().name();
        return null;
    }

    /**
     * Consumes exactly one catcher from the held stack and hands back the result item,
     * so stacked catchers are never lost or duplicated.
     */
    private void consumeAndGive(Player player, ItemStack bucket, ItemStack result) {
        if (bucket.getAmount() > 1) {
            bucket.setAmount(bucket.getAmount() - 1);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(result);
            leftover.values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        } else {
            player.getInventory().setItemInMainHand(result);
        }
    }

    private boolean isAllowedEntityType(Entity entity, String allowedTypes) {
        // Never allow players, armor stands, or non-living entities (item frames,
        // dropped items, etc.) to be captured.
        if (!(entity instanceof LivingEntity) || entity instanceof Player || entity instanceof ArmorStand) {
            return false;
        }
        if ("ANIMAL".equalsIgnoreCase(allowedTypes)) {
            return entity instanceof Animals || entity instanceof Bucketable;
        } else if ("MOB".equalsIgnoreCase(allowedTypes)) {
            return !(entity instanceof Animals);
        } else return "ANYTHING".equalsIgnoreCase(allowedTypes);
    }

    private boolean isWorldDisabled(String worldName) {
        List<String> disabledWorlds = plugin.getConfig().getStringList("exclusions.disabled_worlds");
        return disabledWorlds.contains(worldName);
    }

    private boolean isBypassCatcher(String catcherName) {
        List<String> bypassCatchers = plugin.getConfig().getStringList("exclusions.bypass_catchers");
        return bypassCatchers.contains(catcherName);
    }

    public void placeEntity(Player player, ItemStack bucket, CatcherType catcherType) {
        NBTItem nbtItem = new NBTItem(bucket);

        EntityType entityType;
        try {
            entityType = EntityType.valueOf(nbtItem.getString("capturedEntityType"));
        } catch (IllegalArgumentException e) {
            player.sendMessage(MessageData.getValue(MessageData.COMMAND_CATCHER_NOT_FOUND));
            return;
        }

        Entity spawnedEntity = player.getWorld().spawnEntity(player.getLocation(), entityType);

        // Restore the full snapshot. Health/attributes come back together, so there is
        // no separate clamp needed. Wrapped so a malformed snapshot can never abort the
        // placement (and leave the entity spawned with the bucket un-consumed).
        if (nbtItem.hasKey("entitySnapshot")) {
            try {
                NBTContainer snapshot = new NBTContainer(nbtItem.getString("entitySnapshot"));
                stripVolatileKeys(snapshot);
                new NBTEntity(spawnedEntity).mergeCompound(snapshot);
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "Failed to restore entity snapshot for " + entityType, t);
            }
        }

        if (spawnedEntity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) spawnedEntity;
            if (catcherType.shouldRemoveAI()) {
                livingEntity.setAI(false);
            }
            if (catcherType.shouldSetInvisible()) {
                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false));
            }
            if (catcherType.shouldSetOnFire()) {
                livingEntity.setFireTicks(100);
            }
            if (catcherType.shouldSetGlowing()) {
                livingEntity.setGlowing(true);
            }
            if (catcherType.shouldBeInvincible()) {
                livingEntity.setInvulnerable(true);
            }
        }

        consumeAndGive(player, bucket, catcherType.createEmptyCatcherItem());
    }

    public long getCooldownFromPermissions(Player player, String permissionBase, String action) {
        int maxCooldown = -1;

        for (PermissionAttachmentInfo permInfo : player.getEffectivePermissions()) {
            String permission = permInfo.getPermission();
            if (permission.startsWith(permissionBase + "." + action + ".")) {
                try {
                    int cooldownTime = Integer.parseInt(permission.substring((permissionBase + "." + action + ".").length()));
                    if (cooldownTime > maxCooldown) {
                        maxCooldown = cooldownTime;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return maxCooldown > 0 ? maxCooldown * 60 * 1000L : 0;
    }

    /**
     * Returns true (and messages the player) if the action is still on cooldown.
     * Does not mutate any state.
     */
    public boolean isOnCooldown(Player player, UUID playerUUID, String action) {
        if (player.hasPermission("entitycatcher.bypass.cooldown")) {
            return false;
        }
        long remainingCooldown = cooldownHandler.getCooldown(playerUUID, action);
        if (remainingCooldown > 0) {
            player.sendMessage(MessageData.getValue(MessageData.COOLDOWN, Map.of("{time}", (remainingCooldown / 1000), "{action}", action), player));
            return true;
        }
        return false;
    }

    /**
     * Starts the cooldown for the given action. Called only after a successful
     * capture/place so failed attempts never trigger a cooldown.
     */
    public void applyCooldown(Player player, UUID playerUUID, String action) {
        if (player.hasPermission("entitycatcher.bypass.cooldown")) {
            return;
        }
        long cooldownDuration = getCooldownFromPermissions(player, "entitycatcher", action);
        if (cooldownDuration > 0) {
            cooldownHandler.setCooldown(playerUUID, action, cooldownDuration);
        }
    }

    public Map<String, CatcherType> getBucketTypes() {
        return catcherTypes;
    }
}
