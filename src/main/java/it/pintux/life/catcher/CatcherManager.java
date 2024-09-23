package it.pintux.life.catcher;

import de.tr7zw.changeme.nbtapi.NBTItem;
import it.pintux.life.utils.CooldownHandler;
import it.pintux.life.EntityCatcher;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CatcherManager {
    private final EntityCatcher plugin;
    private final Map<String, CatcherType> catcherTypes;
    private final Map<UUID, Long> playerCaptureTimestamps;
    private final CooldownHandler cooldownHandler;

    public CatcherManager(EntityCatcher plugin) {
        this.plugin = plugin;
        this.cooldownHandler = plugin.getCooldownHandler();
        this.catcherTypes = new HashMap<>();
        this.playerCaptureTimestamps = new ConcurrentHashMap<>();
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
            String displayName = bucketConfig.getString("display_name", "");
            List<String> loreEmpty = bucketConfig.getStringList("description.empty");
            List<String> loreCaptured = bucketConfig.getStringList("description.captured");
            CatcherType catcherType = new CatcherType(bucketKey, displayName, loreEmpty, loreCaptured, allowedTypes,
                    captureCustomName, captureHealth, captureVariant, captureArmor, captureEquipment, removeAI,
                    setInvisible, setGlowing, setOnFire, setInvincible, shape, ingredients, capturePermission, placePermission);
            catcherTypes.put(bucketKey, catcherType);
            registerRecipe(catcherType);
        }
    }

    private void registerRecipe(CatcherType catcherType) {
        ItemStack item = catcherType.createCatcherItem();
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

        if (!nbtItem.hasKey("bucketType")) return null;

        String bucketTypeName = nbtItem.getString("bucketType");

        return catcherTypes.get(bucketTypeName);
    }

    public ItemStack getBucketItem(String bucketKey) {
        CatcherType catcherType = catcherTypes.get(bucketKey);
        return catcherType != null ? catcherType.createCatcherItem() : null;
    }

    public void handleCapture(Player player, Entity entity, ItemStack bucket, CatcherType catcherType) {
        String playerName = player.getName();
        UUID playerUUID = player.getUniqueId();

        if (!canCaptureOrPlace(player, playerName, "capture")) return;
        if (!plugin.getProtectionManager().isProtected(player, entity.getLocation())) {
            player.sendMessage("Can't capture in a protection");
            return;
        }

        NBTItem nbtItem = new NBTItem(bucket);

        if (nbtItem.getBoolean("hasCapture")) {
            player.sendMessage("You already have an entity with " + catcherType.getDisplayName());
            return;
        }

        if (!isAllowedEntityType(entity, catcherType.getAllowedTypes())) {
            player.sendMessage("You cannot capture this type of entity with " + catcherType.getDisplayName());
            return;
        }

        captureEntity(player, entity, bucket, catcherType);

        cooldownHandler.incrementCaptureCount(playerName);
        playerCaptureTimestamps.put(playerUUID, System.currentTimeMillis());

        player.sendMessage("You captured the entity with " + catcherType.getDisplayName());
    }

    public void handlePlace(Player player, ItemStack bucket, CatcherType catcherType) {
        String playerName = player.getName();
        UUID playerUUID = player.getUniqueId();

        Long lastCaptureTime = playerCaptureTimestamps.get(playerUUID);
        if (lastCaptureTime != null && (System.currentTimeMillis() - lastCaptureTime) < 1000) {
            return;
        }

        if (!canCaptureOrPlace(player, playerName, "place")) return;

        if (!plugin.getProtectionManager().isProtected(player, player.getLocation())) {
            player.sendMessage("Can't place in a protection");
            return;
        }

        NBTItem nbtItem = new NBTItem(bucket);

        if (!nbtItem.getBoolean("hasCapture")) {
            return;
        }

        placeEntity(player, bucket, catcherType);
        cooldownHandler.incrementPlaceCount(playerName);

        player.sendMessage("Placed entity using " + catcherType.getDisplayName());
    }

    public void captureEntity(Player player, Entity entity, ItemStack bucket, CatcherType catcherType) {
        NBTItem nbtItem = new NBTItem(bucket);

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
            ItemStack[] armorContents = ((LivingEntity) entity).getEquipment().getArmorContents();
            for (int i = 0; i < armorContents.length; i++) {
                nbtItem.setItemStack("armor_" + i, armorContents[i]);
            }
        }

        if (catcherType.shouldCaptureEquipment() && entity instanceof LivingEntity) {
            ItemStack mainHand = ((LivingEntity) entity).getEquipment().getItemInMainHand();
            ItemStack offHand = ((LivingEntity) entity).getEquipment().getItemInOffHand();
            nbtItem.setItemStack("mainHand", mainHand);
            nbtItem.setItemStack("offHand", offHand);
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

        player.getInventory().setItemInMainHand(nbtItem.getItem());
        player.sendMessage("Captured entity with " + catcherType.getDisplayName());
    }


    private boolean isAllowedEntityType(Entity entity, String allowedTypes) {
        if ("ANIMAL".equalsIgnoreCase(allowedTypes)) {
            return entity instanceof Animals;
        } else if ("MOB".equalsIgnoreCase(allowedTypes)) {
            return !(entity instanceof Animals);
        } else return "ANYTHING".equalsIgnoreCase(allowedTypes);
    }

    public void placeEntity(Player player, ItemStack bucket, CatcherType catcherType) {
        NBTItem nbtItem = new NBTItem(bucket);

        Entity spawnedEntity = player.getWorld().spawnEntity(player.getLocation(), EntityType.valueOf(nbtItem.getString("capturedEntityType")));

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
                ItemStack[] armorContents = new ItemStack[4];
                for (int i = 0; i < 4; i++) {
                    armorContents[i] = nbtItem.getItemStack("armor_" + i);
                }
                livingEntity.getEquipment().setArmorContents(armorContents);
            }

            if (catcherType.shouldCaptureEquipment()) {
                livingEntity.getEquipment().setItemInMainHand(nbtItem.getItemStack("mainHand"));
                livingEntity.getEquipment().setItemInOffHand(nbtItem.getItemStack("offHand"));
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
        player.getInventory().setItemInMainHand(catcherType.createCatcherItem());
        player.sendMessage("Placed entity using " + catcherType.getDisplayName());
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

        return maxCooldown > 0 ? maxCooldown * 60 * 1000L : 0;  // Convert to milliseconds
    }

    public boolean canCaptureOrPlace(Player player, String playerName, String action) {
        long cooldownDuration = getCooldownFromPermissions(player, "mobbucket", action);
        long remainingCooldown = cooldownHandler.getCooldown(playerName, action);

        if (remainingCooldown > 0) {
            player.sendMessage("You must wait " + (remainingCooldown / 1000) + " seconds to " + action + " again.");
            return false;
        }
        cooldownHandler.setCooldown(playerName, action.equals("capture") ? cooldownDuration : 0, action.equals("place") ? cooldownDuration : 0);
        return true;
    }

    public Map<String, CatcherType> getBucketTypes() {
        return catcherTypes;
    }
}
