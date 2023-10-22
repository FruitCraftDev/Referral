package de.silke.referralpaper.listeners;

import de.silke.referralpaper.timecounter.AfkInterceptor;
import de.silke.referralpaper.timecounter.Counter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class AfkPlayerListener implements Listener {
    private final AfkInterceptor afkInterceptor = new AfkInterceptor();
    private final Counter counter = new Counter();

    @EventHandler
    public void onPlayerNotMove(PlayerMoveEvent event) {
        // If player is not moving for AfkInterceptor.MAX_AFK_TIME, then mark him as AFK AfkInterceptor#markPlayerAsAfk(UUID, boolean)
        Player player = event.getPlayer();
    }
}
