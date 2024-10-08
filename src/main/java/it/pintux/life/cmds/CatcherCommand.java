package it.pintux.life.cmds;

import it.pintux.life.EntityCatcher;
import it.pintux.life.utils.MessageData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CatcherCommand implements CommandExecutor, TabCompleter {

    private final EntityCatcher plugin;

    public CatcherCommand(EntityCatcher plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("entitycatcher")) {

            if (args.length == 0) {
                sender.sendMessage("Usage: /entitycatcher <give | giveall | reload>");
                return true;
            }

            if (args[0].equalsIgnoreCase("give")) {
                if (!sender.hasPermission("entitycatcher.give")) {
                    sender.sendMessage(MessageData.getValue(MessageData.NO_PEX));
                    return true;
                }

                if (args.length < 3) {
                    sender.sendMessage("Usage: /entitycatcher give <player> <type> <amount>");
                    return true;
                }

                Player targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer == null) {
                    sender.sendMessage(MessageData.getValue(MessageData.COMMAND_PLAYER_NOT_FOUND));
                    return true;
                }

                String type = args[2];

                ItemStack bucket = plugin.getCatcherManager().getBucketItem(type);
                if (bucket == null) {
                    sender.sendMessage(MessageData.getValue(MessageData.COMMAND_CATCHER_NOT_FOUND));
                    return true;
                }
                int amount = args.length == 4 ? Integer.parseInt(args[3]) : 1;
                if (amount <= 0) {
                    amount = 1;
                }

                bucket.setAmount(amount);

                targetPlayer.getInventory().addItem(bucket);
                targetPlayer.sendMessage(MessageData.getValue(MessageData.COMMAND_SUCCESS, Map.of("{catcher_type}", type), targetPlayer));
            } else if (args[0].equalsIgnoreCase("giveall")) {
                if (!sender.hasPermission("entitycatcher.give")) {
                    sender.sendMessage(MessageData.getValue(MessageData.NO_PEX));
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage("Usage: /entitycatcher giveall <type> <amount>");
                    return true;
                }

                String type = args[1];

                ItemStack bucket = plugin.getCatcherManager().getBucketItem(type);
                if (bucket == null) {
                    sender.sendMessage(MessageData.getValue(MessageData.COMMAND_CATCHER_NOT_FOUND));
                    return true;
                }
                int amount = args.length == 3 ? Integer.parseInt(args[2]) : 1;
                if (amount <= 0) {
                    amount = 1;
                }

                bucket.setAmount(amount);

                for (Player targetPlayer : Bukkit.getOnlinePlayers()) {
                    targetPlayer.getInventory().addItem(bucket);
                    targetPlayer.sendMessage(MessageData.getValue(MessageData.COMMAND_SUCCESS, Map.of("{catcher_type}", type), targetPlayer));
                }
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("entitycatcher.reload")) {
                    sender.sendMessage(MessageData.getValue(MessageData.NO_PEX));
                    return true;
                }
                plugin.reloadData();
                sender.sendMessage(MessageData.getValue(MessageData.COMMAND_RELOAD));
            }
            return true;
        }

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("give", "giveall", "reload");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                if (sender.hasPermission("entitycatcher.give")) {
                    return Stream.of(Bukkit.getOnlinePlayers().stream().map(Player::getName).toArray(String[]::new)).collect(Collectors.toList());
                }
            } else if (args[0].equalsIgnoreCase("giveall")) {
                if (sender.hasPermission("entitycatcher.give")) {
                    return Stream.of(plugin.getCatcherManager().getBucketTypes().keySet().toArray(new String[0])).collect(Collectors.toList());
                }
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                if (sender.hasPermission("entitycatcher.give")) {
                    return Stream.of(plugin.getCatcherManager().getBucketTypes().keySet().toArray(new String[0])).collect(Collectors.toList());
                }
            }
        }
        return List.of();
    }
}
