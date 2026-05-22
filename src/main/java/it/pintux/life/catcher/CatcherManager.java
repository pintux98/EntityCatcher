package it.pintux.life.catcher;

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
        ItemStack item = catcherType.createEmptyCatcherItem();
        NamespacedKey key = new NamespacedKey(plugin, catcherType.getName().toLowerCase() + "_bucket");
        ShapedRecipe recipe = new ShapedRecipe(key, item);
        recipe.shape(catcherType.getShape().get(0), catcherType.getShape().get(1), catcherType.getShape().get(2));

        for (String keyChar : catcherType.getIngredients().getKeys(false)) {
            Material material = Material.getMaterial(catcherType.getIngredients().getString(keyChar));
            if (material != null) {
                recipe.setIngredient(keyChar.charAt(0), material);
            }
        }
        if (plugin.getServer().getRecipe(key) != null) {
            plugin.getServer().removeRecipe(key);
        }
        plugin.getServer().addRecipe(recipe);
    }

    public CatcherType getBucketTypeFromItem(ItemStack item) {
        if (item == null || item.getType() != Material.BUCKET) return null;

        NBTItem nbtItem = new NBTItem(item);

        if (!nbtItem.hasKey("catcherType")) return null;

        String bucketTypeName = nbtItem.getString("catcherType");

        return catcherTypes.get(bucketTypeName);
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

        if (!canCaptureOrPlace(player, playerUUID, "capture")) return;

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

        captureEntity(player, entity, bucket, catcherType);

        cooldownHandler.incrementCaptureCount(playerUUID);
        cooldownHandler.recordActionTime(playerUUID, "capture");
        cooldownHandler.recordCapture(playerUUID, entity.getType().toString(), entity.getCustomName(), catcherType.getName(), player.getWorld().getName());

        player.sendMessage(MessageData.getValue(MessageData.CAPTURE_CATCHED, Map.of("{entity_type}", entity.getType().toString()), player));
    }

    public void handlePlace(Player player, ItemStack bucket, CatcherType catcherType) {
        UUID playerUUID = player.getUniqueId();

        if (cooldownHandler.isActionTooRecent(playerUUID, "capture", ANTI_EXPLOIT_THRESHOLD_MS)) {
            player.sendMessage(MessageData.getValue(MessageData.COOLDOWN, Map.of("{time}", 2, "{action}", "place"), player));
            return;
        }

        String placePerm = catcherType.getPlacePermission();
        if (!placePerm.isEmpty() && !player.hasPermission(placePerm)) {
            player.sendMessage(MessageData.getValue(MessageData.NO_PEX));
            return;
        }

        if (!canCaptureOrPlace(player, playerUUID, "place")) return;

        if (!plugin.getProtectionManager().isProtected(player, player.getLocation())) {
            player.sendMessage(MessageData.getValue(MessageData.PLACE_PROTECTION));
            return;
        }

        NBTItem nbtItem = new NBTItem(bucket);

        if (!nbtItem.getBoolean("hasCapture")) {
            return;
        }

        placeEntity(player, bucket, catcherType);
        cooldownHandler.incrementPlaceCount(playerUUID);
        cooldownHandler.recordActionTime(playerUUID, "place");

        player.sendMessage(MessageData.getValue(MessageData.PLACE_PLACED, Map.of("{entity_type}", nbtItem.getString("capturedEntityType")), player));
    }

    public void captureEntity(Player player, Entity entity, ItemStack bucket, CatcherType catcherType) {
        NBTItem nbtItem = new NBTItem(catcherType.createFullCatcherItem());

        nbtItem.setString("capturedEntityType", entity.getType().toString());

        if (catcherType.shouldCaptureCustomName() && entity.getCustomName() != null) {
            nbtItem.setString("capturedEntityName", entity.getCustomName());
        }

        if (catcherType.shouldCaptureHealth() && entity instanceof Damageable) {
            nbtItem.setDouble("capturedEntityHealth", ((Damageable) entity).getHealth());
        }

        if (catcherType.shouldCaptureVariant() && entity instanceof Sheep) {
            nbtItem.setString("capturedColor", ((Sheep) entity).getColor().name());
        }

        if (catcherType.shouldCaptureArmor() && entity instanceof LivingEntity) {
            org.bukkit.inventory.EntityEquipment equipment = ((LivingEntity) entity).getEquipment();
            if (equipment != null) {
                ItemStack[] armorContents = equipment.getArmorContents();
                for (int i = 0; i < armorContents.length; i++) {
                    nbtItem.setItemStack("armor_" + i, armorContents[i]);
                }
            }
        }

        if (catcherType.shouldCaptureEquipment() && entity instanceof LivingEntity) {
            org.bukkit.inventory.EntityEquipment equipment = ((LivingEntity) entity).getEquipment();
            if (equipment != null) {
                ItemStack mainHand = equipment.getItemInMainHand();
                ItemStack offHand = equipment.getItemInOffHand();
                nbtItem.setItemStack("mainHand", mainHand);
                nbtItem.setItemStack("offHand", offHand);
            }
        }

        if (entity instanceof Ageable) {
            nbtItem.setBoolean("isBaby", !((Ageable) entity).isAdult());
        }

        if (entity instanceof Cat) {
            nbtItem.setString("variant", ((Cat) entity).getCatType().name());
        } else if (entity instanceof Llama) {
            nbtItem.setString("variant", ((Llama) entity).getColor().name());
        } else if (entity instanceof Horse) {
            nbtItem.setString("variant", ((Horse) entity).getColor().name());
        }

        nbtItem.setBoolean("hasCapture", true);

        ItemStack item = nbtItem.getItem();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(MessageData.applyColor(catcherType.getDisplayName()));
        if (!catcherType.getCaptureLore().isEmpty()) {
            List<String> lore = new ArrayList<>();
            catcherType.getCaptureLore().forEach(s -> lore.add(MessageData.applyColor(s).replace("{name}", entity.getName())
                    .replace("{type}", entity.getType().toString()).replace("{age}", nbtItem.getBoolean("isBaby") ? "Baby" : "Adult")
                    .replace("{variant}", nbtItem.getString("variant"))));
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        entity.remove();
        player.getInventory().remove(bucket);
        player.getInventory().setItemInMainHand(item);
    }

    private boolean isAllowedEntityType(Entity entity, String allowedTypes) {
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

        if (spawnedEntity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) spawnedEntity;

            if (catcherType.shouldRemoveAI()) {
                livingEntity.setAI(false);
            }

            if (catcherType.shouldSetInvisible()) {
                livingEntity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1));
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

            if (catcherType.shouldCaptureCustomName() && nbtItem.hasKey("capturedEntityName")) {
                livingEntity.setCustomName(nbtItem.getString("capturedEntityName"));
            }

            if (catcherType.shouldCaptureHealth() && nbtItem.hasKey("capturedEntityHealth")) {
                livingEntity.setHealth(nbtItem.getDouble("capturedEntityHealth"));
            }

            if (catcherType.shouldCaptureVariant() && spawnedEntity instanceof Sheep && nbtItem.hasKey("capturedColor")) {
                ((Sheep) livingEntity).setColor(DyeColor.valueOf(nbtItem.getString("capturedColor")));
            }

            if (catcherType.shouldCaptureArmor()) {
                org.bukkit.inventory.EntityEquipment equipment = livingEntity.getEquipment();
                if (equipment != null) {
                    ItemStack[] armorContents = new ItemStack[4];
                    for (int i = 0; i < 4; i++) {
                        armorContents[i] = nbtItem.getItemStack("armor_" + i);
                    }
                    equipment.setArmorContents(armorContents);
                }
            }

            if (catcherType.shouldCaptureEquipment()) {
                org.bukkit.inventory.EntityEquipment equipment = livingEntity.getEquipment();
                if (equipment != null) {
                    equipment.setItemInMainHand(nbtItem.getItemStack("mainHand"));
                    equipment.setItemInOffHand(nbtItem.getItemStack("offHand"));
                }
            }

            if (spawnedEntity instanceof Ageable && nbtItem.hasKey("isBaby")) {
                boolean isBaby = nbtItem.getBoolean("isBaby");
                if (isBaby) {
                    ((Ageable) livingEntity).setBaby();
                } else {
                    ((Ageable) livingEntity).setAdult();
                }
            }

            if (spawnedEntity instanceof Cat && nbtItem.hasKey("variant")) {
                ((Cat) livingEntity).setCatType(Cat.Type.valueOf(nbtItem.getString("variant")));
            } else if (spawnedEntity instanceof Llama && nbtItem.hasKey("variant")) {
                ((Llama) livingEntity).setColor(Llama.Color.valueOf(nbtItem.getString("variant")));
            } else if (spawnedEntity instanceof Horse && nbtItem.hasKey("variant")) {
                ((Horse) livingEntity).setColor(Horse.Color.valueOf(nbtItem.getString("variant")));
            }
        }

        player.getInventory().setItemInMainHand(catcherType.createEmptyCatcherItem());
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

    public boolean canCaptureOrPlace(Player player, UUID playerUUID, String action) {
        if (player.hasPermission("entitycatcher.bypass.cooldown")) {
            return true;
        }
        long cooldownDuration = getCooldownFromPermissions(player, "entitycatcher", action);
        long remainingCooldown = cooldownHandler.getCooldown(playerUUID, action);

        if (remainingCooldown > 0) {
            player.sendMessage(MessageData.getValue(MessageData.COOLDOWN, Map.of("{time}", (remainingCooldown / 1000), "{action}", action), player));
            return false;
        }
        cooldownHandler.setCooldown(playerUUID, action.equals("capture") ? cooldownDuration : 0, action.equals("place") ? cooldownDuration : 0);
        return true;
    }

    public Map<String, CatcherType> getBucketTypes() {
        return catcherTypes;
    }
}
