package de.silke.referralpaper.timecounter;

import lombok.Data;

import java.util.HashMap;
import java.util.UUID;

@Data
public class AfkInterceptor {
    // 1 минута до того, как игрок будет помечен как AFK
    public final long MAX_AFK_TIME = 60 * 1000;
    public HashMap<UUID, Long> playersTime = new HashMap<>();
    public HashMap<UUID, Boolean> markedAfkPlayers = new HashMap<>();

    /**
     * Пометить игрока как AFK или нет
     *
     * @param uuid  UUID игрока
     * @param isAfk true, если игрок AFK
     */
    public void markPlayerAsAfk(UUID uuid, boolean isAfk) {
        markedAfkPlayers.put(uuid, isAfk);
    }

    /**
     * Проверить, является ли игрок AFK
     *
     * @param uuid UUID игрока
     * @return true, если игрок AFK
     */
    public boolean isPlayerAfk(UUID uuid) {
        return markedAfkPlayers.getOrDefault(uuid, false);
    }
}
