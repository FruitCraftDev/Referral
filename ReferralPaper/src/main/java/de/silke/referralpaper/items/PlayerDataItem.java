package de.silke.referralpaper.items;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import de.silke.referralpaper.Main;
import de.silke.referralpaper.luckperms.LuckPermsConnector;
import de.silke.referralpaper.utils.DateConverter;
import de.silke.referralpaper.utils.SkullCreator;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PlayerDataItem {
    @SuppressWarnings({"deprecation", "ConstantConditions"})
    public static GuiItem getHead(Player player) {
        ItemStack scull = new ItemStack(SkullCreator.createHead(player));
        ItemMeta meta = scull.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        String role = LuckPermsConnector.getGroup(player);
        String invitedBy = Main.plugin.getPlayerInfoDatabaseConnection().getReferredPlayerName(player).join();
        String timePlayed = DateConverter.getFormattedTimePlayed(player);
        int invites = Main.plugin.getReferralsDatabaseConnection().getAmountOfTimesUsed(player).join();

        // Цвет ника зависит от роли игрока
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

        if (Main.plugin.getPlayerInfoDatabaseConnection().getReferredPlayerName(player).join() != null) {
            lore.add(ChatColor.GRAY + "Пригласил: " + ChatColor.WHITE + invitedBy);
        }

        lore.add(ChatColor.GRAY + "Время игры: " + ChatColor.WHITE + timePlayed);
        lore.add(ChatColor.GRAY + "Вы пригласили: " + ChatColor.WHITE + invites + "чел.");

        meta.setLore(lore);
        scull.setItemMeta(meta);

        return new GuiItem(scull);
    }
}
