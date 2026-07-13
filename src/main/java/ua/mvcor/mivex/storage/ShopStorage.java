package ua.mvcor.mivex.storage;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ua.mvcor.mivex.Mivex;
import ua.mvcor.mivex.shop.Shop;
import ua.mvcor.mivex.shop.ShopManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class ShopStorage {

    private final Mivex plugin;
    private final ShopManager shopManager;
    private final File file;
    private final File backupFile;
    private final FileConfiguration config;

    public ShopStorage(Mivex plugin, ShopManager shopManager) {
        this.plugin = plugin;
        this.shopManager = shopManager;

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        file = new File(plugin.getDataFolder(), "shops.yml");
        backupFile = new File(plugin.getDataFolder(), "shops.backup.yml");

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    public void saveShops() {

        backupCurrentFile();

        config.set("shops", null);

        for (Shop shop : shopManager.getShops()) {

            String path = "shops." + shop.getId();

            config.set(path + ".owner", shop.getOwner().toString());

            config.set(path + ".world", shop.getLocation().getWorld().getName());
            config.set(path + ".x", shop.getLocation().getBlockX());
            config.set(path + ".y", shop.getLocation().getBlockY());
            config.set(path + ".z", shop.getLocation().getBlockZ());

            config.set(path + ".item", shop.getItem().name());
            config.set(path + ".amount", shop.getAmount());
            config.set(path + ".price", shop.getPrice());
            config.set(path + ".type", shop.getType());
            config.set(path + ".key", shop.getKey());

            config.set(path + ".restoreCode", shop.getRestoreCode());
            config.set(path + ".lost", shop.isLost());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Перед перезаписом shops.yml копіює поточний файл у shops.backup.yml.
     * Files.copy(...) — вбудований спосіб Java скопіювати файл на диску;
     * REPLACE_EXISTING означає "якщо бекап вже є, перезаписати його новим".
     */
    private void backupCurrentFile() {
        if (!file.exists()) return;

        try {
            Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadShops() {

        if (!config.contains("shops")) {
            return;
        }

        for (String idString : config.getConfigurationSection("shops").getKeys(false)) {

            String path = "shops." + idString;

            Location location = new Location(
                    plugin.getServer().getWorld(config.getString(path + ".world")),
                    config.getInt(path + ".x"),
                    config.getInt(path + ".y"),
                    config.getInt(path + ".z")
            );

            UUID id = UUID.fromString(idString);
            UUID owner = UUID.fromString(config.getString(path + ".owner"));

            Shop shop = new Shop(
                    id,
                    owner,
                    location,
                    Material.valueOf(config.getString(path + ".item")),
                    config.getInt(path + ".amount"),
                    config.getInt(path + ".price"),
                    config.getString(path + ".type"),
                    config.getString(path + ".key"),
                    config.getString(path + ".restoreCode"),
                    config.getBoolean(path + ".lost")
            );

            shopManager.addShop(shop);
        }
    }
}