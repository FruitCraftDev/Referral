package de.silke.referralpaper.items;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import de.silke.referralpaper.Main;
import de.silke.referralpaper.utils.Colorizer;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class BackItem {

    @SuppressWarnings("deprecation")
    public static GuiItem getItem() {
        final ItemStack back = new ItemStack(Material.RED_CONCRETE);
        final ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text(ChatColor.RED + "Назад"));
        List<String> lore = backMeta.getLore();
        String backDescriptionString = Main.plugin.getConfig().getString("descriptions.back-item-description");

        if (lore == null || lore.isEmpty()) {
            lore = new ArrayList<>();
            lore.add(Colorizer.color(backDescriptionString));
        }
        backMeta.setLore(lore);
        back.setItemMeta(backMeta);

        return new GuiItem(back);
    }
}
