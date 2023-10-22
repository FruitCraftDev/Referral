package de.silke.referralpaper.listeners;

import de.silke.referralpaper.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.function.Predicate;

public class StopPlayerMovingListener implements Listener {
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Predicate<Player> shouldCancelEvent = player -> PlayerFirstJoinListener.isFirstTimePlayerJoin() || Main.plugin.getPlayerInfoDatabaseConnection().getDeclinedReferQuestion(player).join().equals(true);

        if (shouldCancelEvent.test(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
