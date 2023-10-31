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

public class ExplanationItem {

    @SuppressWarnings({"deprecation", "ConstantConditions"})
    public static GuiItem getItem() {
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        List<String> loreFromConfig = Main.plugin.getConfig().getStringList("descriptions.invite-gui-explanation");

        meta.displayName(Component.text(ChatColor.GREEN + "Что это?"));
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
