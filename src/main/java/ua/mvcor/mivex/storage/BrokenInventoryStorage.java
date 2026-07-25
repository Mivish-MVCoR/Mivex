package ua.mvcor.mivex.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import ua.mvcor.mivex.Mivex;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Зберігає інвентар зламаних (BROKEN) магазинів в окремих файлах:
 * plugins/Mivex/inventories/<shop-uuid>.yml
 * ItemStack серіалізується стандартним способом Bukkit (config.set/getItemStack),
 * тому зачарування, lore, custom name тощо зберігаються повністю.
 */
public class BrokenInventoryStorage {

    private final File folder;

    public BrokenInventoryStorage(Mivex plugin) {
        folder = new File(plugin.getDataFolder(), "inventories");
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    private File fileFor(UUID shopId) {
        return new File(folder, shopId.toString() + ".yml");
    }

    public boolean exists(UUID shopId) {
        return fileFor(shopId).exists();
    }

    /** Повертає true, якщо збереження пройшло успішно (важливо для атомарності переходу ACTIVE -> BROKEN). */
    public boolean saveInventory(UUID shopId, ItemStack[] contents) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("size", contents.length);

        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                config.set("items." + i, contents[i]);
            }
        }

        try {
            config.save(fileFor(shopId));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Повертає масив предметів, або null якщо файл відсутній чи пошкоджений. */
    public ItemStack[] loadInventory(UUID shopId) {
        File file = fileFor(shopId);
        if (!file.exists()) return null;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains("size")) return null;

        int size = config.getInt("size");
        ItemStack[] contents = new ItemStack[size];

        ConfigurationSection section = config.getConfigurationSection("items");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    if (slot >= 0 && slot < size) {
                        contents[slot] = config.getItemStack("items." + key);
                    }
                } catch (NumberFormatException ignored) {
                    // некоректний ключ у файлі — просто пропускаємо цей слот
                }
            }
        }

        return contents;
    }

    /** Викликати лише ПІСЛЯ успішного відновлення предметів у скриню. */
    public void deleteInventory(UUID shopId) {
        File file = fileFor(shopId);
        if (file.exists()) {
            file.delete();
        }
    }
}