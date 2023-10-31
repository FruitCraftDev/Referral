package de.silke.referralpurpur.commands;

import de.silke.referralpurpur.guis.AdminInvitesGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AdminInvitesCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (args.length == 0) {
                if (player.hasPermission("referralpaper.admin") || player.hasPermission("referralpaper.*") || player.isOp()) {
                    new AdminInvitesGUI(player);
                }

            }
        }
        return true;
    }
}
