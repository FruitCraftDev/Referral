package de.silke.referralpaper.utils;

import de.silke.referralpaper.Main;
import org.bukkit.entity.Player;

public class DateConverter {
    /**
     * Получить отформатированное время игры игрока
     * <p>Время игры возвращается в формате "д дней ч часов м минут с секунд"
     * @param player Игрок
     * @return Отформатированное время игры игрока
     */
    public static String getFormattedTimePlayed(Player player) {
        long timePlayed = Main.plugin.getPlayerInfoDatabaseConnection().getTimePlayed(player).join();
        long seconds = timePlayed / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        seconds %= 60;
        minutes %= 60;
        hours %= 24;

        StringBuilder stringBuilder = new StringBuilder();

        if (days > 0) {
            stringBuilder.append(days).append("д ");
        }
        if (hours > 0) {
            stringBuilder.append(hours).append("ч ");
        }
        if (minutes > 0) {
            stringBuilder.append(minutes).append("м ");
        }
        if (seconds > 0) {
            stringBuilder.append(seconds).append("с");
        }

        return stringBuilder.toString();
    }
}
