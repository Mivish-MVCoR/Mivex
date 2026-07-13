package ua.mvcor.mivex.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import ua.mvcor.mivex.shop.Shop;
import ua.mvcor.mivex.shop.ShopManager;
import ua.mvcor.mivex.storage.ShopStorage;

import java.util.Map;

public class ShopListener implements Listener {

    private static final Material CURRENCY = Material.PHANTOM_MEMBRANE;
    private static final Material KEY_ITEM_MATERIAL = Material.TRIPWIRE_HOOK;

    private final ShopManager shopManager;
    private final ShopStorage shopStorage;

    public ShopListener(ShopManager shopManager, ShopStorage shopStorage) {
        this.shopManager = shopManager;
        this.shopStorage = shopStorage;
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Shop shop = shopManager.getShop(event.getBlock().getLocation());
        if (shop == null) return;

        shop.setLost(true);
        shopStorage.saveShops();

        if (event.getPlayer().getUniqueId().equals(shop.getOwner())) {
            event.getPlayer().sendMessage("§eТвій магазин зламано і позначено як LOST.");
            event.getPlayer().sendMessage("§7Код відновлення: §e" + shop.getRestoreCode());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) return;

        BlockState state = block.getState();
        if (!(state instanceof Chest)) return;

        Shop shop = shopManager.getShop(block.getLocation());

        if (shop == null) {
            return;
        }

        if (shop.isLost()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cЦей магазин втрачено (LOST). Власник має відновити його через /cshop restore.");
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        ItemStack handItem = player.getInventory().getItemInMainHand();

        String keyText = getKeyText(handItem);

        if (keyText != null) {
            if (shop.getKey().equals(keyText)) {
                player.openInventory(((Chest) state).getInventory());
            } else {
                player.sendMessage("§cНевірний ключ для цього магазину.");
            }
            return;
        }

        if (handItem.getType() == CURRENCY) {
            if (!shop.getType().equals("sell")) {
                player.sendMessage("§cЦей магазин не продає товар.");
                return;
            }
            handleBuy(player, shop, (Chest) state);
            return;
        }

        if (handItem.getType() == shop.getItem()) {
            if (!shop.getType().equals("buy")) {
                player.sendMessage("§cЦей магазин не купує товар.");
                return;
            }
            handleSell(player, shop, (Chest) state);
            return;
        }

        player.sendMessage("§7Тримай " + shop.getItem() + " або Phantom Membrane для цього магазину.");
    }

    private String getKeyText(ItemStack item) {
        if (item == null || item.getType() != KEY_ITEM_MATERIAL) return null;
        if (!item.hasItemMeta()) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return null;

        return meta.getDisplayName();
    }

    private void handleBuy(Player player, Shop shop, Chest chestState) {
        PlayerInventory playerInv = player.getInventory();
        Inventory chestInv = chestState.getInventory();

        int needCurrency = shop.getPrice();
        int needItem = shop.getAmount();

        if (countMatching(playerInv, CURRENCY) < needCurrency) {
            player.sendMessage("§cНедостатньо Phantom Membrane. Потрібно: " + needCurrency + ".");
            return;
        }

        if (countMatching(chestInv, shop.getItem()) < needItem) {
            player.sendMessage("§cМагазин порожній.");
            return;
        }

        Map<Integer, ItemStack> leftoverPlayer = playerInv.removeItem(new ItemStack(CURRENCY, needCurrency));
        if (!leftoverPlayer.isEmpty()) {
            player.sendMessage("§cПомилка транзакції. Спробуй ще раз.");
            return;
        }

        Map<Integer, ItemStack> leftoverChest = chestInv.removeItem(new ItemStack(shop.getItem(), needItem));
        if (!leftoverChest.isEmpty()) {
            playerInv.addItem(new ItemStack(CURRENCY, needCurrency));
            player.sendMessage("§cПомилка транзакції. Спробуй ще раз.");
            return;
        }

        chestInv.addItem(new ItemStack(CURRENCY, needCurrency));

        Map<Integer, ItemStack> leftoverGive = playerInv.addItem(new ItemStack(shop.getItem(), needItem));
        for (ItemStack drop : leftoverGive.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        shopStorage.saveShops();

        player.sendMessage("§aКуплено " + needItem + "x " + shop.getItem() + " за " + needCurrency + " мембран.");
    }

    private void handleSell(Player player, Shop shop, Chest chestState) {
        PlayerInventory playerInv = player.getInventory();
        Inventory chestInv = chestState.getInventory();

        int needItem = shop.getAmount();
        int payCurrency = shop.getPrice();

        if (countMatching(playerInv, shop.getItem()) < needItem) {
            player.sendMessage("§cУ тебе недостатньо " + shop.getItem() + ". Потрібно: " + needItem + ".");
            return;
        }

        if (countMatching(chestInv, CURRENCY) < payCurrency) {
            player.sendMessage("§cУ магазина закінчились гроші.");
            return;
        }

        Map<Integer, ItemStack> leftoverPlayer = playerInv.removeItem(new ItemStack(shop.getItem(), needItem));
        if (!leftoverPlayer.isEmpty()) {
            player.sendMessage("§cПомилка транзакції. Спробуй ще раз.");
            return;
        }

        Map<Integer, ItemStack> leftoverChest = chestInv.removeItem(new ItemStack(CURRENCY, payCurrency));
        if (!leftoverChest.isEmpty()) {
            playerInv.addItem(new ItemStack(shop.getItem(), needItem));
            player.sendMessage("§cПомилка транзакції. Спробуй ще раз.");
            return;
        }

        chestInv.addItem(new ItemStack(shop.getItem(), needItem));

        Map<Integer, ItemStack> leftoverGive = playerInv.addItem(new ItemStack(CURRENCY, payCurrency));
        for (ItemStack drop : leftoverGive.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        shopStorage.saveShops();

        player.sendMessage("§aПродано " + needItem + "x " + shop.getItem() + " за " + payCurrency + " мембран.");
    }

    private int countMatching(Inventory inventory, Material material) {
        int total = 0;
        for (ItemStack stack : inventory.getContents()) {
            if (stack != null && stack.getType() == material) {
                total += stack.getAmount();
            }
        }
        return total;
    }
}