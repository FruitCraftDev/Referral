package de.silke.referralpaper.timecounter;

import de.silke.referralpaper.Main;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class PlayerTimeManager {
    private HashMap<UUID, PlayerTime> playerTimes = new HashMap<>();
    private Map<UUID, Long> playerStartTimes = new HashMap<>();

    /**
     * Записать время вход игрока
     * @param playerUUID UUID игрока
     */
    public void playerLogin(UUID playerUUID) {
        if (!playerTimes.containsKey(playerUUID)) {
            playerTimes.put(playerUUID, new PlayerTime(playerUUID));
        }

        PlayerTime playerTime = playerTimes.get(playerUUID);
        playerTime.setLoginTime(System.currentTimeMillis());

        if (!playerStartTimes.containsKey(playerUUID)) {
            playerStartTimes.put(playerUUID, System.currentTimeMillis());
        }

        if (Main.plugin.getPlayerInfoDatabaseConnection().containsPlayer(playerUUID).join()) {
            // Добавляем время игры в базу данных
            Main.plugin.getPlayerInfoDatabaseConnection().addTimePlayed(playerTime.getPlayer(playerUUID), playerTime.getTotalPlaytime());
        }

        // Debug
        Main.log.info("Registering login time for " + playerTime.getPlayer(playerUUID).getName());
        Main.log.info("Players in playerTimes: " + playerTimes.keySet());
    }

    /**
     * Записать время игры игрока
     * @param playerUUID UUID игрока
     */
    public void playerLogout(UUID playerUUID) {
        if (playerTimes.containsKey(playerUUID)) {
            PlayerTime playerTime = playerTimes.get(playerUUID);
            long logoutTime = System.currentTimeMillis();
            long loginTime = playerTime.getLoginTime();
            long playtime = logoutTime - loginTime;
            playerTime.setTotalPlaytime(playerTime.getTotalPlaytime() + playtime);

            if (Main.plugin.getPlayerInfoDatabaseConnection().containsPlayer(playerUUID).join()) {
                // Добавляем время игры в базу данных
                Main.plugin.getPlayerInfoDatabaseConnection().addTimePlayed(playerTime.getPlayer(playerUUID), playerTime.getTotalPlaytime());
            }

            // Debug
            Main.log.info("Registering logout time for " + playerTimes.get(playerUUID).getPlayer(playerUUID).getName());
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
        // Сохраняем накопленное время игры в базу данных
        Player player = Bukkit.getOfflinePlayer(playerUUID).getPlayer();
        if (Main.plugin.getPlayerInfoDatabaseConnection().containsPlayer(playerUUID).join()) {
            // Добавляем время игры в базу данных
            Main.plugin.getPlayerInfoDatabaseConnection().addTimePlayed(player, elapsedTime);
        }
    }

    /**
     * Добавить игрока в список времени игры
     * @param player Игрок
     */
    private void addPLayer(Player player) {
        UUID playerUUID = player.getUniqueId();
        playerTimes.put(playerUUID, new PlayerTime(playerUUID));
    }

    /**
     * Получить время игры игрока
     * @param player Игрок
     * @return Время игры игрока
     */
    public PlayerTime getPlayerTime(Player player) {
        UUID playerUUID = player.getUniqueId();
        return playerTimes.get(playerUUID);
    }

    /**
     * Получить время игры игрока по его UUID
     * @param playerUUID UUID игрока
     * @return Время игры игрока
     */
    public PlayerTime getPlayerTime(UUID playerUUID) {
        return playerTimes.get(playerUUID);
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
