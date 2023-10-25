package de.silke.referralpaper.guis;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import de.silke.referralpaper.Main;
import de.silke.referralpaper.items.ExplanationItem;
import de.silke.referralpaper.items.MentionedPlayerItem;
import de.silke.referralpaper.items.PlayerDataItem;
import de.silke.referralpaper.luckperms.LuckPermsConnector;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.*;

public class InvitesGUI extends ChestGui {
    private static final int ROWS = 6;
    private static final String TITLE = "Приглашённые игроки";
    private static final int SLOT_DISTANCE = 1;

    public InvitesGUI(Player player, @Nullable UUID target) {
        super(ROWS, TITLE);

        setOnGlobalClick(event -> event.setCancelled(true));
        setOnGlobalDrag(event -> event.setCancelled(true));

        if (!AdminInvitesGUI.isAdminAccess) {
            addPane(createBasePane(player));
            show(player);
        } else {
            UUID realTarget = Optional.ofNullable(target).orElse(player.getUniqueId());

            switch (LuckPermsConnector.getGroup(realTarget)) {
                case "content":
                    setTitle("Приглашённые игроки" + ChatColor.GREEN + Main.plugin.getReferralsDatabaseConnection().getPlayerName(realTarget).join());
                    break;
                case "admin":
                case "director":
                    setTitle("Приглашённые игроки" + ChatColor.RED + Main.plugin.getReferralsDatabaseConnection().getPlayerName(realTarget).join());
                    break;
                default:
                    setTitle("Приглашённые игроки" + ChatColor.WHITE + Main.plugin.getReferralsDatabaseConnection().getPlayerName(realTarget).join());
                    break;
            }

            addPane(createAdminBasePane(player, realTarget));
            show(player);
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
    }

    private Pane createBasePane(Player player) {
        final StaticPane pane = new StaticPane(0, 0, 9, 6);

        GuiItem playerDataItem = new GuiItem(PlayerDataItem.getHead(player).getItem(), event -> {
            if (PlayerDataItem.shouldRefresh) {
                Main.plugin.getPlayerTimeManager().updatePlayerTime(player.getUniqueId());

                PlayerDataItem.shouldRefresh = false;
                reOpen(player);
            }
        });

        List<String> invitedPlayerNames = Main.plugin.getReferralsDatabaseConnection().getPlayersUsedReferralNick(player.getUniqueId()).join();
        Set<String> uniqueNames = new HashSet<>(invitedPlayerNames);

        int slot = SLOT_DISTANCE;
        for (String playerName : uniqueNames) {
            UUID playerUUID = Main.plugin.getPlayerInfoDatabaseConnection().getPlayerUUID(playerName).join();
            GuiItem item = MentionedPlayerItem.getHead(playerUUID);

            pane.addItem(item, slot, 1);
            slot += SLOT_DISTANCE;
        }

        pane.addItem(playerDataItem, 4, 0);
        pane.addItem(ExplanationItem.getItem(), 4, 5);

        return pane;
    }

    @SuppressWarnings("deprecation")
    private Pane createAdminBasePane(Player player, UUID target) {
        final StaticPane pane = new StaticPane(0, 0, 9, 6);
        final ItemStack back = new ItemStack(Material.RED_CONCRETE);
        final ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text(ChatColor.RED + "Назад"));
        List<String> lore = backMeta.getLore();

        if (lore == null) {
            lore = new ArrayList<>();
        }

        lore.add(ChatColor.GRAY + "Нажмите, чтобы вернуться назад к" + ChatColor.WHITE + " реферальной системе");
        backMeta.setLore(lore);
        back.setItemMeta(backMeta);

        GuiItem backItem = new GuiItem(back, event -> {
            AdminInvitesGUI.isAdminAccess = false;
            new AdminInvitesGUI(player);
        });

        GuiItem playerDataItem = new GuiItem(PlayerDataItem.getHead(player).getItem(), event -> {
            if (PlayerDataItem.shouldRefresh) {
                Main.plugin.getPlayerTimeManager().updatePlayerTime(player.getUniqueId());

                PlayerDataItem.shouldRefresh = false;
                reOpen(player);
            }
        });

        List<String> invitedPlayerNames = Main.plugin.getReferralsDatabaseConnection().getPlayersUsedReferralNick(target).join();
        Set<String> uniqueNames = new HashSet<>(invitedPlayerNames);

        int slot = SLOT_DISTANCE;
        for (String playerName : uniqueNames) {
            UUID playerUUID = Main.plugin.getPlayerInfoDatabaseConnection().getPlayerUUID(playerName).join();
            GuiItem item = MentionedPlayerItem.getHead(playerUUID);

            pane.addItem(item, slot, 1);
            slot += SLOT_DISTANCE;
        }

        pane.addItem(backItem, 0, 5);
        pane.addItem(playerDataItem, 4, 0);
        pane.addItem(ExplanationItem.getItem(), 4, 5);

        return pane;
    }

    public void reOpen(Player player) {
        if (player.getOpenInventory().title().equals(Component.text(TITLE))) {
            player.closeInventory();
            new InvitesGUI(player, null);
        }
    }
}
