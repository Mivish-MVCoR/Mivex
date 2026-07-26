package ua.mvcor.mivex.config;

import org.bukkit.configuration.file.FileConfiguration;

public class HistoryConfig {

    private static boolean enabled = true;
    private static int maxRecords = 50;

    public static void load(FileConfiguration config) {
        enabled = config.getBoolean("history.enabled", true);
        maxRecords = config.getInt("history.max-records", 50);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int getMaxRecords() {
        return maxRecords;
    }
}
