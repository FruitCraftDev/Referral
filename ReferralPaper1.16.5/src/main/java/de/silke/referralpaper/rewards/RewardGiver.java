package de.silke.referralpaper.rewards;

import de.silke.referralpaper.utils.Colorizer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class RewardGiver {
    public static RewardsParser parser = new RewardsParser();
    public static final List<String> rewardCommands = parser.getRewardCommands();

    public static void giveReward(Player player) {
        if (player != null) {
            for (String command : rewardCommands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Colorizer.color(command.replace("%player%", player.getName())));
            }
        }
    }
}
