package de.silke.referralpurpur;

import de.silke.referralpurpur.commands.AdminInvitesCommand;
import de.silke.referralpurpur.commands.InvitesCommand;
import de.silke.referralpurpur.configuration.Configurator;
import de.silke.referralpurpur.database.PlayerInfoDatabase;
import de.silke.referralpurpur.database.ReferralsDatabase;
import de.silke.referralpurpur.listeners.PlayerFirstJoinListener;
import de.silke.referralpurpur.listeners.StartTimeCounterListener;
import de.silke.referralpurpur.listeners.StopPlayerMovingListener;
import de.silke.referralpurpur.timecounter.PlayerTimeManager;
import de.silke.referralpurpur.timecounter.PlaytimeUpdateTask;
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
    private PlayerInfoDatabase playerInfoDatabase;
    private ReferralsDatabase referralsDatabase;
    private PlayerTimeManager playerTimeManager;
    private PlaytimeUpdateTask playtimeUpdateTask;

    @Override
    public void onEnable() {
        plugin = this;
        log.info("v" + version + " инициализирован!");

        // Инициализация
        getServer().getScheduler().runTask(this, () -> {
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
//            playerInfoDatabaseConnection = new PlayerInfoDatabaseConnection();
//            referralsDatabaseConnection = new ReferralsDatabaseConnection();
            playerInfoDatabase = new PlayerInfoDatabase();
            referralsDatabase = new ReferralsDatabase();

            // Определяем счётчики игрового времени
            playerTimeManager = new PlayerTimeManager();
            playtimeUpdateTask = new PlaytimeUpdateTask(playerTimeManager);
            playtimeUpdateTask.runTaskTimer(this, 0L, 20 * 30); // 30 секунд

            // Регистрация команд
            getCommand("invites").setExecutor(new InvitesCommand());
            getCommand("admininvites").setExecutor(new AdminInvitesCommand());

            // Регистрация слушателей
            getServer().getPluginManager().registerEvents(new StartTimeCounterListener(), this);
            getServer().getPluginManager().registerEvents(new PlayerFirstJoinListener(), this);
            getServer().getPluginManager().registerEvents(new StopPlayerMovingListener(), this);
        });
    }

    @Override
    public void onDisable() {
        if (playtimeUpdateTask != null) {
            // Перед завершением работы необходимо убедиться, что все ожидающие обновления завершены
            playerTimeManager.onGlobalTimeSave();
            playtimeUpdateTask.cancel();
        }

        // Отключение от базы данных
        if (playerInfoDatabase.getConnectionPool().getConnection() != null || referralsDatabase.getConnectionPool().getConnection() != null) {
            playerInfoDatabase.getConnectionPool().close();
        }
    }
}
