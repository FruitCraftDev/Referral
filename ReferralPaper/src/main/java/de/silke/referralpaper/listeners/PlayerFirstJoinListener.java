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
import java.util.HashMap;
import java.util.UUID;

@Getter
@SuppressWarnings("deprection")
public class PlayerFirstJoinListener implements Listener {
    @Setter
    public static boolean isFirstJoin;
    @Setter
    public static boolean shouldAskQuestion;
    private final PlayerInfoDatabaseConnection playerInfoDatabase = Main.plugin.getPlayerInfoDatabaseConnection();
    public static HashMap<UUID, Boolean> newbies = new HashMap<>();

    @EventHandler
    public void onPlayerFirstJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPlayedBefore()) {
            isFirstJoin = true;

            if (!playerInfoDatabase.containsPlayer(player).join()) {
                if (
                        playerInfoDatabase.getTimePlayed(player).join() <= 120000 || // or if the player played for less than 2 minutes
                                playerInfoDatabase.getReferredPlayerName(player) == null || // or the player has not yet specified a referral
                                isFirstJoin || // or the player entered the server for the first time
                                shouldAskQuestion) { // or the player needs to ask a question
                    sendReferralQuestion(player);
                }
            } else {
                // Adding a player to the database
                playerInfoDatabase.addPlayer(player);

                isFirstJoin = true;
                shouldAskQuestion = true;
                sendReferralQuestion(player);
            }
            newbies.put(player.getUniqueId(), true);
        } else {
            if (playerInfoDatabase.containsPlayer(player).join()) {
                return;
            } else {
                // Adding a player to the database
                playerInfoDatabase.addPlayer(player);

                isFirstJoin = true;
                shouldAskQuestion = true;
                sendReferralQuestion(player);
            }
            newbies.put(player.getUniqueId(), true);
        }

        // Проверка на заполненность информации об игроке в БД
        // Также служит для обновления информации об игроке в БД
        if (playerInfoDatabase.getRegistrationDate(player).join() == null) {
            playerInfoDatabase.setRegistrationDate(player, Date.valueOf(LocalDate.now())).join();
        }

        if (playerInfoDatabase.getLuckPermsRole(player).join() == null || playerInfoDatabase.getLuckPermsRole(player).join() != null) {
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
