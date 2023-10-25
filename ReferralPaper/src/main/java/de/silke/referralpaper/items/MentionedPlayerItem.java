package de.silke.referralpaper.items;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import de.silke.referralpaper.Main;
import de.silke.referralpaper.luckperms.LuckPermsConnector;
import de.silke.referralpaper.utils.DateConverter;
import de.silke.referralpaper.utils.SkullCreator;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MentionedPlayerItem {
    @SuppressWarnings({"deprecation", "ConstantConditions"})
    public static GuiItem getHead(UUID playerUUID) {
        ItemStack skull = new ItemStack(SkullCreator.itemFromUuid(playerUUID));
        ItemMeta meta = skull.getItemMeta();
        List<String> lore = meta.getLore();

        if (lore == null) {
            lore = new ArrayList<>();
        }

        String playerName = Main.plugin.getPlayerInfoDatabaseConnection().getPlayerName(playerUUID).join();
        String role = LuckPermsConnector.getGroup(playerUUID);
        String dateOfRegistration = DateConverter.getFormattedRegistrationDatePlayed(playerUUID);
        String timePlayed = DateConverter.getFormattedTimePlayed(playerUUID);
        long timePlayedFromDatabase = Main.plugin.getPlayerInfoDatabaseConnection().getTimePlayed(playerUUID).join();

        switch (role) {
            case "default":
                meta.displayName(Component.text(ChatColor.WHITE + playerName));
                break;
            case "content":
                meta.displayName(Component.text(ChatColor.GREEN + playerName));
                break;
            case "admin":
            case "director":
                meta.displayName(Component.text(ChatColor.RED + playerName));
                break;
        }

        if (dateOfRegistration != null) {
            lore.add(ChatColor.GRAY + "Дата регистрации: " + ChatColor.WHITE + dateOfRegistration);
        }

        // Если время игры меньше 1 минуты
        if (!timePlayed.equals("1м") || timePlayedFromDatabase < 60000L) {
            lore.add(ChatColor.GRAY + "Время игры: " + ChatColor.WHITE + "меньше минуты");
        } else {
            lore.add(ChatColor.GRAY + "Время игры: " + ChatColor.WHITE + timePlayed);
        }

        meta.setLore(lore);
        skull.setItemMeta(meta);

        return new GuiItem(skull);
    }
}
