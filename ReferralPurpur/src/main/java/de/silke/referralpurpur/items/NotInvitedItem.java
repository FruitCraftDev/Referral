package de.silke.referralpurpur.items;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import de.silke.referralpurpur.Main;
import de.silke.referralpurpur.utils.Colorizer;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class NotInvitedItem {

    @SuppressWarnings({"deprecation", "ConstantConditions"})
    public static GuiItem getItem() {
        final ItemStack back = new ItemStack(Material.ENDER_EYE);
        final ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text(ChatColor.RED + "Вас никто не приглашал"));

        List<String> lore = backMeta.hasLore() ? backMeta.getLore() : new ArrayList<>();
        List<String> notInvitedDescription = Main.plugin.getConfig().getStringList("descriptions.not-invited-item-description");

        if (lore == null || lore.isEmpty()) {
            lore = new ArrayList<>();
            for (String s : notInvitedDescription) {
                lore.add(String.format(Colorizer.color(s)));
            }
        }
        backMeta.setLore(lore);
        back.setItemMeta(backMeta);

        return new GuiItem(back);
    }
}
