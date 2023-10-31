package de.silke.referralpaper.rewards;

import de.silke.referralpaper.Main;
import lombok.Data;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class RewardsParser {
    public static final File rewardsFile = new File(Main.plugin.getDataFolder(), "rewards.yml");
    public static List<String> rewardCommands;

    private List<String> loadRewardCommands() {
        try (FileReader fileReader = new FileReader(rewardsFile)) {
            Yaml yaml = new Yaml();
            Object data = yaml.load(fileReader);

            if (data instanceof Map) {
                Map<String, List<String>> yamlData = (Map<String, List<String>>) data;
                if (yamlData.containsKey("reward-commands")) {
                    return yamlData.get("reward-commands");
                }
            }

            // Handle cases where the structure doesn't match or the file is missing
            return new ArrayList<>();
        } catch (Exception e) {
            Main.log.severe("REWARDS_PARSER: Ошибка при загрузке наград из файла");
            return new ArrayList<>();
        }
    }

    public List<String> getRewardCommands() {
        if (rewardCommands == null) {
            rewardCommands = loadRewardCommands();
        }
        return rewardCommands;
    }
}
