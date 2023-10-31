package de.silke.referralpurpur.listeners;

import de.silke.referralpurpur.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class StartTimeCounterListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        Main.log.info("Отмечаем время входа игрока в игру");
        Main.plugin.getPlayerTimeManager().playerLogin(playerUUID);
        Main.plugin.getPlayerTimeManager().getPlayerStartTimes().put(playerUUID, System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        Main.plugin.getPlayerTimeManager().playerLogout(playerUUID);
        Main.plugin.getPlayerTimeManager().updatePlayerTime(playerUUID);
    }
}
