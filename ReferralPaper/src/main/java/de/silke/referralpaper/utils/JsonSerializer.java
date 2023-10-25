package de.silke.referralpaper.utils;

import com.google.gson.Gson;

import java.util.List;

public class JsonSerializer {
    public static String serializeToJSON(List<String> nicknames) {
        Gson gson = new Gson();
        return gson.toJson(nicknames);
    }

    public static List deserializeFromJSON(String nicknamesJson) {
        Gson gson = new Gson();
        return gson.fromJson(nicknamesJson, List.class);
    }
}
