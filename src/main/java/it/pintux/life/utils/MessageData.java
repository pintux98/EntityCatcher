package it.pintux.life.utils;

import it.pintux.life.EntityCatcher;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageData {

    private static String PREFIX = "prefix";
    public static String NO_PEX = "noPex";
    public static String MENU_NOPEX = "menu.noPex";
    public static String MENU_NOJAVA = "menu.noJava";
    public static String MENU_ARGS = "menu.arguments";

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

    private static String replaceVariables(String value, Map<String, Object> replacements, Player player) {
        if (replacements != null) {
            for (Map.Entry<String, Object> entry : replacements.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                value = value.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }
        if (plugin.isPlaceholderAPI()) {
            value = PlaceholderAPI.setPlaceholders(player, value);
        }
        Matcher matcher = Pattern.compile("\\{(\\w+)}").matcher(value);
        while (matcher.find()) {
            String placeholder = matcher.group(1);
            System.out.println("Missing replacement value for placeholder: " + placeholder);
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
