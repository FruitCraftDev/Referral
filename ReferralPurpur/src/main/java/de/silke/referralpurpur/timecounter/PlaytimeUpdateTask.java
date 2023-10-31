package de.silke.referralpurpur.timecounter;

import de.silke.referralpurpur.Main;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class PlaytimeUpdateTask extends BukkitRunnable {
    private final PlayerTimeManager playerTimeManager;

    public PlaytimeUpdateTask(PlayerTimeManager playerTimeManager) {
        this.playerTimeManager = playerTimeManager;
    }

    @Override
    public void run() {
        for (PlayerTime playerTime : playerTimeManager.getAllPlayerTimes()) {
            UUID playerUUID = playerTime.getPlayerUUID();

            if (playerTime != null) {
                long currentTime = System.currentTimeMillis();
                long loginTime = playerTime.getLoginTime();
                long playtime = currentTime - loginTime;

                if (playtime > 0) {
                    playerTime.addPlaytime(playtime);
                    playerTime.setLoginTime(currentTime);
                }

                if (Main.plugin.getPlayerInfoDatabase().getConnectionPool().getConnection() != null) {
                    // Добавляем время игры в базу данных
                    if (Main.plugin.getPlayerInfoDatabase().containsPlayer(playerUUID).join()) {
                        // Добавляем время игры в базу данных
                        Main.plugin.getPlayerInfoDatabase().addTimePlayed(playerUUID, playtime);
                    }
                } else {
                    Main.plugin.getPlayerInfoDatabase().reconnect().join();
                }
            }
        }
    }
}
