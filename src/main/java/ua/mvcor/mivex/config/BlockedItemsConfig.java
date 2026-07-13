package ua.mvcor.mivex.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.Set;

public class BlockedItemsConfig {

    private static final Set<Material> blockedItems = new HashSet<>();

    public static void load(FileConfiguration config) {
        blockedItems.clear();
        for (String name : config.getStringList("blocked-items")) {
            Material material = Material.matchMaterial(name);
            if (material != null) {
                blockedItems.add(material);
            }
        }
    }

    public static boolean isBlocked(Material material) {
        return blockedItems.contains(material);
    }
}