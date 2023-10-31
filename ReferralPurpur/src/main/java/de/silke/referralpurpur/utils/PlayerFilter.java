package de.silke.referralpurpur.utils;

import de.silke.referralpurpur.Main;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayerFilter {
    private final List<String> allPlayerNames;

    public PlayerFilter(List<String> allPlayerNames) {
        this.allPlayerNames = allPlayerNames;
    }

    public Set<UUID> filterPlayers(String searchChars) {
        Set<String> filteredNames = allPlayerNames.stream()
                .filter(playerName -> playerName.toLowerCase().contains(searchChars.toLowerCase()))
                .collect(Collectors.toSet());

        Set<UUID> filteredUUIDs = new HashSet<>();
        for (String playerName : filteredNames) {
            UUID playerUUID = Main.plugin.getReferralsDatabase().getPlayerUUID(playerName).join();
            filteredUUIDs.add(playerUUID);
        }

        return filteredUUIDs;
    }
}