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
        Predicate<Player> shouldCancelEvent = player -> PlayerFirstJoinListener.newbies.containsKey(player.getUniqueId()) && PlayerFirstJoinListener.newbies.get(player.getUniqueId());

        // debug for player crasher16
        Main.log.info("Do we need to stop crasher16?");
        Main.log.info("Is he in newbies? " + PlayerFirstJoinListener.newbies.containsKey(event.getPlayer().getUniqueId()));

        if (shouldCancelEvent.test(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
