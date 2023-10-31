package de.silke.referralpaper.timecounter;

import de.silke.referralpaper.Main;
import lombok.Data;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class PlayerTimeManager {
    private static final Map<UUID, PlayerTime> playerTimes = new HashMap<>();
    private Map<UUID, Long> playerStartTimes = new HashMap<>();

    /**
     * Записать время вход игрока
     *
     * @param playerUUID UUID игрока
     */
    public void playerLogin(UUID playerUUID) {
        if (!playerTimes.containsKey(playerUUID)) {
            Main.log.info("Игрок " + Bukkit.getOfflinePlayer(playerUUID).getName() + " не найден в playerTimes, добавляем его туда");
            playerTimes.put(playerUUID, new PlayerTime(playerUUID));
        }

        PlayerTime playerTime = playerTimes.get(playerUUID);
        Main.log.info("Устанавливаем время входа игрока " + Main.plugin.getPlayerInfoDatabase().getPlayerName(playerUUID).join() + " в " + System.currentTimeMillis() + " мс");
        playerTime.setLoginTime(System.currentTimeMillis());

        if (!playerStartTimes.containsKey(playerUUID)) {
            Main.log.info("Игрок " + Main.plugin.getPlayerInfoDatabase().getPlayerName(playerUUID).join() + " не найден в playerStartTimes, добавляем его туда");
            playerStartTimes.put(playerUUID, System.currentTimeMillis());
        }

        if (Main.plugin.getPlayerInfoDatabase().containsPlayer(playerUUID).join()) {
            // Добавляем время игры в базу данных
            Main.log.info("Добавляем время игры игрока " + Main.plugin.getPlayerInfoDatabase().getPlayerName(playerUUID).join() + " в БД");
            Main.plugin.getPlayerInfoDatabase().addTimePlayed(playerUUID, playerTime.getTotalPlaytime());
        }
    }

    /**
     * Записать время игры игрока
     *
     * @param playerUUID UUID игрока
     */
    public void playerLogout(UUID playerUUID) {
        if (playerTimes.containsKey(playerUUID)) {
            PlayerTime playerTime = playerTimes.get(playerUUID);
            long logoutTime = System.currentTimeMillis();
            long loginTime = playerTime.getLoginTime();
            long playtime = logoutTime - loginTime;
            playerTime.setTotalPlaytime(playerTime.getTotalPlaytime() + playtime);

            if (Main.plugin.getPlayerInfoDatabase().containsPlayer(playerUUID).join()) {
                // Добавляем время игры в базу данных
                Main.plugin.getPlayerInfoDatabase().addTimePlayed(playerUUID, playerTime.getTotalPlaytime());
            }
        }
    }

    /**
     * Обновить время определённого игрока
     *
     * @param playerUUID UUID игрока
     */
    public void updatePlayerTime(UUID playerUUID) {
        if (playerTimes.containsKey(playerUUID)) {
            PlayerTime playerTime = playerTimes.get(playerUUID);
            Main.log.info("Игрок " + Main.plugin.getPlayerInfoDatabase().getPlayerName(playerUUID).join() + " есть в playerTimes и он " + (playerTime.isLoggedIn() ? "залогинен" : "не залогинен"));

            if (playerTime.isLoggedIn()) {
                long logoutTime = System.currentTimeMillis();
                long loginTime = playerTime.getLoginTime();
                long playtime = logoutTime - loginTime;
                playerTime.setTotalPlaytime(playerTime.getTotalPlaytime() + playtime);
                playerTime.setLoggedIn(true);

                if (Main.plugin.getPlayerInfoDatabase().containsPlayer(playerUUID).join()) {
                    Main.log.info("Обновление времени игры игрока " + playerUUID + " в БД");
                    Main.plugin.getPlayerInfoDatabase().addTimePlayed(playerUUID, playerTime.getTotalPlaytime());
                }
            }
        } else {
            Main.log.info("Игрок " + Main.plugin.getPlayerInfoDatabase().getPlayerName(playerUUID).join() + " не найден в playerTimes");
        }
    }

    public void onGlobalTimeSave() {
        long currentTime = System.currentTimeMillis();

        for (UUID playerUUID : playerStartTimes.keySet()) {
            long startTime = playerStartTimes.get(playerUUID);
            long elapsedTime = currentTime - startTime;

            // Обновляем время игры игрока
            updatePlayerPlaytime(playerUUID, elapsedTime);
        }

        // Очищаем таймеры для следующего интервала
        playerStartTimes.clear();
    }

    private void updatePlayerPlaytime(UUID playerUUID, long elapsedTime) {
        if (Main.plugin.getPlayerInfoDatabase().containsPlayer(playerUUID).join()) {
            // Добавляем время игры в базу данных
            Main.plugin.getPlayerInfoDatabase().addTimePlayed(playerUUID, elapsedTime);
        }
    }

    /**
     * Получить время всех игроков
     *
     * @return Время всех игроков
     */
    public Collection<PlayerTime> getAllPlayerTimes() {
        return playerTimes.values();
    }
}
