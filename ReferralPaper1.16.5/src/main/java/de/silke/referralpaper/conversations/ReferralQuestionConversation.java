package de.silke.referralpaper.conversations;

import de.silke.referralpaper.Main;
import de.silke.referralpaper.listeners.PlayerFirstJoinListener;
import de.silke.referralpaper.luckperms.LuckPermsConnector;
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

import java.sql.Date;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class ReferralQuestionConversation {
    private final ConversationFactory conversationFactory;
    private final String agreeMessage = Main.plugin.getConfig().getString("messages.referral-agree");
    private final String agreeMessageNoPlayer = Main.plugin.getConfig().getString("messages.referral-player-not-exists");
    private final String disagreeMessage = Main.plugin.getConfig().getString("messages.referral-disagree");

    public boolean isNegativeAnswer = false;
    public boolean isPositiveAnswerRealPlayer = false;
    public boolean isPositiveAnswerNoPlayer = false;

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
            if (isNegativeAnswer) {
                return Colorizer.color(disagreeMessage);
            } else if (isPositiveAnswerRealPlayer) {
                return Colorizer.color(agreeMessage);
            } else if (isPositiveAnswerNoPlayer) {
                return Colorizer.color(agreeMessageNoPlayer);
            }
            return ChatColor.GRAY + "Вы также можете ответить негативно, это будет считаться за отказ";
        }

        @Override
        public @Nullable Prompt acceptInput(@NotNull ConversationContext context, @Nullable String input) {
            Player player = (Player) context.getForWhom();
            UUID playerUUID = player.getUniqueId();
            if (input != null) {
                // Если игрок ответил отрицательно
                if (AnswerValidator.isNoAnswer(input)) {
                    if (disagreeMessage != null) {
                        isNegativeAnswer = true;
                        PlayerFirstJoinListener.newbies.remove(player.getUniqueId());

                        player.sendMessage(Colorizer.color(disagreeMessage));
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);

                        // Добавляем игрока в БД
                        addToDatabases(player, playerUUID);

                        // Устанавливаем, что игрок отказался от реферала
                        if (Main.plugin.getPlayerInfoDatabase().containsPlayer(playerUUID).join()) {
                            player.sendMessage(Colorizer.color(disagreeMessage));
                            Main.plugin.getPlayerInfoDatabase().setDeclinedReferQuestion(playerUUID, true);
                        }

                        // Заполняем информацию об игроке в БД
                        fillData(player, playerUUID);

                        return END_OF_CONVERSATION;
                    }
                } else {
                    // Если игрок ответил положительно и ввёл ник существующего игрока
                    if (AnswerValidator.isRealPlayer(input) || Boolean.parseBoolean(String.valueOf(AnswerValidator.isRealPlayer(input)))) {
                        if (agreeMessage != null) {
                            isPositiveAnswerRealPlayer = true;
                            player.sendMessage(Colorizer.color(agreeMessage));
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);

                            PlayerFirstJoinListener.newbies.remove(player.getUniqueId());

                            // Добавляем игрока в БД
                            addToDatabases(player, playerUUID);

                            UUID referredPlayer = Main.plugin.getPlayerInfoDatabase().getPlayerUUID(input).join();

                            // Добавляем +1 к количеству указывания реферального игрока
                            if (Main.plugin.getReferralsDatabase().containsPlayer(referredPlayer).join()) {
                                player.sendMessage(Colorizer.color(agreeMessage));
                                Main.plugin.getReferralsDatabase().addPlayerToUsedReferralNick(referredPlayer, player.getName());
                                Main.plugin.getReferralsDatabase().addAmountOfTimesUsed(referredPlayer);
                            }

                            // Устанавливаем, что игрок не отказался от реферала
                            if (Main.plugin.getPlayerInfoDatabase().containsPlayer(playerUUID).join()) {
                                player.sendMessage(Colorizer.color(agreeMessage));
                                Main.plugin.getPlayerInfoDatabase().setDeclinedReferQuestion(playerUUID, false);
                            }

                            // Устанавливаем реферального игрока
                            if (Main.plugin.getPlayerInfoDatabase().containsPlayer(playerUUID).join()) {
                                player.sendMessage(Colorizer.color(agreeMessage));
                                Main.plugin.getPlayerInfoDatabase().setReferredPlayer(playerUUID, referredPlayer);
                            }

                            // Заполняем информацию об игроке в БД
                            fillData(player, playerUUID);

                            return END_OF_CONVERSATION;
                        }
                    } else {
                        // Если игрок ответил положительно, но ввёл ник несуществующего игрока
                        if (agreeMessageNoPlayer != null) {
                            isPositiveAnswerNoPlayer = true;
                            player.sendMessage(Colorizer.color(agreeMessageNoPlayer));
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);

                            return new ReferralQuestionPrompt();
                        }
                    }
                }
            } else {
                // Это вообще не может произойти, но на всякий случай оставим
                player.sendMessage(ChatColor.RED + "Вы не ввели ник игрока");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);

                return new ReferralQuestionPrompt();
            }
            return END_OF_CONVERSATION;
        }

        public void addToDatabases(Player player, UUID playerUUID) {
            // Добавляем игрока в БД
            if (!Main.plugin.getPlayerInfoDatabase().containsPlayer(playerUUID).join()) {
                player.sendMessage(Colorizer.color(disagreeMessage));
                Main.plugin.getPlayerInfoDatabase().addPlayer(playerUUID);
            }

            // Добавляем реферала в БД
            if (!Main.plugin.getReferralsDatabase().containsPlayer(playerUUID).join()) {
                player.sendMessage(Colorizer.color(disagreeMessage));
                Main.plugin.getReferralsDatabase().addPlayer(player);
            }
        }

        public void fillData(Player player, UUID playerUUID) {
            if (Main.plugin.getPlayerInfoDatabase().containsPlayer(playerUUID).join()) {
                // Проверка на заполненность информации об игроке в БД
                // Также служит для обновления информации об игроке в БД
                if (Main.plugin.getPlayerInfoDatabase().getRegistrationDate(playerUUID).join() == null) {
                    Main.plugin.getPlayerInfoDatabase().setRegistrationDate(playerUUID, Date.valueOf(LocalDate.now())).join();
                }

                if (Main.plugin.getPlayerInfoDatabase().getLuckPermsRole(playerUUID).join() == null || Main.plugin.getPlayerInfoDatabase().getLuckPermsRole(playerUUID).join() != null) {
                    Main.plugin.getPlayerInfoDatabase().setLuckPermsRole(playerUUID, LuckPermsConnector.getGroup(player)).join();
                }

                if (Main.plugin.getPlayerInfoDatabase().getTimePlayed(playerUUID).join() == null || Main.plugin.getPlayerInfoDatabase().getTimePlayed(playerUUID).join() <= 0L) {
                    Main.log.info("Время игры игрока " + player.getName() + " не найдено в БД, устанавливаем 20 секунд");
                    Main.plugin.getPlayerInfoDatabase().setTimePlayed(playerUUID, 20L).join();
                }
            }
        }
    }
}
