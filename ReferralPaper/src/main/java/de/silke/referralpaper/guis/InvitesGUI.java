package de.silke.referralpaper.guis;

import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import de.silke.referralpaper.items.ExplanationItem;
import de.silke.referralpaper.items.PlayerDataItem;
import org.bukkit.entity.Player;

public class InvitesGUI extends ChestGui {
    private static final int ROWS = 6;
    private static final String TITLE = "Приглашённые игроки";

    public InvitesGUI(Player player) {
        super(ROWS, TITLE);

        setOnGlobalClick(event -> event.setCancelled(true));
        setOnGlobalDrag(event -> event.setCancelled(true));

        addPane(createBasePane(player));
        show(player);
    }

    private Pane createBasePane(Player player) {
        final StaticPane pane = new StaticPane(0, 0, 9, 6);



        pane.addItem(PlayerDataItem.getHead(player), 4, 0);
        pane.addItem(ExplanationItem.getItem(player), 4, 6);

        return pane;
    }
}
