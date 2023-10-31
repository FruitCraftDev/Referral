package de.silke.referralpurpur.utils;

import de.silke.referralpurpur.Main;

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
            UUID playerUUID = Main.plugin.getPlayerInfoDatabase().getPlayerUUID(answer).join();
            if (playerUUID != null) {
                return Main.plugin.getPlayerInfoDatabase().containsPlayer(playerUUID).join();
            }
        } catch (NullPointerException exception) {
            return false;
        }
        return false;
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
            // Чем меньше расстояние, тем больше совпадение
            if (distance <= 2) {
                return true;
            }

            if (answer.equalsIgnoreCase(negativeAnswer)) {
                return true;
            }
        }
        return false;
    }
}
