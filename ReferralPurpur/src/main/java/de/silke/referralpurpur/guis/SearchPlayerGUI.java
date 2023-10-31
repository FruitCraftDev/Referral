package de.silke.referralpurpur.guis;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import de.silke.referralpurpur.Main;
import de.silke.referralpurpur.items.BackItem;
import de.silke.referralpurpur.items.ExplanationItem;
import de.silke.referralpurpur.items.NoPlayerFoundItem;
import de.silke.referralpurpur.items.ReferralOwnerPlayerItem;
import de.silke.referralpurpur.utils.PlayerFilter;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class SearchPlayerGUI extends ChestGui {
    private static final int ROWS = 6;
    private static final String TITLE = "Результаты поиска";

    public SearchPlayerGUI(Player player, @Nullable String searchChars) {
        super(ROWS, TITLE);

        setOnGlobalClick(event -> event.setCancelled(true));
        setOnGlobalDrag(event -> event.setCancelled(true));

        String search = Optional.ofNullable(searchChars).orElse("");

        addPane(createBasePane(player, search));
        show(player);

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
    }

    private Pane createBasePane(Player player, String searchChars) {
        final StaticPane pane = new StaticPane(0, 0, 9, 6);

        PlayerFilter playerFilter = new PlayerFilter(Main.plugin.getReferralsDatabase().getReferralOwnersNames().join());
        Set<UUID> filteredPlayerUUIDs = playerFilter.filterPlayers(searchChars);

        GuiItem backItem = new GuiItem(BackItem.getItem().getItem(), event -> {
            AdminInvitesGUI.isAdminAccess = false;
            new AdminInvitesGUI(player);
        });

        int availableSlots = 9;
        int xCoord = 1;
        int yCoord = 1;

        if (filteredPlayerUUIDs.isEmpty()) {
            GuiItem noPlayerFound = new GuiItem(NoPlayerFoundItem.getItem(searchChars).getItem(), event -> {
                player.closeInventory();
                new AdminInvitesGUI(player);
            });
            pane.addItem(noPlayerFound, 4, 2);
        }

        for (UUID playerUUID : filteredPlayerUUIDs) {
            GuiItem item = new GuiItem(ReferralOwnerPlayerItem.getHead(playerUUID).getItem(), event -> {
                if (event.getClick().isRightClick()) {
                    Main.plugin.getPlayerTimeManager().updatePlayerTime(playerUUID);
                    reOpen(player);
                } else {
                    if (Main.plugin.getReferralsDatabase().getAmountOfTimesUsed(playerUUID).join() > 0) {
                        AdminInvitesGUI.isAdminAccess = true;
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

        pane.addItem(backItem, 0, 5);
        pane.addItem(ExplanationItem.getItem(), 4, 5);

        return pane;
    }

    public void reOpen(Player player) {
        if (player.getOpenInventory().title().equals(Component.text(TITLE))) {
            player.closeInventory();
            new SearchPlayerGUI(player, null);
        }
    }
}
