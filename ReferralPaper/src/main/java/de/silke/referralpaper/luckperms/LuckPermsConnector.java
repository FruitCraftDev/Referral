package de.silke.referralpaper.luckperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class LuckPermsConnector {
    static RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);

    /**
     * Получить роль игрока
     * <p>Если группа не найдена, то по умолчанию будет возвращена группа "default"
     *
     * @param player Игрок
     * @return Роль игрока
     */
    @SuppressWarnings("ConstantConditions")
    public static String getGroup(Player player) {
        if (provider != null) {
            LuckPerms api = provider.getProvider();
            User user = api.getUserManager().getUser(player.getUniqueId());
            return user.getPrimaryGroup() != null ? user.getPrimaryGroup() : "default";
        }
        return "default";
    }
}
