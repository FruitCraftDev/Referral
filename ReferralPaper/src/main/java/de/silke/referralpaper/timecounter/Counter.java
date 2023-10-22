package de.silke.referralpaper.timecounter;

import de.silke.referralpaper.Main;
import org.bukkit.entity.Player;

public class Counter {
    private long startTime;
    private long endTime;

    /**
     * Запустить счетчик
     */
    public void start() {
        startTime = System.currentTimeMillis();
    }

    /**
     * Остановить счетчик
     */
    public void stop() {
        endTime = System.currentTimeMillis();
    }

    /**
     * Получить время работы счетчика
     *
     * @return Время работы счетчика
     */
    public long getTime() {
        return endTime - startTime;
    }

    /**
     * Сохранить время работы счетчика в БД
     * <p>Если время работы счетчика меньше 0, то оно не будет сохранено
     *
     * @param player Игрок
     */
    public void saveTime(Player player) {
        if (getTime() > 0) {
            Main.plugin.getPlayerInfoDatabaseConnection().addTimePlayed(player, getTime());
        }
    }
}