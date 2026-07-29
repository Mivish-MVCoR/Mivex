package ua.mvcor.mivex.shop;

import org.bukkit.Location;
import org.bukkit.Material;

import java.util.UUID;

public class Shop {

    private final UUID id;
    private final UUID owner;
    private final Location location;
    private final long createdAt;

    private Material item;
    private int amount;
    private int price;
    private String type;
    private final String key;

    private boolean broken;

    private long totalCurrency;
    private int totalTrades;
    private long lastTradeMillis;

    private boolean firstTradeRewarded;

    public Shop(UUID id,
                UUID owner,
                Location location,
                Material item,
                int amount,
                int price,
                String type,
                String key,
                long createdAt) {

        this.id = id;
        this.owner = owner;
        this.location = location;
        this.item = item;
        this.amount = amount;
        this.price = price;
        this.type = type;
        this.key = key;
        this.createdAt = createdAt;
        this.firstTradeRewarded = false;
        this.broken = false;
        this.totalCurrency = 0;
        this.totalTrades = 0;
        this.lastTradeMillis = 0;
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

    public long getCreatedAt() {
        return createdAt;
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

    public boolean isBroken() {
        return broken;
    }

    public void setBroken(boolean broken) {
        this.broken = broken;
    }

    public long getTotalCurrency() {
        return totalCurrency;
    }

    public void setTotalCurrency(long totalCurrency) {
        this.totalCurrency = totalCurrency;
    }

    public int getTotalTrades() {
        return totalTrades;
    }

    public void setTotalTrades(int totalTrades) {
        this.totalTrades = totalTrades;
    }

    public long getLastTradeMillis() {
        return lastTradeMillis;
    }

    public void setLastTradeMillis(long lastTradeMillis) {
        this.lastTradeMillis = lastTradeMillis;
    }

    /** Викликається лише після УСПІШНОЇ транзакції (не при скасованих операціях). */
    public void recordTrade(int currencyAmount) {
        this.totalTrades++;
        this.totalCurrency += currencyAmount;
        this.lastTradeMillis = System.currentTimeMillis();
    }
    public boolean isFirstTradeRewarded() {
        return firstTradeRewarded;
    }

    public void setFirstTradeRewarded(boolean firstTradeRewarded) {
        this.firstTradeRewarded = firstTradeRewarded;
    }
}