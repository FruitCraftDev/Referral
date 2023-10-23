package de.silke.referralpaper.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.UUID;

public class SkullCreator {
    /**
     * Создаёт голову игрока
     *
     * @return ItemStack черепа игрока
     */
    public static ItemStack createSkull() {
        try {
            return new ItemStack(Material.valueOf("PLAYER_HEAD"));
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

    /**
     * Получить голову игрока из скина
     *
     * @param url - Ссылка на скин
     * @return - Голова игрока
     */
    public static ItemStack createHead(URL url) {
        UUID uuid = UUID.randomUUID();
        PlayerProfile profile = Bukkit.createProfile(uuid);

        PlayerTextures textures = profile.getTextures();
        textures.setSkin(url);

        profile.setTextures(textures);

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.setPlayerProfile(profile);

        return head;
    }

    /**
     * Получить голову игрока из экземпляра игрока
     *
     * @param player - Игрок, чью голову нужно получить
     * @return - Голова игрока
     */
    public static ItemStack createHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        skullMeta.setOwningPlayer(player);

        return head;
    }
}
