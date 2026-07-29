package ua.mvcor.mivex.events;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ua.mvcor.mivex.config.EventsConfig;
import ua.mvcor.mivex.shop.Shop;
import ua.mvcor.mivex.storage.EventsStorage;
import ua.mvcor.mivex.storage.ShopStorage;

import java.util.Map;

/**
 * Логіка ігрових подій (Events) — окремо від основної торгової логіки.
 *
 * Подія "Перша угода": коли магазин власника вперше продає товар
 * РЕАЛЬНОМУ іншому гравцю (не самому собі), і власник, і той гравець
 * отримують нагороду в Phantom Membrane.
 */
public class ShopEvents {

    private static final Material CURRENCY = Material.PHANTOM_MEMBRANE;

    private final EventsStorage eventsStorage;
    private final ShopStorage shopStorage;

    public ShopEvents(EventsStorage eventsStorage, ShopStorage shopStorage) {
        this.eventsStorage = eventsStorage;
        this.shopStorage = shopStorage;
    }

    /** Чи активна система подій зараз (враховує і config "enabled", і автовимкнення через duration-hours). */
    public boolean isActive() {
        if (!EventsConfig.isEnabled()) return false;

        int durationHours = EventsConfig.getDurationHours();
        if (durationHours <= 0) return true;

        long elapsedMillis = System.currentTimeMillis() - eventsStorage.getStartTime();
        long elapsedHours = elapsedMillis / 3_600_000L;

        return elapsedHours < durationHours;
    }

    /**
     * Викликати після КОЖНОЇ успішної торгової операції.
     * otherPlayer — той, хто щойно клікнув по магазину (не обов'язково власник).
     */
    public void onSuccessfulTrade(Shop shop, Player otherPlayer) {

        if (!isActive() || !EventsConfig.isFirstTradeEnabled()) return;
        if (shop.isFirstTradeRewarded()) return;

        // Головна умова: угода з РЕАЛЬНИМ іншим гравцем, а не власник сам із собою.
        if (otherPlayer.getUniqueId().equals(shop.getOwner())) return;

        int reward = EventsConfig.getFirstTradeReward();

        giveReward(otherPlayer, reward);
        otherPlayer.sendMessage("§d🎉 Подія \"Перша угода\"! Отримано " + reward + " Phantom Membrane.");

        OfflinePlayer ownerOffline = Bukkit.getOfflinePlayer(shop.getOwner());
        Player ownerOnline = ownerOffline.getPlayer(); // null, якщо власник зараз офлайн

        if (ownerOnline != null) {
            giveReward(ownerOnline, reward);
            ownerOnline.sendMessage("§d🎉 Подія \"Перша угода\"! Твій магазин уклав першу угоду — отримано " + reward + " Phantom Membrane.");
        }

        shop.setFirstTradeRewarded(true);
        shopStorage.saveShops();
    }

    private void giveReward(Player player, int amount) {
        if (amount <= 0) return;

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack(CURRENCY, amount));
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }
}