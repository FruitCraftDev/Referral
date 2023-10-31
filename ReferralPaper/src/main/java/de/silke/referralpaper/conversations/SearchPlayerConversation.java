package de.silke.referralpaper.conversations;

import de.silke.referralpaper.Main;
import de.silke.referralpaper.guis.AdminInvitesGUI;
import de.silke.referralpaper.guis.SearchPlayerGUI;
import de.silke.referralpaper.utils.AnswerValidator;
import de.silke.referralpaper.utils.Colorizer;
import lombok.Data;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
public class SearchPlayerConversation {
    public static String searchChars = "";
    private final ConversationFactory conversationFactory;
    private final String findPlayerMessage = Main.plugin.getConfig().getString("messages.find-player-message");

    public SearchPlayerConversation(Player player) {

        conversationFactory = new ConversationFactory(Main.plugin)
                .withModality(true)
                .withFirstPrompt(new ReferralQuestionPrompt())
                .withEscapeSequence(AnswerValidator.negativeAnswers.toString())
                .withLocalEcho(false)
                .addConversationAbandonedListener(event -> {
                    if (!event.gracefulExit()) {
                        // Такое по сути вообще не может произойти, но на всякий случай оставим
                        player.sendMessage("Вопрос отменён");
                    }
                });
        conversationFactory.buildConversation(player).begin();
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
    }

    private class ReferralQuestionPrompt extends StringPrompt {
        @Override
        public @NotNull String getPromptText(@NotNull ConversationContext context) {
            if (findPlayerMessage != null) {
                return Colorizer.color(findPlayerMessage);
            } else {
                Main.log.severe("Сообщение \"find-player-message\" не найдено в конфиге!");
            }
            return "";
        }

        @Override
        public @Nullable Prompt acceptInput(@NotNull ConversationContext context, @Nullable String input) {
            Player player = (Player) context.getForWhom();
            if (input != null) {
                if (!AnswerValidator.isNoAnswer(input)) {
                    searchChars = input;
                    new SearchPlayerGUI(player, searchChars);
                } else {
                    searchChars = input;
                    player.sendMessage(ChatColor.RED + "Поиск отменён");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                    new AdminInvitesGUI(player);

                }
                return END_OF_CONVERSATION;
            } else {
                // Это вообще не может произойти, но на всякий случай оставим
                player.sendMessage(ChatColor.RED + "Вы не ввели ник игрока");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);

                return new ReferralQuestionPrompt();
            }
        }
    }
}
