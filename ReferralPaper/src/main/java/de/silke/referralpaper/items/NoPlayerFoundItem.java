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

public class NoPlayerFoundItem {

    @SuppressWarnings({"deprecation", "ConstantConditions"})
    public static GuiItem getItem(String searchChars) {
        final ItemStack back = new ItemStack(Material.SPYGLASS);
        final ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(Component.text(ChatColor.RED + "Ничего не найдено"));

        List<String> lore = backMeta.hasLore() ? backMeta.getLore() : new ArrayList<>();
        List<String> noPlayerFoundDescription = Main.plugin.getConfig().getStringList("descriptions.no-player-found-item-description");

        if (lore == null || lore.isEmpty()) {
            lore = new ArrayList<>();
            for (String s : noPlayerFoundDescription) {
                lore.add(String.format(Colorizer.color(s), searchChars));
            }
        }
        backMeta.setLore(lore);
        back.setItemMeta(backMeta);

        return new GuiItem(back);
    }
}
