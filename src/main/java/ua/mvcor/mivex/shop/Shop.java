package ua.mvcor.mivex.shop;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.UUID;

public class Shop {

    private final UUID id;
    private final UUID owner;

    private Location location;

    private Material item;
    private int amount;
    private int price;
    private String type;
    private final String key;
    private final String restoreCode;

    private boolean lost;

    public Shop(UUID id,
                UUID owner,
                Location location,
                Material item,
                int amount,
                int price,
                String type,
                String key,
                String restoreCode,
                boolean lost) {

        this.id = id;
        this.owner = owner;
        this.location = location;
        this.item = item;
        this.amount = amount;
        this.price = price;
        this.type = type;
        this.key = key;
        this.restoreCode = restoreCode;
        this.lost = lost;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Material getItem() {
        return item;
    }

    public void setItem(Material item) {
        this.item = item;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getKey() {
        return key;
    }

    public String getRestoreCode() {
        return restoreCode;
    }

    public boolean isLost() {
        return lost;
    }

    public void setLost(boolean lost) {
        this.lost = lost;
    }
}