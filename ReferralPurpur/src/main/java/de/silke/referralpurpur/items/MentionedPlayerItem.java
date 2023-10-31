package de.silke.referralpurpur.items;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import de.silke.referralpurpur.Main;
import de.silke.referralpurpur.luckperms.LuckPermsConnector;
import de.silke.referralpurpur.utils.AmountConverter;
import de.silke.referralpurpur.utils.SkullCreator;
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
        String role = LuckPermsConnector.getGroup(playerUUID);
        String dateOfRegistration = AmountConverter.getFormattedRegistrationDatePlayed(playerUUID);
        String timePlayed = AmountConverter.getFormattedTimePlayed(playerUUID);
        boolean isContains = Main.plugin.getReferralsDatabase().containsPlayerWithReward(player.getUniqueId(), playerUUID).join();
        long timePlayedFromDatabase = Main.plugin.getPlayerInfoDatabase().getTimePlayed(playerUUID).join();
        // Дополнительная информация
        String getAwardInfo = ChatColor.LIGHT_PURPLE + " (награда)";

        switch (role) {
            case "default":
                meta.displayName(Component.text(ChatColor.WHITE + playerName + (!isContains ? getAwardInfo : "")));
                break;
            case "content":
                meta.displayName(Component.text(ChatColor.GREEN + playerName + (!isContains ? getAwardInfo : "")));
                break;
            case "admin":
            case "director":
                meta.displayName(Component.text(ChatColor.RED + playerName + (!isContains ? getAwardInfo : "")));
                break;
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
