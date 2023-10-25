package de.silke.referralpaper.items;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import de.silke.referralpaper.Main;
import de.silke.referralpaper.luckperms.LuckPermsConnector;
import de.silke.referralpaper.utils.DateConverter;
import de.silke.referralpaper.utils.SkullCreator;
import lombok.Data;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@Data
public class PlayerDataItem {
    public static boolean shouldRefresh = false;

    @SuppressWarnings({"deprecation", "ConstantConditions"})
    public static GuiItem getHead(Player player) {
        ItemStack skull = new ItemStack(SkullCreator.itemFromUuid(player.getUniqueId()));
        ItemMeta meta = skull.getItemMeta();
        List<String> lore = meta.getLore();

        if (lore == null) {
            lore = new ArrayList<>();
        }

        String role = LuckPermsConnector.getGroup(player);
        String invitedBy = Main.plugin.getPlayerInfoDatabaseConnection().getReferredPlayerName(player).join();
        String timePlayed = DateConverter.getFormattedTimePlayed(player);
        long timePlayedFromDatabase = Main.plugin.getPlayerInfoDatabaseConnection().getTimePlayed(player).join();
        int invites = Main.plugin.getReferralsDatabaseConnection().getAmountOfTimesUsed(player).join();

        switch (role) {
            case "default":
                meta.displayName(Component.text(ChatColor.WHITE + player.getName()));
                break;
            case "content":
                meta.displayName(Component.text(ChatColor.GREEN + player.getName()));
                break;
            case "admin":
            case "director":
                meta.displayName(Component.text(ChatColor.RED + player.getName()));
                break;
        }

        if (invitedBy != null) {
            lore.add(ChatColor.GRAY + "Пригласил: " + ChatColor.WHITE + invitedBy);
        }

        if (timePlayed.equals("0") || timePlayed == null || timePlayedFromDatabase == 0L) {
            lore.add(ChatColor.GRAY + "Время игры: " + ChatColor.WHITE + "меньше минуты " + ChatColor.GREEN + "(обновить)");
            shouldRefresh = true;
        } else {
            lore.add(ChatColor.GRAY + "Время игры: " + ChatColor.WHITE + timePlayed);
        }

        if (invites > 0) {
            lore.add(ChatColor.GRAY + "Вы пригласили: " + ChatColor.WHITE + invites + " чел.");
        }

        meta.setLore(lore);
        skull.setItemMeta(meta);

        return new GuiItem(skull);
    }
}
