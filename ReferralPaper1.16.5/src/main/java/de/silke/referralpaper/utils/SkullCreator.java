package de.silke.referralpaper.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class SkullCreator {
    /**
     * Создаёт голову игрока
     *
     * @return ItemStack черепа игрока
     */
    @SuppressWarnings("deprecation")
    public static ItemStack createSkull() {
        try {
            return new ItemStack(Material.PLAYER_HEAD);
        } catch (IllegalArgumentException e) {
            return new ItemStack(Material.valueOf("SKULL_ITEM"), 1, (byte) 3);
        }
    }

    /**
     * Создаёт голову игрока используя UUID игрока
     *
     * @param id UUID игрока
     * @return ItemStack голова игрока
     */
    public static ItemStack itemFromUuid(UUID id) {
        return itemWithUuid(createSkull(), id);
    }

    /**
     * Переделывает голову игрока на заданный UUID
     *
     * @param item ItemStack головы игрока
     * @param id   UUID игрока
     * @return ItemStack голова игрока
     */
    public static ItemStack itemWithUuid(ItemStack item, UUID id) {
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(id));
        item.setItemMeta(meta);

        return item;
    }
}
