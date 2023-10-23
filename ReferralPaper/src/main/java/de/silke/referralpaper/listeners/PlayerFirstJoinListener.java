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
    private final PlayerInfoDatabaseConnection playerInfoDatabase = Main.plugin.getPlayerInfoDatabaseConnection();
    @Getter @Setter
    public static HashMap<UUID, Boolean> newbies = new HashMap<>();

    @EventHandler
    public void onPlayerFirstJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.hasPlayedBefore()) {
            newbies.put(player.getUniqueId(), true);

            if (!playerInfoDatabase.containsPlayer(player).join()) {
                if (
                        playerInfoDatabase.getTimePlayed(player).join() <= 120000 || // игрок играл меньше 2 минут
                        playerInfoDatabase.getReferredPlayerName(player) == null || // или игрок не указал реферала
                        newbies.containsKey(player.getUniqueId()) && newbies.get(player.getUniqueId())) // или игрок в newbies
                {
                    sendReferralQuestion(player);
                }
            } else {
                // Добавляем игрока в БД
//                playerInfoDatabase.addPlayer(player);

                newbies.put(player.getUniqueId(), true);
                sendReferralQuestion(player);
            }
            newbies.put(player.getUniqueId(), true);
        } else {
            if (playerInfoDatabase.containsPlayer(player).join()) {
                return;
            } else {
                // Добавляем игрока в БД
//                playerInfoDatabase.addPlayer(player);

                newbies.put(player.getUniqueId(), true);
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
