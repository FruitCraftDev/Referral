package de.silke.referralpaper.items;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import de.silke.referralpaper.Main;
import de.silke.referralpaper.luckperms.LuckPermsConnector;
import de.silke.referralpaper.utils.AmountConverter;
import de.silke.referralpaper.utils.SkullCreator;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReferralOwnerPlayerItem {
    @SuppressWarnings({"deprecation", "ConstantConditions"})
    public static GuiItem getHead(UUID playerUUID) {
        ItemStack skull = new ItemStack(SkullCreator.itemFromUuid(playerUUID));
        ItemMeta meta = skull.getItemMeta();
        List<String> lore = meta.getLore();

        if (lore == null) {
            lore = new ArrayList<>();
        }

        // Ник игрока
        String playerName = Main.plugin.getReferralsDatabase().getPlayerName(playerUUID).join();
        // Роль игрока
        String role = LuckPermsConnector.getGroup(playerUUID);
        // Дата регистрации
        String dateOfRegistration = AmountConverter.getFormattedRegistrationDatePlayed(playerUUID);
        // Время игры
        String timePlayed = AmountConverter.getFormattedTimePlayed(playerUUID);
        // Время игры в миллисекундах
        long timePlayedFromDatabase = Main.plugin.getPlayerInfoDatabase().getTimePlayed(playerUUID).join();
        // Количество приглашённых игроков
        int invites = Main.plugin.getReferralsDatabase().getAmountOfTimesUsed(playerUUID).join();
        // Ник пригласившего игрока
        String invitedBy = Main.plugin.getPlayerInfoDatabase().getReferredPlayerName(playerUUID).join();
        // Дополнительная информация
        String extraInfoWithInvited = ChatColor.GRAY + " (ПКМ обновить) (ЛКМ приглашённые игроки)";
        // Дополнительная информация
        String extraInfoWithoutInvited = ChatColor.GRAY + " (ПКМ обновить)";


        switch (role) {
            case "default":
                meta.displayName(Component.text(ChatColor.WHITE + playerName + (invites > 0 ? extraInfoWithInvited : extraInfoWithoutInvited)));
                break;
            case "content":
                meta.displayName(Component.text(ChatColor.GREEN + playerName + (invites > 0 ? extraInfoWithInvited : extraInfoWithoutInvited)));
                break;
            case "admin":
            case "director":
                meta.displayName(Component.text(ChatColor.RED + playerName + (invites > 0 ? extraInfoWithInvited : extraInfoWithoutInvited)));
                break;
        }

        // Роль игрока
        if (role != null) {
            lore.add(ChatColor.GRAY + "Роль: " + ChatColor.WHITE + role);
        } else {
            lore.add(ChatColor.GRAY + "Роль: " + ChatColor.WHITE + "игрок");
        }

        // Если время игры меньше 1 минуты
        if (timePlayed.equals("1м") || timePlayedFromDatabase < 60000L) {
            lore.add(ChatColor.GRAY + "Время игры: " + ChatColor.WHITE + "меньше минуты" + ChatColor.GRAY + " (" + timePlayed + ")");
        } else {
            lore.add(ChatColor.GRAY + "Время игры: " + ChatColor.WHITE + timePlayed);
        }

        // Дата регистрации
        if (dateOfRegistration != null) {
            lore.add(ChatColor.GRAY + "Дата регистрации: " + ChatColor.WHITE + dateOfRegistration);
        }

        // Ник пригласившего игрока
        if (invitedBy != null) {
            lore.add(ChatColor.GRAY + "Приглашён: " + ChatColor.WHITE + invitedBy);
        } else {
            lore.add(ChatColor.GRAY + "Приглашён: " + ChatColor.WHITE + "нет");
        }

        // Количество приглашённых игроков
        if (invites != 0) {
            lore.add(ChatColor.GRAY + "Пригласил: " + ChatColor.WHITE + AmountConverter.declensionPlayers(invites));
        } else {
            lore.add(ChatColor.GRAY + "Пригласил: " + ChatColor.WHITE + "никого");
        }

        meta.setLore(lore);
        skull.setItemMeta(meta);

        return new GuiItem(skull);
    }
}
