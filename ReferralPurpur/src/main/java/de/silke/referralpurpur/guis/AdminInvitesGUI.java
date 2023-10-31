package de.silke.referralpurpur.guis;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import de.silke.referralpurpur.Main;
import de.silke.referralpurpur.conversations.SearchPlayerConversation;
import de.silke.referralpurpur.items.AdminExplanationItem;
import de.silke.referralpurpur.items.ReferralOwnerPlayerItem;
import de.silke.referralpurpur.items.SearchPlayerItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AdminInvitesGUI extends ChestGui {
    private static final int ROWS = 6;
    private static final String TITLE = "Реферальная система";
    public static boolean isAdminAccess = false;

    public AdminInvitesGUI(Player player) {
        super(ROWS, TITLE);

        setOnGlobalClick(event -> event.setCancelled(true));
        setOnGlobalDrag(event -> event.setCancelled(true));

        addPane(createBasePane(player));
        show(player);

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
    }

    private Pane createBasePane(Player player) {
        final StaticPane pane = new StaticPane(0, 0, 9, 6);

        List<String> referralOwnerNames = Main.plugin.getReferralsDatabase().getReferralOwnersNames().join();
        Set<String> uniqueOwnersNames = new HashSet<>(referralOwnerNames);

        int availableSlots = 9;
        int xCoord = 1;
        int yCoord = 1;

        for (String playerName : uniqueOwnersNames) {
            UUID playerUUID = Main.plugin.getReferralsDatabase().getPlayerUUID(playerName).join();

            GuiItem item = new GuiItem(ReferralOwnerPlayerItem.getHead(playerUUID).getItem(), event -> {
                if (event.getClick().isRightClick()) {
                    Main.plugin.getPlayerTimeManager().updatePlayerTime(playerUUID);

                    reOpen(player);
                } else {
                    if (Main.plugin.getReferralsDatabase().getAmountOfTimesUsed(playerUUID).join() > 0) {
                        // Указываем админский доступ для правильного отображения GUI
                        isAdminAccess = true;
                        new InvitesGUI(player, playerUUID);
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1, 1);
                    }
                }
            });

            pane.addItem(item, xCoord, yCoord);
            xCoord += 1;

            // Если координата X больше или равна количеству доступных слотов, то переходим на следующую строку
            if (xCoord >= availableSlots - 1) {
                xCoord = 1;
                yCoord += 1;
            }
        }

        GuiItem searchItem = new GuiItem(SearchPlayerItem.getItem().getItem(), event -> {
            player.closeInventory();
            new SearchPlayerConversation(player);
        });

        pane.addItem(AdminExplanationItem.getItem(), 4, 5);
        pane.addItem(searchItem, 0, 5);

        return pane;
    }

    public void reOpen(Player player) {
        if (player.getOpenInventory().title().equals(Component.text(TITLE))) {
            player.closeInventory();
            new AdminInvitesGUI(player);
        }
    }
}
