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

public class SearchPlayerItem {

    @SuppressWarnings({"deprecation", "ConstantConditions"})
    public static GuiItem getItem() {
        ItemStack book = new ItemStack(Material.SPYGLASS);
        ItemMeta meta = book.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        List<String> loreFromConfig = Main.plugin.getConfig().getStringList("descriptions.find-player-item-description");

        meta.displayName(Component.text(ChatColor.GREEN + "Поиск игрока"));
        if (lore == null || lore.isEmpty()) {
            lore = new ArrayList<>();
            for (String s : loreFromConfig) {
                lore.add(Colorizer.color(s));
            }
        }

        meta.setLore(lore);
        book.setItemMeta(meta);

        return new GuiItem(book);
    }
}
