package ua.mvcor.mivex.config;

import org.bukkit.configuration.file.FileConfiguration;

public class EventsConfig {

    private static boolean enabled = true;
    private static int durationHours = 0;
    private static boolean firstTradeEnabled = true;
    private static int firstTradeReward = 5;

    public static void load(FileConfiguration config) {
        enabled = config.getBoolean("events.enabled", true);
        durationHours = config.getInt("events.duration-hours", 0);
        firstTradeEnabled = config.getBoolean("events.first-trade.enabled", true);
        firstTradeReward = config.getInt("events.first-trade.reward", 5);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static int getDurationHours() {
        return durationHours;
    }

    public static boolean isFirstTradeEnabled() {
        return firstTradeEnabled;
    }

    public static int getFirstTradeReward() {
        return firstTradeReward;
    }
}