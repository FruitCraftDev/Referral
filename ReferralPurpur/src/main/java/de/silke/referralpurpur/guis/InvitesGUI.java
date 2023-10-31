package de.silke.referralpurpur.guis;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import de.silke.referralpurpur.Main;
import de.silke.referralpurpur.conversations.ReferralQuestionConversation;
import de.silke.referralpurpur.items.*;
import de.silke.referralpurpur.luckperms.LuckPermsConnector;
import de.silke.referralpurpur.rewards.RewardGiver;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.*;

public class InvitesGUI extends ChestGui {
    private static final int ROWS = 6;
    private static final String TITLE = "Приглашённые игроки";

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
                    setTitle(ChatColor.GREEN + Main.plugin.getReferralsDatabase().getPlayerName(realTarget).join() + ChatColor.RESET + " пригласил(а):");
                    break;
                case "admin":
                case "director":
                    setTitle(ChatColor.RED + Main.plugin.getReferralsDatabase().getPlayerName(realTarget).join() + ChatColor.RESET + " пригласил(а):");
                    break;
                default:
                    setTitle(ChatColor.WHITE + Main.plugin.getReferralsDatabase().getPlayerName(realTarget).join() + ChatColor.RESET + " пригласил(а):");
                    break;
            }

            addPane(createAdminBasePane(player, realTarget));
            show(player);
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
    }

    private Pane createBasePane(Player player) {
        final StaticPane pane = new StaticPane(0, 0, 9, 6);
        UUID playerUUID = player.getUniqueId();

        GuiItem playerDataItem = new GuiItem(PlayerDataItem.getHead(player).getItem(), event -> {
            if (PlayerDataItem.shouldRefresh) {
                Main.log.info("Обновление времени игры для игрока " + player.getName() + "...");
                Main.plugin.getPlayerTimeManager().updatePlayerTime(playerUUID);

                PlayerDataItem.shouldRefresh = false;
                reOpen(player);
            }
        });

        String isInvited = Main.plugin.getPlayerInfoDatabase().getReferredPlayerName(playerUUID).join();
        if (isInvited != null) {
            List<String> invitedPlayerNames = Main.plugin.getReferralsDatabase().getPlayersUsedReferralNick(playerUUID).join();
            Set<String> uniqueNames = new HashSet<>(invitedPlayerNames);

            int availableSlots = 9;
            int xCoord = 1;
            int yCoord = 1;

            for (String playerName : uniqueNames) {
                List<String> playersWhomReceivedAward = Main.plugin.getReferralsDatabase().getPlayerRewardsReceived(playerUUID).join();
                UUID joinedPlayerUUID = Main.plugin.getPlayerInfoDatabase().getPlayerUUID(playerName).join();
                GuiItem item = new GuiItem(MentionedPlayerItem.getHead(player, joinedPlayerUUID).getItem(), event -> {
                    if (event.getClick().isRightClick()) {
                        if (!playersWhomReceivedAward.contains(playerName)) {
                            Main.plugin.getReferralsDatabase().addPlayerWithReward(playerUUID, playerName).join();
                            RewardGiver.giveReward(player);
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                            reOpen(player);
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
        } else {
            GuiItem notInvitedItem = new GuiItem(NotInvitedItem.getItem().getItem(), event -> {
                player.closeInventory();
                new ReferralQuestionConversation(player);
            });
            pane.addItem(notInvitedItem, 4, 2);
        }
        pane.addItem(playerDataItem, 4, 0);
        pane.addItem(ExplanationItem.getItem(), 4, 5);

        return pane;
    }

    private Pane createAdminBasePane(Player player, UUID target) {
        final StaticPane pane = new StaticPane(0, 0, 9, 6);
        UUID playerUUID = player.getUniqueId();

        GuiItem backItem = new GuiItem(BackItem.getItem().getItem(), event -> {
            AdminInvitesGUI.isAdminAccess = false;
            new AdminInvitesGUI(player);
        });

        GuiItem playerDataItem = new GuiItem(PlayerDataItem.getHead(player).getItem(), event -> {
            if (PlayerDataItem.shouldRefresh) {
                Main.plugin.getPlayerTimeManager().updatePlayerTime(playerUUID);

                PlayerDataItem.shouldRefresh = false;
                reOpen(player);
            }
        });

        List<String> invitedPlayerNames = Main.plugin.getReferralsDatabase().getPlayersUsedReferralNick(target).join();
        Set<String> uniqueNames = new HashSet<>(invitedPlayerNames);

        int availableSlots = 9;
        int xCoord = 1;
        int yCoord = 1;

        for (String playerName : uniqueNames) {
            UUID joinedPlayerUUID = Main.plugin.getPlayerInfoDatabase().getPlayerUUID(playerName).join();
            GuiItem item = MentionedPlayerItem.getHead(player, joinedPlayerUUID);

            pane.addItem(item, xCoord, yCoord);
            xCoord += 1;

            // Если координата X больше или равна количеству доступных слотов, то переходим на следующую строку
            if (xCoord >= availableSlots - 1) {
                yCoord += 1;
            }
        }

        pane.addItem(backItem, 0, 5);
        pane.addItem(playerDataItem, 4, 0);
        pane.addItem(AdminExplanationItem.getItem(), 4, 5);

        return pane;
    }

    public void reOpen(Player player) {
        if (player.getOpenInventory().title().equals(Component.text(TITLE))) {
            player.closeInventory();
            new InvitesGUI(player, null);
        }
    }
}
