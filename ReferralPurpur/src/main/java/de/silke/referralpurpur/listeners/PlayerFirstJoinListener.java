package de.silke.referralpurpur.listeners;

import de.silke.referralpurpur.Main;
import de.silke.referralpurpur.conversations.ReferralQuestionConversation;
import de.silke.referralpurpur.database.PlayerInfoDatabase;
import de.silke.referralpurpur.luckperms.LuckPermsConnector;
import de.silke.referralpurpur.utils.Colorizer;
import lombok.Getter;
import lombok.Setter;
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
    public static HashMap<UUID, Boolean> newbies = new HashMap<>();
    private final PlayerInfoDatabase playerInfoDatabase = Main.plugin.getPlayerInfoDatabase();

    @EventHandler
    public void onPlayerFirstJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (!player.hasPlayedBefore()) {
            newbies.put(player.getUniqueId(), true);

            if (!playerInfoDatabase.containsPlayer(playerUUID).join()) {
                sendReferralQuestion(player);
            } else {
                newbies.put(playerUUID, true);
                sendReferralQuestion(player);
            }
            newbies.put(playerUUID, true);
        } else {
            if (playerInfoDatabase.containsPlayer(playerUUID).join()) {
                return;
            } else {
                newbies.put(playerUUID, true);
                sendReferralQuestion(player);
            }
            newbies.put(playerUUID, true);
        }

        if (playerInfoDatabase.containsPlayer(playerUUID).join()) {
            // Проверка на заполненность информации об игроке в БД
            // Также служит для обновления информации об игроке в БД
            if (playerInfoDatabase.getRegistrationDate(playerUUID).join() == null) {
                playerInfoDatabase.setRegistrationDate(playerUUID, Date.valueOf(LocalDate.now())).join();
            }

            if (playerInfoDatabase.getLuckPermsRole(playerUUID).join() == null || playerInfoDatabase.getLuckPermsRole(playerUUID).join() != null) {
                playerInfoDatabase.setLuckPermsRole(playerUUID, LuckPermsConnector.getGroup(player)).join();
            }
        }
    }

    public void sendReferralQuestion(Player player) {
        String message = Main.plugin.getConfig().getString("messages.referral-question");
        if (message != null) {
            player.sendMessage(Colorizer.color(message));
        }
        new ReferralQuestionConversation(player);
    }
}
