package de.silke.referralpaper;

import de.silke.referralpaper.commands.InvitesCommand;
import de.silke.referralpaper.configuration.Configurator;
import de.silke.referralpaper.database.PlayerInfoDatabaseConnection;
import de.silke.referralpaper.database.ReferralsDatabaseConnection;
import de.silke.referralpaper.listeners.AfkPlayerListener;
import de.silke.referralpaper.listeners.PlayerFirstJoinListener;
import de.silke.referralpaper.listeners.StartTimeCounterListener;
import de.silke.referralpaper.listeners.StopPlayerMovingListener;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

@SuppressWarnings({"ConstantConditions"})
@Getter
public final class Main extends JavaPlugin {
    public static final Logger log = Logger.getLogger("ReferralPaper");
    public static Main plugin;
    private final String version = this.getDescription().getVersion();
    private PlayerInfoDatabaseConnection playerInfoDatabaseConnection;
    private ReferralsDatabaseConnection referralsDatabaseConnection;

    @Override
    public void onEnable() {
        plugin = this;

        // Оповещение о включении плагина
        // TODO: Лог об инициализации только после подключения к бд и т.д
        log.info("v" + version + " инициализирован!");

        // Создание папки для конфигурации
        Configurator.createFolder();

        // Создание файла конфигурации
        Configurator.createConfig();

        // Создание файла наград
        Configurator.createRewards();

        // Проверка наличия LuckPerms
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            log.warning("LuckPerms не найден");
            Bukkit.getPluginManager().disablePlugin(this);
        } else {
            log.info("LuckPerms найден");
        }

        // Подключение к базе данных
        playerInfoDatabaseConnection = new PlayerInfoDatabaseConnection();
        referralsDatabaseConnection = new ReferralsDatabaseConnection();

        // Регистрация команд
        getCommand("invites").setExecutor(new InvitesCommand());

        // Регистрация слушателей
        getServer().getPluginManager().registerEvents(new StartTimeCounterListener(), this);
        getServer().getPluginManager().registerEvents(new AfkPlayerListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerFirstJoinListener(), this);
        getServer().getPluginManager().registerEvents(new StopPlayerMovingListener(), this);
    }

    @Override
    public void onDisable() {
        // Отключение от базы данных
        if (playerInfoDatabaseConnection != null) {
            playerInfoDatabaseConnection.close();
        }

        if (referralsDatabaseConnection != null) {
            referralsDatabaseConnection.close();
        }
    }
}
