package de.silke.referralpaper.listeners;

import de.silke.referralpaper.timecounter.Counter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class StartTimeCounterListener implements Listener {
    private final Counter counter = new Counter();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Запуск счетчика
        counter.start();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Остановка счетчика
        counter.stop();
        // Сохранение времени работы счетчика в БД
        counter.saveTime(event.getPlayer());
    }
}
