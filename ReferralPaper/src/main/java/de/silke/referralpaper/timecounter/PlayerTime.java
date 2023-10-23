package de.silke.referralpaper.timecounter;

import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@Data
public class PlayerTime {
    private UUID playerUUID;
    private long totalPlaytime;
    private long loginTime;

    public PlayerTime(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.totalPlaytime = 0L;
    }

    /**
     * Получить игрока по его UUID
     * @param playerUUID UUID игрока
     * @return Игрок
     */
    protected Player getPlayer(UUID playerUUID) {
        return Bukkit.getOfflinePlayer(playerUUID).getPlayer();
    }
}
