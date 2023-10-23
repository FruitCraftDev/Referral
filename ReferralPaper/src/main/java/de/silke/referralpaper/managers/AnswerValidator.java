package de.silke.referralpaper.managers;

import de.silke.referralpaper.Main;
import de.silke.referralpaper.database.PlayerInfoDatabaseConnection;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.UUID;

public class AnswerValidator {
    public static final List<String> negativeAnswers = Main.plugin.getConfig().getStringList("answers.negative");
    private static final PlayerInfoDatabaseConnection playerInfoDatabase = Main.plugin.getPlayerInfoDatabaseConnection();

    /**
     * Проверить, является ли ответ игрока положительным
     * <p>Произойдёт проверка на наличие игрока в базе данных
     *
     * @param answer Ответ игрока (ник)
     * @return true - если игрок есть в базе данных, иначе false
     */
    public static boolean isRealPlayer(String answer) {
        UUID answerPlayer = Bukkit.getOfflinePlayer(answer).getUniqueId();

        return playerInfoDatabase.containsPlayer(answerPlayer).join() != null;
    }

    /**
     * Проверить, является ли ответ игрока отрицательным
     * <p>Большой список ответов на отрицание находится в конфиге
     *
     * @param answer Ответ игрока
     * @return true - если ответ игрока равен тире, иначе false
     */
    public static boolean isNoAnswer(String answer) {
        return negativeAnswers.contains(answer);
    }
}
