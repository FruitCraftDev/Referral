package de.silke.referralpaper.luckperms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

    /**
     * Получить роль игрока по UUID
     * <p>Если группа не найдена, то по умолчанию будет возвращена группа "default"
     *
     * @param playerUUID UUID игрока
     * @return Роль игрока
     */
    @SuppressWarnings("ConstantConditions")
    public static String getGroup(UUID playerUUID) {
        if (provider != null) {
            LuckPerms api = provider.getProvider();
            UserManager userManager = api.getUserManager();
            CompletableFuture<User> userFuture = userManager.loadUser(playerUUID);
            return userFuture.join().getPrimaryGroup() != null ? userFuture.join().getPrimaryGroup() : "default";
        }
        return "default";
    }

    public static String getGroupPrefix(UUID playerUUID) {
        if (provider != null) {
            LuckPerms api = provider.getProvider();
            UserManager userManager = api.getUserManager();
            CompletableFuture<User> userFuture = userManager.loadUser(playerUUID);
            return userFuture.join().getCachedData().getMetaData().getPrefix();
        }
        return "";
    }

    public static String getPrefixColor(UUID playerUUID) {
        if (provider != null) {
            LuckPerms api = provider.getProvider();
            UserManager userManager = api.getUserManager();
            CompletableFuture<User> userFuture = userManager.loadUser(playerUUID);
            return userFuture.join().getCachedData().getMetaData().getPrefix().substring(0, 2);
        }
        return "";
    }
}
