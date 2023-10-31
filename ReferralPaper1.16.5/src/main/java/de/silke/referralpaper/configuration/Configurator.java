package de.silke.referralpaper.configuration;

import de.silke.referralpaper.Main;

import java.io.File;

public class Configurator {
    /**
     * Создать файл конфигурации
     */
    public static void createConfig() {
        Main.plugin.getConfig().options().copyDefaults(true);
        Main.plugin.saveConfig();
    }

    /**
     * Создать файл наград
     */
    public static void createRewards() {
        File rewardsFile = new File(Main.plugin.getDataFolder(), "rewards.yml");
        if (!rewardsFile.exists()) {
            Main.plugin.saveResource("rewards.yml", false);
        }
    }

    /**
     * Создать папку для конфигурации
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void createFolder() {
        if (!Main.plugin.getDataFolder().exists()) {
            Main.plugin.getDataFolder().mkdir();
        }
    }
}
