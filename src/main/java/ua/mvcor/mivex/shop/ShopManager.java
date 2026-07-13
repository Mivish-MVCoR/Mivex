package ua.mvcor.mivex.shop;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShopManager {

    private final List<Shop> shops = new ArrayList<>();

    public boolean isShopAt(Location location) {
        return getShop(location) != null;
    }

    public Shop getShop(Location location) {
        for (Shop shop : shops) {
            if (isSameBlock(shop.getLocation(), location)) {
                return shop;
            }
        }
        return null;
    }

    public void addShop(Shop shop) {
        shops.add(shop);
    }

    public boolean removeShop(Location location) {
        Shop shop = getShop(location);

        if (shop == null) {
            return false;
        }

        shops.remove(shop);
        return true;
    }

    public List<Shop> getShops() {
        return Collections.unmodifiableList(shops);
    }

    private boolean isSameBlock(Location a, Location b) {

        if (a == null || b == null) {
            return false;
        }

        if (a.getWorld() == null || b.getWorld() == null) {
            return false;
        }

        return a.getWorld().equals(b.getWorld())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }
}