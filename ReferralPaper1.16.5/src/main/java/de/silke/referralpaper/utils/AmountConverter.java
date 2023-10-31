package de.silke.referralpaper.utils;

import de.silke.referralpaper.Main;
import lombok.SneakyThrows;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class AmountConverter {
    /**
     * Получить отформатированное время игры игрока
     * <p>Время игры возвращается в формате "д дней ч часов м минут с секунд"
     *
     * @param player Игрок
     * @return Отформатированное время игры игрока
     */
    public static String getFormattedTimePlayed(Player player) {
        UUID playerUUID = player.getUniqueId();
        long timePlayed = Main.plugin.getPlayerInfoDatabase().getTimePlayed(playerUUID).join();
        if (!(timePlayed <= 0)) {
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
        return null;
    }

    /**
     * Получить отформатированное время игры игрока по UUID
     * <p>Время игры возвращается в формате "д дней ч часов м минут с секунд"
     *
     * @param playerUUID Игрок
     * @return Отформатированное время игры игрока
     */
    public static String getFormattedTimePlayed(UUID playerUUID) {
        long timePlayed = Main.plugin.getPlayerInfoDatabase().getTimePlayed(playerUUID).join();
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

    /**
     * Получить отформатированную дату регистрации игрока по UUID
     * <p>Время игры возвращается в формате "д дней ч часов м минут с секунд"
     *
     * @param playerUUID Игрок
     * @return Отформатированное время игры игрока
     */
    @SneakyThrows(ParseException.class)
    public static String getFormattedRegistrationDatePlayed(UUID playerUUID) {
        Date dateOfRegistration = Main.plugin.getPlayerInfoDatabase().getRegistrationDate(playerUUID).join();
        SimpleDateFormat databaseDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        if (dateOfRegistration != null) {
            Date parsedDate = databaseDateFormat.parse(dateOfRegistration.toString());
            long timeElapsed = System.currentTimeMillis() - parsedDate.getTime();
            long days = TimeUnit.MILLISECONDS.toDays(timeElapsed);
            long hours = TimeUnit.MILLISECONDS.toHours(timeElapsed) % 24;

            SimpleDateFormat readableDateFormat = new SimpleDateFormat("dd.MM.yyyy");
            String readableDate = readableDateFormat.format(parsedDate);

            StringBuilder formattedDate = new StringBuilder(readableDate);

            if (days > 0) {
                formattedDate.append(ChatColor.GRAY + " (").append(days).append(" ").append(declensionDays(days));
                if (hours > 0) {
                    formattedDate.append(" ").append(hours).append(" ").append(declensionHours(hours));
                }
                formattedDate.append(" назад)");
            } else {
                formattedDate.append(ChatColor.GRAY + " (сегодня)");
            }

            return formattedDate.toString();
        } else {
            Main.plugin.getPlayerInfoDatabase().setRegistrationDate(playerUUID, java.sql.Date.valueOf(LocalDate.now()));
        }
        return null;
    }

    protected static String declensionDays(long days) {
        String[] declensions = {"день", "дня", "дней"};
        int mod10 = (int) (days % 10);
        int mod100 = (int) (days % 100);

        if (mod10 == 1 && mod100 != 11) {
            return declensions[0];
        } else if ((mod10 >= 2 && mod10 <= 4) && (mod100 < 10 || mod100 >= 20)) {
            return declensions[1];
        } else {
            return declensions[2];
        }
    }

    protected static String declensionHours(long hours) {
        String[] declensions = {"час", "часа", "часов"};
        int mod10 = (int) (hours % 10);
        int mod100 = (int) (hours % 100);

        if (mod10 == 1 && mod100 != 11) {
            return declensions[0];
        } else if ((mod10 >= 2 && mod10 <= 4) && (mod100 < 10 || mod100 >= 20)) {
            return declensions[1];
        } else {
            return declensions[2];
        }
    }

    public static String declensionPlayers(long amount) {
        String[] declensions = {"игрок", "игрока", "игроков"};
        int mod10 = (int) (amount % 10);
        int mod100 = (int) (amount % 100);

        if (mod10 == 1 && mod100 != 11) {
            return amount + " " + declensions[0];
        } else if (mod10 >= 2 && mod10 <= 4 && (mod100 < 10 || mod100 >= 20)) {
            return amount + " " + declensions[1];
        } else {
            return amount + " " + declensions[2];
        }
    }
}
