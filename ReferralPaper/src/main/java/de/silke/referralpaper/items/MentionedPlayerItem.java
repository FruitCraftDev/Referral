package de.silke.referralpaper.items;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import de.silke.referralpaper.Main;
import de.silke.referralpaper.luckperms.LuckPermsConnector;
import de.silke.referralpaper.utils.AmountConverter;
import de.silke.referralpaper.utils.SkullCreator;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MentionedPlayerItem {
    @SuppressWarnings({"deprecation", "ConstantConditions"})
    public static GuiItem getHead(Player player, UUID playerUUID) {
        ItemStack skull = new ItemStack(SkullCreator.itemFromUuid(playerUUID));
        ItemMeta meta = skull.getItemMeta();
        List<String> lore = meta.getLore();

        if (lore == null) {
            lore = new ArrayList<>();
        }

        String playerName = Main.plugin.getPlayerInfoDatabase().getPlayerName(playerUUID).join();
        String dateOfRegistration = AmountConverter.getFormattedRegistrationDatePlayed(playerUUID);
        String timePlayed = AmountConverter.getFormattedTimePlayed(playerUUID);
        boolean isContains = Main.plugin.getReferralsDatabase().containsPlayerWithReward(player.getUniqueId(), playerUUID).join();
        long timePlayedFromDatabase = Main.plugin.getPlayerInfoDatabase().getTimePlayed(playerUUID).join();
        // Дополнительная информация
        String getAwardInfo = ChatColor.LIGHT_PURPLE + " (награда)";

        if (LuckPermsConnector.getGroupPrefix(playerUUID) != null) {
            meta.displayName(Component.text(ChatColor.translateAlternateColorCodes('&', LuckPermsConnector.getPrefixColor(playerUUID) + playerName + (isContains ? getAwardInfo : ""))));
        } else {
            meta.displayName(Component.text(ChatColor.translateAlternateColorCodes('&', "&7" + playerName + (isContains ? getAwardInfo : ""))));
        }

        if (dateOfRegistration != null) {
            lore.add(ChatColor.GRAY + "Дата регистрации: " + ChatColor.WHITE + dateOfRegistration);
        }

        // Если время игры меньше 1 минуты
        if (timePlayedFromDatabase < 60000L) {
            lore.add(ChatColor.GRAY + "Время игры: " + ChatColor.WHITE + "меньше минуты");
        } else {
            lore.add(ChatColor.GRAY + "Время игры: " + ChatColor.WHITE + timePlayed);
        }

        meta.setLore(lore);
        skull.setItemMeta(meta);

        return new GuiItem(skull);
    }
}
