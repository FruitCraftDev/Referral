package de.silke.referralpaper.commands;

import de.silke.referralpaper.Main;
import de.silke.referralpaper.database.PlayerInfoDatabaseConnection;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ReferralCommand implements CommandExecutor {
    private final PlayerInfoDatabaseConnection playerInfoDatabase = Main.plugin.getPlayerInfoDatabaseConnection();
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length != 0) {
                switch (args[0].toLowerCase()) {
                    case "reload":
                        if (player.isOp()) {
                            Main.plugin.reloadConfig();
                            player.sendMessage(ChatColor.GREEN + "Конфигурация перезагружена");
                            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                        }
                        break;
                    case "debug":
                        playerInfoDatabase.addPlayer(player);
                        break;
                    default:
                        help(player);
                }
            }
        }

        return true;
    }

    public void help(Player player) {
        if (player.isOp()) {
            player.sendMessage(ChatColor.YELLOW + "Доступные команды:");
            player.sendMessage(ChatColor.YELLOW + "/referral help" + ChatColor.RESET + " - Показать список команд");
            player.sendMessage(ChatColor.YELLOW + "/referral reload" + ChatColor.RESET + " - Перезагрузить плагин");
            // TODO: Добавить остальные команды
        }
    }
}
