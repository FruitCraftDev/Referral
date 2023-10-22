package de.silke.referralpaper.listeners;

import de.silke.referralpaper.Main;
import de.silke.referralpaper.conversations.ReferralQuestionConversation;
import de.silke.referralpaper.database.PlayerInfoDatabaseConnection;
import de.silke.referralpaper.luckperms.LuckPermsConnector;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.Date;
import java.time.LocalDate;

@SuppressWarnings("deprection")
public class PlayerFirstJoinListener implements Listener {
    private final PlayerInfoDatabaseConnection playerInfoDatabase = Main.plugin.getPlayerInfoDatabaseConnection();
    @Getter @Setter
    public static boolean isFirstTimePlayerJoin;
    @Getter @Setter
    public static boolean needToAskQuestion;

    @EventHandler
    public void onPlayerFirstJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // TODO: Если игрок заходит в первый раз и его нет в БД, то баг "java.lang.NullPointerException: Cannot invoke "java.lang.Boolean.booleanValue()" because the return value of "java.util.concurrent.CompletableFuture.join()" is null"
        if (
                !playerInfoDatabase.containsPlayer(player).join() || // если в БД нет игрока
                playerInfoDatabase.getTimePlayed(player).join() <= 120000 || // или если игрок играл меньше 2 минут
                playerInfoDatabase.getReferredPlayerName(player) == null || // или игрок ещё не указал реферала
                isFirstTimePlayerJoin || // или игрок впервые зашёл на сервер
                needToAskQuestion) { // или игроку нужно задать вопрос
            isFirstTimePlayerJoin = true;
            sendReferralQuestion(player);
        }

        // Проверка на заполненность информации об игроке в БД
        if (playerInfoDatabase.getRegistrationDate(player).join() == null) {
            playerInfoDatabase.setRegistrationDate(player, Date.valueOf(LocalDate.now())).join();
        }

        if (playerInfoDatabase.getLuckPermsRole(player).join() == null) {
            playerInfoDatabase.setLuckPermsRole(player, LuckPermsConnector.getGroup(player)).join();
        }
    }

    public void sendReferralQuestion(Player player) {
        String message = Main.plugin.getConfig().getString("messages.referral-question");
        if (message != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
        new ReferralQuestionConversation(player);
    }
}
