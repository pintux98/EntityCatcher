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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class CatcherCommand implements CommandExecutor, TabCompleter {

    private final EntityCatcher plugin;

    public CatcherCommand(EntityCatcher plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("entitycatcher")) {

            if (args.length == 0) {
                sender.sendMessage("Usage: /entitycatcher <give | giveall | reload | stats | collection>");
                return true;
            }

            if (args[0].equalsIgnoreCase("give")) {
                if (!sender.hasPermission("entitycatcher.give")) {
                    sender.sendMessage(MessageData.getValue(MessageData.NO_PEX));
                    return true;
                }

                if (args.length < 3) {
                    sender.sendMessage("Usage: /entitycatcher give <player> <type> [amount]");
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
                int amount = 1;
                if (args.length >= 4) {
                    try {
                        amount = Integer.parseInt(args[3]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("Invalid amount: " + args[3]);
                        return true;
                    }
                    if (amount <= 0) {
                        amount = 1;
                    }
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
                    sender.sendMessage("Usage: /entitycatcher giveall <type> [amount]");
                    return true;
                }

                String type = args[1];

                ItemStack bucket = plugin.getCatcherManager().getBucketItem(type);
                if (bucket == null) {
                    sender.sendMessage(MessageData.getValue(MessageData.COMMAND_CATCHER_NOT_FOUND));
                    return true;
                }
                int amount = 1;
                if (args.length >= 3) {
                    try {
                        amount = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("Invalid amount: " + args[2]);
                        return true;
                    }
                    if (amount <= 0) {
                        amount = 1;
                    }
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

            } else if (args[0].equalsIgnoreCase("stats")) {
                if (!sender.hasPermission("entitycatcher.stats")) {
                    sender.sendMessage(MessageData.getValue(MessageData.NO_PEX));
                    return true;
                }

                Player targetPlayer;
                if (args.length >= 2) {
                    targetPlayer = Bukkit.getPlayer(args[1]);
                    if (targetPlayer == null) {
                        sender.sendMessage(MessageData.getValue(MessageData.COMMAND_PLAYER_NOT_FOUND));
                        return true;
                    }
                } else if (sender instanceof Player) {
                    targetPlayer = (Player) sender;
                } else {
                    sender.sendMessage("Usage: /entitycatcher stats [player]");
                    return true;
                }

                UUID uuid = targetPlayer.getUniqueId();
                int captures = plugin.getCooldownHandler().getCaptureCount(uuid);
                int places = plugin.getCooldownHandler().getPlaceCount(uuid);
                sender.sendMessage(MessageData.getValueNoPrefix(MessageData.STATS_HEADER, null, targetPlayer));
                sender.sendMessage(MessageData.getValueNoPrefix(MessageData.STATS_CAPTURES, Map.of("{count}", captures), targetPlayer));
                sender.sendMessage(MessageData.getValueNoPrefix(MessageData.STATS_PLACES, Map.of("{count}", places), targetPlayer));

            } else if (args[0].equalsIgnoreCase("collection")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("This command can only be used by players.");
                    return true;
                }
                Player viewer = (Player) sender;
                Player targetPlayer;
                if (args.length >= 2) {
                    if (!viewer.hasPermission("entitycatcher.collection.others")) {
                        viewer.sendMessage(MessageData.getValue(MessageData.NO_PEX));
                        return true;
                    }
                    targetPlayer = Bukkit.getPlayer(args[1]);
                    if (targetPlayer == null) {
                        viewer.sendMessage(MessageData.getValue(MessageData.COMMAND_PLAYER_NOT_FOUND));
                        return true;
                    }
                } else {
                    targetPlayer = viewer;
                }
                plugin.getCollectionGUI().open(viewer, targetPlayer);

            } else {
                sender.sendMessage("Usage: /entitycatcher <give | giveall | reload | stats | collection>");
            }
            return true;
        }

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("give", "giveall", "reload", "stats", "collection");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                if (sender.hasPermission("entitycatcher.give")) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                }
            } else if (args[0].equalsIgnoreCase("giveall")) {
                if (sender.hasPermission("entitycatcher.give")) {
                    return new ArrayList<>(plugin.getCatcherManager().getBucketTypes().keySet());
                }
            } else if (args[0].equalsIgnoreCase("stats")) {
                if (sender.hasPermission("entitycatcher.stats")) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                }
            } else if (args[0].equalsIgnoreCase("collection")) {
                if (sender.hasPermission("entitycatcher.collection.others")) {
                    return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                }
            }
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                if (sender.hasPermission("entitycatcher.give")) {
                    return new ArrayList<>(plugin.getCatcherManager().getBucketTypes().keySet());
                }
            }
        }
        return List.of();
    }
}
