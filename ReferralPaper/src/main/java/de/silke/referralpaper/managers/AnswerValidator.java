package de.silke.referralpaper.managers;

import de.silke.referralpaper.Main;
import de.silke.referralpaper.utils.Levenshtein;

import java.util.List;
import java.util.UUID;

public class AnswerValidator {
    public static final List<String> negativeAnswers = Main.plugin.getConfig().getStringList("answers.negative");

    /**
     * Проверить, является ли ответ игрока положительным
     * <p>Произойдёт проверка на наличие игрока в базе данных
     *
     * @param answer Ответ игрока (ник)
     * @return true - если игрок есть в базе данных, иначе false
     */
    public static boolean isRealPlayer(String answer) {
        try {
            UUID playerUUID = Main.plugin.getPlayerInfoDatabaseConnection().getPlayerUUID(answer).join();
            return Main.plugin.getPlayerInfoDatabaseConnection().containsPlayer(playerUUID).join();
        } catch (NullPointerException exception) {
            return false;
        }
    }

    /**
     * Проверить, является ли ответ игрока отрицательным
     * <p>Большой список ответов на отрицание находится в конфиге
     *
     * @param answer Ответ игрока
     * @return true - если ответ игрока равен тире, иначе false
     */
    public static boolean isNoAnswer(String answer) {
        for (String negativeAnswer : negativeAnswers) {
            int distance = Levenshtein.calculateDistance(answer, negativeAnswer);

            // Расстояние 2
            if (distance <= 2) {
                return true;
            }
        }
        return false;
    }
}
