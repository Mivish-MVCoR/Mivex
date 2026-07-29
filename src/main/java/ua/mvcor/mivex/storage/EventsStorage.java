package ua.mvcor.mivex.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import ua.mvcor.mivex.Mivex;

import java.io.File;
import java.io.IOException;

/**
 * Зберігає момент "старту" системи подій (plugins/Mivex/events.yml),
 * щоб duration-hours рахувався від першого запуску плагіна,
 * а не обнулявся при кожному рестарті сервера.
 */
public class EventsStorage {

    private final File file;
    private final YamlConfiguration config;
    private final long startTime;

    public EventsStorage(Mivex plugin) {
        file = new File(plugin.getDataFolder(), "events.yml");

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(file);

        if (!config.contains("start-time")) {
            startTime = System.currentTimeMillis();
            config.set("start-time", startTime);
            save();
        } else {
            startTime = config.getLong("start-time");
        }
    }

    public long getStartTime() {
        return startTime;
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}