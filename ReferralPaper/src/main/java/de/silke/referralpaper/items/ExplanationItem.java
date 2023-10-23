package de.silke.referralpaper.items;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ExplanationItem {

    @SuppressWarnings({"deprecation", "ConstantConditions"})
    public static GuiItem getItem(Player player) {
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

        meta.displayName(Component.text(ChatColor.GREEN + "Что это?"));
        lore.add(ChatColor.GRAY + "Здесь вы можете увидеть список приглашённых вами игроков.");
        lore.add(ChatColor.GRAY + "Вы также можете посмотреть немного информации о них.");
        lore.add(ChatColor.GRAY + "Например, кто пригласил вас на сервер, сколько вы провели на нём времени или сколько времени играл каждый игрок на сервере.");
        lore.add(ChatColor.GRAY + "Если вы никого не видите, то скорее всего, ни один игрок не указал вас в качестве пригласившего.");

        meta.setLore(lore);
        book.setItemMeta(meta);

        return new GuiItem(book);
    }
}
