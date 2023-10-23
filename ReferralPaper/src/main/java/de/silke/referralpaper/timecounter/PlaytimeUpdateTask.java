package de.silke.referralpaper.timecounter;

import de.silke.referralpaper.Main;
import lombok.Data;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

@Data
public class PlaytimeUpdateTask extends BukkitRunnable {
    private PlayerTimeManager playerTimeManager;

    public PlaytimeUpdateTask(PlayerTimeManager playerTimeManager) {
        this.playerTimeManager = playerTimeManager;
    }

    @Override
    public void run() {
        for (PlayerTime playerTime : playerTimeManager.getAllPlayerTimes()) {
            long playtime = playerTime.getTotalPlaytime();
            UUID playerUUID = playerTime.getPlayerUUID();

            if (Main.plugin.getPlayerInfoDatabaseConnection().containsPlayer(playerUUID).join()) {
                // Добавляем время игры в базу данных
                Main.plugin.getPlayerInfoDatabaseConnection().addTimePlayed(playerTime.getPlayer(playerUUID), playtime);
            }

            // Debug
            Main.log.info("Saving playtime for " + playerTime.getPlayer(playerUUID).getName() + " with time " + playerTime.getTotalPlaytime() + " to database");
        }
    }
}
