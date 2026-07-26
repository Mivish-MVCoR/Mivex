package ua.mvcor.mivex.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ua.mvcor.mivex.Mivex;
import ua.mvcor.mivex.config.HistoryConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Історія успішних торгових операцій одного магазину:
 * plugins/Mivex/history/<shop-uuid>.yml
 * Не кешується в пам'яті між командами (навантаження лише на час читання/запису файлу).
 */
public class HistoryStorage {

    public static class HistoryRecord {
        public final String playerName;
        public final String verb;      // "купив" або "продав"
        public final String itemName;  // Material.name()
        public final int amount;
        public final int price;
        public final long timeMillis;

        public HistoryRecord(String playerName, String verb, String itemName, int amount, int price, long timeMillis) {
            this.playerName = playerName;
            this.verb = verb;
            this.itemName = itemName;
            this.amount = amount;
            this.price = price;
            this.timeMillis = timeMillis;
        }
    }

    private final File folder;

    public HistoryStorage(Mivex plugin) {
        folder = new File(plugin.getDataFolder(), "history");
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    private File fileFor(UUID shopId) {
        return new File(folder, shopId.toString() + ".yml");
    }

    public void addRecord(UUID shopId, HistoryRecord record) {
        if (!HistoryConfig.isEnabled()) return;

        File file = fileFor(shopId);
        YamlConfiguration config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();

        List<HistoryRecord> records = readAll(config);
        records.add(record);

        int max = HistoryConfig.getMaxRecords();
        while (records.size() > max) {
            records.remove(0);
        }

        writeAll(config, records);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<HistoryRecord> loadRecords(UUID shopId) {
        File file = fileFor(shopId);
        if (!file.exists()) return new ArrayList<>();

        return readAll(YamlConfiguration.loadConfiguration(file));
    }

    public void deleteHistory(UUID shopId) {
        File file = fileFor(shopId);
        if (file.exists()) {
            file.delete();
        }
    }

    private List<HistoryRecord> readAll(YamlConfiguration config) {
        List<HistoryRecord> records = new ArrayList<>();

        ConfigurationSection section = config.getConfigurationSection("records");
        if (section == null) return records;

        List<Integer> indexes = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            try {
                indexes.add(Integer.parseInt(key));
            } catch (NumberFormatException ignored) {
            }
        }
        indexes.sort(Integer::compareTo);

        for (int index : indexes) {
            String path = "records." + index;
            records.add(new HistoryRecord(
                    config.getString(path + ".player"),
                    config.getString(path + ".verb"),
                    config.getString(path + ".item"),
                    config.getInt(path + ".amount"),
                    config.getInt(path + ".price"),
                    config.getLong(path + ".time")
            ));
        }

        return records;
    }

    private void writeAll(YamlConfiguration config, List<HistoryRecord> records) {
        config.set("records", null);

        for (int i = 0; i < records.size(); i++) {
            HistoryRecord r = records.get(i);
            String path = "records." + i;
            config.set(path + ".player", r.playerName);
            config.set(path + ".verb", r.verb);
            config.set(path + ".item", r.itemName);
            config.set(path + ".amount", r.amount);
            config.set(path + ".price", r.price);
            config.set(path + ".time", r.timeMillis);
        }
    }
}