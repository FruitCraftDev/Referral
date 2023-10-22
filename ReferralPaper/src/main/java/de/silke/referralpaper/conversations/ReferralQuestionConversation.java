package de.silke.referralpaper.conversations;

import de.silke.referralpaper.Main;
import de.silke.referralpaper.listeners.PlayerFirstJoinListener;
import de.silke.referralpaper.managers.AnswerValidator;
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
public class ReferralQuestionConversation {
    private final ConversationFactory conversationFactory;
    private final String agreeMessage = Main.plugin.getConfig().getString("messages.referral-agree");
    private final String agreeMessageNoPlayer = Main.plugin.getConfig().getString("messages.referral-player-not-exists");
    private final String disagreeMessage = Main.plugin.getConfig().getString("messages.referral-disagree");
    public ReferralQuestionConversation(Player player) {

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
            return ChatColor.GRAY + "Вы также можете ответить негативно, это будет считать за отказ";
        }

        @Override
        public @Nullable Prompt acceptInput(@NotNull ConversationContext context, @Nullable String input) {
            Player player = (Player) context.getForWhom();
            if (input != null) {
                // Если игрок ответил отрицательно
                if (AnswerValidator.isNoAnswer(input)) {
                    if (disagreeMessage != null) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', disagreeMessage));
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);

                        PlayerFirstJoinListener.setFirstTimePlayerJoin(false);
                        PlayerFirstJoinListener.setNeedToAskQuestion(true);

                        // Добавляем реферала в БД
                        if (!Main.plugin.getReferralsDatabaseConnection().containsPlayer(player).join()) {
                            Main.plugin.getReferralsDatabaseConnection().addPlayer(player);
                        }

                        // Устанавливаем, что игрок отказался от реферала
                        if (Main.plugin.getPlayerInfoDatabaseConnection().containsPlayer(player).join()) {
                            Main.plugin.getPlayerInfoDatabaseConnection().setDeclinedReferQuestion(player, true);
                        }
                        return END_OF_CONVERSATION;
                    }
                } else {
                    // Если игрок ответил положительно и ввёл ник существующего игрока
                    if (AnswerValidator.isRealPlayer(input)) {
                        if (agreeMessage != null) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', agreeMessage));
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);

                            PlayerFirstJoinListener.setNeedToAskQuestion(true);
                            PlayerFirstJoinListener.setFirstTimePlayerJoin(false);

                            // Добавляем реферала в БД
                            if (!Main.plugin.getReferralsDatabaseConnection().containsPlayer(player).join()) {
                                Main.plugin.getReferralsDatabaseConnection().addPlayer(player);
                            }

                            // Устанавливаем, что игрок не отказался от реферала
                            if (Main.plugin.getPlayerInfoDatabaseConnection().containsPlayer(player).join()) {
                                Main.plugin.getPlayerInfoDatabaseConnection().setDeclinedReferQuestion(player, false);
                            }
                            return END_OF_CONVERSATION;
                        }
                    } else {
                        // Если игрок ответил положительно, но ввёл ник несуществующего игрока
                        if (agreeMessageNoPlayer != null) {
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&', agreeMessageNoPlayer));
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);

                            return new ReferralQuestionPrompt();
                        }
                    }
                }
            } else {
                player.sendMessage(ChatColor.RED + "Вы не ввели ник игрока");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);

                return new ReferralQuestionPrompt();
            }
            return END_OF_CONVERSATION;
        }
    }
}
