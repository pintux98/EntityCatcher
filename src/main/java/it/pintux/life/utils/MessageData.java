package it.pintux.life.utils;

import it.pintux.life.EntityCatcher;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageData {

    private static String PREFIX = "prefix";
    public static String NO_PEX = "noPex";
    public static String COMMAND_PLAYER_NOT_FOUND = "command.player_not_found";
    public static String COMMAND_CATCHER_NOT_FOUND = "command.catcher_not_found";
    public static String COMMAND_SUCCESS = "command.success";
    public static String COMMAND_RELOAD = "command.reload";
    public static String COMMAND_USAGE_MAIN = "command.usage_main";
    public static String COMMAND_USAGE_GIVE = "command.usage_give";
    public static String COMMAND_USAGE_GIVEALL = "command.usage_giveall";
    public static String COMMAND_USAGE_STATS = "command.usage_stats";
    public static String COMMAND_INVALID_AMOUNT = "command.invalid_amount";
    public static String COMMAND_PLAYERS_ONLY = "command.players_only";
    public static String COOLDOWN = "cooldown";
    public static String CAPTURE_PROTECTION = "capture.protection";
    public static String CAPTURE_FULL_CATCHER = "capture.full";
    public static String CAPTURE_TYPE_WRONG = "capture.type_wrong";
    public static String CAPTURE_CATCHED = "capture.catched";

    public static String CAPTURE_EXCLUDED = "capture.excluded";

    public static String CAPTURE_WORLD_DISABLED = "capture.world_disabled";

    public static String CAPTURE_FAILED_CHANCE = "capture.failed_chance";

    public static String COLLECTION_TITLE = "collection.title";
    public static String COLLECTION_INFO_NAME = "collection.info_name";
    public static String COLLECTION_INFO_LORE = "collection.info_lore";
    public static String COLLECTION_ENTRY_NAME = "collection.entry_name";
    public static String COLLECTION_ENTRY_LORE = "collection.entry_lore";

    public static String PLACE_PROTECTION = "place.protection";
    public static String PLACE_PLACED = "place.placed";

    public static String STATS_HEADER = "stats.header";
    public static String STATS_CAPTURES = "stats.captures";
    public static String STATS_PLACES = "stats.places";

    private static final Pattern hexPattern = Pattern.compile("<#([A-Fa-f0-9]){6}>");

    private static FileConfiguration config;
    private static EntityCatcher plugin;

    public MessageData(EntityCatcher plugin, String filename) {
        this.plugin = plugin;
        File file = new File(plugin.getDataFolder(), filename);
        config = YamlConfiguration.loadConfiguration(file);
    }

    public static String getValueNoPrefix(String key, Map<String, Object> replacements, Player player) {
        String value = getValueFrom(key);
        value = replaceVariables(value, replacements, player);
        return value;
    }

    public static String getValue(String key) {
        return getValue(key, null, null);
    }

    /**
     * Returns a configurable list of lines (e.g. GUI lore), color- and
     * placeholder-resolved. Empty list if the key is missing.
     */
    public static List<String> getList(String key, Map<String, Object> replacements, Player player) {
        if (config == null) {
            return Collections.emptyList();
        }
        List<String> raw = config.getStringList(key);
        List<String> out = new ArrayList<>(raw.size());
        for (String line : raw) {
            out.add(applyColor(replaceVariables(line, replacements, player)));
        }
        return out;
    }

    public static String getValue(String key, Map<String, Object> replacements, Player player) {
        String prefix = getValueFrom(PREFIX);
        String value = getValueNoPrefix(key, replacements, player);
        return prefix.concat(" ").concat(value);
    }

    private static String getValueFrom(String key) {
        return applyColor(getValueFromConfig(key));
    }

    private static String getValueFromConfig(String path) {
        String currentElement = "";
        if (config == null) {
            return currentElement;
        }
        currentElement = config.getString(path);
        return currentElement == null || currentElement.isBlank() ? "&aValue not found in &6messages.yml &afor &6".concat(path).concat(" &a - please add it manually with the necessary variables to fix this error") : currentElement;
    }

    public static String replaceVariables(String value, Map<String, Object> replacements, Player player) {
        if (replacements != null) {
            for (Map.Entry<String, Object> entry : replacements.entrySet()) {
                value = value.replace(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        if (plugin != null && plugin.isPlaceholderAPI()) {
            value = PlaceholderAPI.setPlaceholders(player, value);
        }
        return value;
    }

    public static String applyColor(String message) {
        Matcher matcher = hexPattern.matcher(message);
        while (matcher.find()) {
            final ChatColor hexColor = ChatColor.of(matcher.group().substring(1, matcher.group().length() - 1));
            final String before = message.substring(0, matcher.start());
            final String after = message.substring(matcher.end());
            message = before + hexColor + after;
            matcher = hexPattern.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
