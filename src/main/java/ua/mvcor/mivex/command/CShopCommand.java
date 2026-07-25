package ua.mvcor.mivex.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ua.mvcor.mivex.config.BlockedItemsConfig;
import ua.mvcor.mivex.shop.Shop;
import ua.mvcor.mivex.shop.ShopManager;
import ua.mvcor.mivex.storage.BrokenInventoryStorage;
import ua.mvcor.mivex.storage.ShopStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class CShopCommand implements CommandExecutor, TabCompleter {

    private static final int MAX_TARGET_DISTANCE = 3;
    private static final int MAX_AMOUNT = 3456;
    private static final int MAX_PRICE = 999999;
    private static final int TAB_SUGGESTION_LIMIT = 128;
    private static final Material CURRENCY = Material.PHANTOM_MEMBRANE;

    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");

    private final ShopManager shopManager;
    private final ShopStorage shopStorage;
    private final BrokenInventoryStorage inventoryStorage;

    public CShopCommand(ShopManager shopManager, ShopStorage shopStorage, BrokenInventoryStorage inventoryStorage) {
        this.shopManager = shopManager;
        this.shopStorage = shopStorage;
        this.inventoryStorage = inventoryStorage;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Цю команду може використовувати лише гравець.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            sendHelp(player);
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "sell":
                handleCreate(player, args, "sell");
                return true;
            case "buy":
                handleCreate(player, args, "buy");
                return true;
            case "edit":
                handleEdit(player, args);
                return true;
            case "delete":
                handleDelete(player, args);
                return true;
            case "unbroken":
                handleUnbroken(player, args);
                return true;
            case "mylist":
                handleMyList(player);
                return true;
            case "list":
                if (args.length >= 2) {
                    handleListPlayer(player, args[1]);
                } else {
                    handleListAll(player);
                }
                return true;
            case "about":
                handleAbout(player);
                return true;
            case "help":
                sendHelp(player);
                return true;
            default:
                player.sendMessage("§cНевідома дія: " + args[0]);
                player.sendMessage("§7Використай /cshop help для списку команд.");
                return true;
        }
    }

    // -------------------------------------------------------------------
    // ДОПОМОГА
    // -------------------------------------------------------------------

    private void sendHelp(Player player) {
        player.sendMessage("§b=== Mivex Shop — команди ===");
        player.sendMessage("§f/cshop sell key[KEY] ITEM AMOUNT PRICE §7- створити SELL-магазин");
        player.sendMessage("§f/cshop buy key[KEY] ITEM AMOUNT PRICE §7- створити BUY-магазин");
        player.sendMessage("§7Приклад: §f/cshop sell key[MV] OAK_DOOR 32 5");
        player.sendMessage("§7(дивись на звичайну скриню в момент виконання команди)");
        player.sendMessage("");
        player.sendMessage("§e/cshop edit key[KEY] sell/buy ITEM AMOUNT PRICE §f- редагувати");
        player.sendMessage("§e/cshop unbroken key[KEY] §f- відновити зламаний магазин");
        player.sendMessage("§e/cshop delete key[KEY] §f- видалити магазин (не BROKEN)");
        player.sendMessage("§e/cshop mylist §f- твої магазини");
        player.sendMessage("§7/cshop list [player] §7(адмін)");
        player.sendMessage("§7/cshop about");
    }

    // -------------------------------------------------------------------
    // TAB-АВТОДОПОВНЕННЯ
    // -------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        int position = args.length;
        String current = args.length > 0 ? args[args.length - 1] : "";
        String action = args.length > 0 ? args[0].toLowerCase() : "";

        if (position == 1) {
            List<String> options = List.of(
                    "about", "buy", "delete", "edit", "help", "list", "mylist", "sell", "unbroken"
            );
            return filter(options, current);
        }

        if (action.equals("delete") || action.equals("unbroken")) {
            if (position == 2) return filter(List.of("key["), current);
            return List.of();
        }

        if (action.equals("sell") || action.equals("buy")) {
            if (position == 2) return filter(List.of("key["), current);
            if (position == 3) return filter(getItemNames(), current);
            if (position == 4) return filter(getNumberSuggestions(), current);
            if (position == 5) return filter(getNumberSuggestions(), current);
            return List.of();
        }

        if (action.equals("edit")) {
            if (position == 2) return filter(List.of("key["), current);
            if (position == 3) return filter(List.of("sell", "buy"), current);
            if (position == 4) return filter(getItemNames(), current);
            if (position == 5) return filter(getNumberSuggestions(), current);
            if (position == 6) return filter(getNumberSuggestions(), current);
            return List.of();
        }

        return List.of();
    }

    private List<String> getItemNames() {
        List<String> names = new ArrayList<>();
        for (Material material : Material.values()) {
            if (!material.isItem()) continue;
            if (material == CURRENCY) continue;
            if (BlockedItemsConfig.isBlocked(material)) continue;
            names.add(material.name());
        }
        return names;
    }

    private List<String> getNumberSuggestions() {
        List<String> options = new ArrayList<>();
        for (int i = 1; i <= TAB_SUGGESTION_LIMIT; i++) {
            options.add(String.valueOf(i));
        }
        return options;
    }

    private List<String> filter(List<String> options, String current) {
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(current.toLowerCase())) {
                result.add(option);
            }
        }
        return result;
    }

    // -------------------------------------------------------------------
    // СТВОРЕННЯ: /cshop sell|buy key[KEY] ITEM AMOUNT PRICE
    // -------------------------------------------------------------------

    private void handleCreate(Player player, String[] args, String type) {

        if (args.length < 5) {
            player.sendMessage("§cВикористання: /cshop " + type + " key[KEY] ITEM AMOUNT PRICE");
            return;
        }

        String key = parseKeyArg(args[1]);
        if (key == null) {
            player.sendMessage("§cВкажи ключ у форматі key[ТЕКСТ].");
            return;
        }

        if (!KEY_PATTERN.matcher(key).matches()) {
            player.sendMessage("§cКлюч може містити лише англійські літери, цифри, \"_\" та \"-\".");
            return;
        }

        Material item = Material.matchMaterial(args[2]);
        if (item == null) {
            player.sendMessage("§cНевідомий предмет: " + args[2]);
            return;
        }

        if (BlockedItemsConfig.isBlocked(item)) {
            player.sendMessage("§cЦей предмет заборонено використовувати як товар.");
            return;
        }

        int amount;
        int price;
        try {
            amount = Integer.parseInt(args[3]);
            price = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cКількість та ціна повинні бути числами.");
            return;
        }

        if (amount <= 0 || amount > MAX_AMOUNT) {
            player.sendMessage("§cКількість повинна бути від 1 до " + MAX_AMOUNT + ".");
            return;
        }

        if (price <= 0 || price > MAX_PRICE) {
            player.sendMessage("§cЦіна повинна бути від 1 до " + MAX_PRICE + ".");
            return;
        }

        Block targetBlock = player.getTargetBlockExact(MAX_TARGET_DISTANCE);

        if (targetBlock == null) {
            player.sendMessage("§cТи не дивишся на жоден блок поруч.");
            return;
        }

        if (targetBlock.getType() != Material.CHEST) {
            player.sendMessage("§cМагазин можна створити тільки на звичайній скрині.");
            return;
        }

        BlockState state = targetBlock.getState();

        if (!(state instanceof Chest)) {
            player.sendMessage("§cЦя скриня недоступна.");
            return;
        }

        Shop existing = shopManager.getShop(targetBlock.getLocation());

        if (existing != null) {
            if (existing.isBroken()) {
                player.sendMessage("§cТут вже знаходиться BROKEN-магазин.");
                player.sendMessage("§7Спочатку відновіть його (/cshop unbroken key[...]) або видаліть.");
            } else {
                player.sendMessage("§cНа цій скрині вже є магазин.");
            }
            return;
        }

        UUID id = UUID.randomUUID();
        UUID owner = player.getUniqueId();

        Shop shop = new Shop(id, owner, targetBlock.getLocation(), item, amount, price, type, key);

        shopManager.addShop(shop);
        shopStorage.saveShops();

        player.sendMessage("§aМагазин успішно створено!");

        if (isDoubleChest(targetBlock)) {
            player.sendMessage("§e⚠️ Ви створюєте магазин на подвійній скрині.");
            player.sendMessage("§7Щоб друга половина також стала магазином, подивіться на неї та повторіть команду.");
        } else {
            player.sendMessage("§e⚠️ Одинарна скриня: приєднана пізніше друга половина НЕ стане магазином автоматично.");
        }
    }

    // -------------------------------------------------------------------
    // РЕДАГУВАННЯ: /cshop edit key[KEY] sell/buy ITEM AMOUNT PRICE
    // -------------------------------------------------------------------

    private void handleEdit(Player player, String[] args) {

        if (args.length < 6) {
            player.sendMessage("§cВикористання: /cshop edit key[KEY] sell/buy ITEM AMOUNT PRICE");
            return;
        }

        String providedKey = parseKeyArg(args[1]);
        if (providedKey == null) {
            player.sendMessage("§cВкажи ключ у форматі key[ТЕКСТ].");
            return;
        }

        String type;
        if (args[2].equalsIgnoreCase("sell")) {
            type = "sell";
        } else if (args[2].equalsIgnoreCase("buy")) {
            type = "buy";
        } else {
            player.sendMessage("§cВкажи тип: sell або buy.");
            return;
        }

        Material item = Material.matchMaterial(args[3]);
        if (item == null) {
            player.sendMessage("§cНевідомий предмет: " + args[3]);
            return;
        }

        if (BlockedItemsConfig.isBlocked(item)) {
            player.sendMessage("§cЦей предмет заборонено використовувати як товар.");
            return;
        }

        int amount;
        int price;
        try {
            amount = Integer.parseInt(args[4]);
            price = Integer.parseInt(args[5]);
        } catch (NumberFormatException e) {
            player.sendMessage("§cКількість та ціна повинні бути числами.");
            return;
        }

        if (amount <= 0 || amount > MAX_AMOUNT) {
            player.sendMessage("§cКількість повинна бути від 1 до " + MAX_AMOUNT + ".");
            return;
        }

        if (price <= 0 || price > MAX_PRICE) {
            player.sendMessage("§cЦіна повинна бути від 1 до " + MAX_PRICE + ".");
            return;
        }

        Block targetBlock = player.getTargetBlockExact(MAX_TARGET_DISTANCE);

        if (targetBlock == null) {
            player.sendMessage("§cДивись на скриню-магазин, яку хочеш редагувати.");
            return;
        }

        Shop shop = shopManager.getShop(targetBlock.getLocation());

        if (shop == null) {
            player.sendMessage("§cЦя скриня не є магазином.");
            return;
        }

        if (shop.isBroken()) {
            player.sendMessage("§cЦей магазин BROKEN. Спочатку виконай /cshop unbroken key[...]");
            return;
        }

        if (!shop.getKey().equals(providedKey)) {
            player.sendMessage("§cНевірний ключ. Магазин не змінено.");
            return;
        }

        shop.setItem(item);
        shop.setAmount(amount);
        shop.setPrice(price);
        shop.setType(type);

        shopStorage.saveShops();

        player.sendMessage("§aМагазин оновлено.");
    }

    // -------------------------------------------------------------------
    // ВІДНОВЛЕННЯ ПІСЛЯ ЗЛАМУ: /cshop unbroken key[KEY]
    // -------------------------------------------------------------------

    private void handleUnbroken(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage("§cВикористання: /cshop unbroken key[KEY]");
            return;
        }

        String providedKey = parseKeyArg(args[1]);
        if (providedKey == null) {
            player.sendMessage("§cВкажи ключ у форматі key[ТЕКСТ].");
            return;
        }

        Block targetBlock = player.getTargetBlockExact(MAX_TARGET_DISTANCE);

        if (targetBlock == null || targetBlock.getType() != Material.CHEST) {
            player.sendMessage("§cПостав нову скриню на місце зламаного магазину і дивись на неї.");
            return;
        }

        Shop shop = shopManager.getShop(targetBlock.getLocation());

        if (shop == null) {
            player.sendMessage("§cНа цьому місці немає магазину.");
            return;
        }

        if (!shop.isBroken()) {
            player.sendMessage("§cЦей магазин не зламаний, відновлювати нічого.");
            return;
        }

        if (!shop.getOwner().equals(player.getUniqueId()) && !player.hasPermission("mivex.admin")) {
            player.sendMessage("§cЦе не твій магазин.");
            return;
        }

        if (!shop.getKey().equals(providedKey)) {
            player.sendMessage("§cНевірний ключ.");
            return;
        }

        BlockState state = targetBlock.getState();
        if (!(state instanceof Chest)) {
            player.sendMessage("§cЦя скриня недоступна.");
            return;
        }

        Chest chest = (Chest) state;
        Inventory chestInv = chest.getInventory();

        if (!isInventoryEmpty(chestInv)) {
            player.sendMessage("§cСкриня повинна бути порожньою для відновлення.");
            return;
        }

        if (!inventoryStorage.exists(shop.getId())) {
            player.sendMessage("§cФайл інвентарю не знайдено. Зверніться до адміністратора.");
            return;
        }

        ItemStack[] contents = inventoryStorage.loadInventory(shop.getId());

        if (contents == null) {
            player.sendMessage("§cФайл інвентарю пошкоджено. Відновлення неможливе.");
            return;
        }

        chestInv.setContents(contents);

        // Видаляємо файл лише ПІСЛЯ того, як предмети вже реально в скрині.
        inventoryStorage.deleteInventory(shop.getId());

        shop.setBroken(false);
        shopStorage.saveShops();

        player.sendMessage("§aМагазин відновлено! Товари повернуто у скриню.");
    }

    private boolean isInventoryEmpty(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }
        return true;
    }

    // -------------------------------------------------------------------
    // ВИДАЛЕННЯ: /cshop delete key[KEY]
    // -------------------------------------------------------------------

    private void handleDelete(Player player, String[] args) {

        Block targetBlock = player.getTargetBlockExact(MAX_TARGET_DISTANCE);

        if (targetBlock == null || targetBlock.getType() != Material.CHEST) {
            player.sendMessage("§cДивись на скриню-магазин, яку хочеш видалити.");
            return;
        }

        Shop shop = shopManager.getShop(targetBlock.getLocation());

        if (shop == null) {
            player.sendMessage("§cЦя скриня не є магазином.");
            return;
        }

        if (shop.isBroken()) {
            player.sendMessage("§cЦей магазин BROKEN. Спочатку виконай /cshop unbroken key[...]");
            player.sendMessage("§7Після відновлення видалення буде доступне.");
            return;
        }

        boolean isAdmin = player.hasPermission("mivex.admin") || player.hasPermission("mivex.*");
        boolean isOwner = shop.getOwner().equals(player.getUniqueId());

        if (!isOwner && !isAdmin) {
            player.sendMessage("§cЦе не твій магазин.");
            return;
        }

        if (!isAdmin) {
            if (args.length < 2) {
                player.sendMessage("§cВикористання: /cshop delete key[KEY]");
                return;
            }

            String providedKey = parseKeyArg(args[1]);
            if (providedKey == null) {
                player.sendMessage("§cВкажи ключ у форматі key[ТЕКСТ].");
                return;
            }

            if (!shop.getKey().equals(providedKey)) {
                player.sendMessage("§cНевірний ключ.");
                return;
            }
        }

        shopManager.removeShop(shop.getLocation());
        shopStorage.saveShops();

        player.sendMessage("§aМагазин видалено назавжди. На цьому місці тепер можна створити новий.");
    }

    // -------------------------------------------------------------------
    // СПИСКИ
    // -------------------------------------------------------------------

    private void handleMyList(Player player) {

        List<Shop> ownShops = new ArrayList<>();

        for (Shop shop : shopManager.getShops()) {
            if (shop.getOwner().equals(player.getUniqueId())) {
                ownShops.add(shop);
            }
        }

        player.sendMessage("§b=== Твої магазини ===");

        if (ownShops.isEmpty()) {
            player.sendMessage("§7У тебе немає магазинів.");
            return;
        }

        for (Shop shop : ownShops) {
            printOwnShopDetails(player, shop);
        }

        player.sendMessage("§bВсього магазинів: " + ownShops.size());
    }

    private void handleListAll(Player player) {

        if (!player.hasPermission("mivex.admin")) {
            player.sendMessage("§cУ тебе немає прав на цю команду.");
            return;
        }

        List<Shop> allShops = shopManager.getShops();

        player.sendMessage("§b========== Всі магазини ==========");

        if (allShops.isEmpty()) {
            player.sendMessage("§7На сервері немає жодного магазину.");
            return;
        }

        int activeCount = 0;
        int brokenCount = 0;

        for (Shop shop : allShops) {
            printAdminShopDetails(player, shop);
            if (shop.isBroken()) brokenCount++; else activeCount++;
        }

        player.sendMessage("§bВсього магазинів: " + allShops.size());
        player.sendMessage("§aACTIVE: " + activeCount);
        player.sendMessage("§cBROKEN: " + brokenCount);
    }

    private void handleListPlayer(Player player, String targetName) {

        if (!player.hasPermission("mivex.admin")) {
            player.sendMessage("§cУ тебе немає прав на цю команду.");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetId = target.getUniqueId();

        List<Shop> targetShops = new ArrayList<>();
        for (Shop shop : shopManager.getShops()) {
            if (shop.getOwner().equals(targetId)) {
                targetShops.add(shop);
            }
        }

        player.sendMessage("§b========== Магазини " + targetName + " ==========");

        if (targetShops.isEmpty()) {
            player.sendMessage("§7У цього гравця немає магазинів.");
            return;
        }

        int activeCount = 0;
        int brokenCount = 0;

        for (Shop shop : targetShops) {
            printAdminShopDetails(player, shop);
            if (shop.isBroken()) brokenCount++; else activeCount++;
        }

        player.sendMessage("§bВсього магазинів: " + targetShops.size());
        player.sendMessage("§aACTIVE: " + activeCount);
        player.sendMessage("§cBROKEN: " + brokenCount);
    }

    private void handleAbout(Player player) {
        player.sendMessage("§bMivex — MV Shop");
        player.sendMessage("§7Originally designed for Projekt 5.2");
        player.sendMessage("§7Created by MV CoR (Mivish)");
        player.sendMessage("§dThanks for using Mivex \u2764");
    }

    // -------------------------------------------------------------------
    // ДОПОМІЖНІ
    // -------------------------------------------------------------------

    private String parseKeyArg(String raw) {
        if (raw == null || !raw.startsWith("key[") || !raw.endsWith("]")) {
            return null;
        }
        return raw.substring(4, raw.length() - 1);
    }

    private void printOwnShopDetails(Player player, Shop shop) {
        String status = shop.isBroken() ? "§c🔴 BROKEN" : "§a🟢 ACTIVE";

        player.sendMessage(status);
        player.sendMessage("§fНазва: §7" + formatMaterialName(shop.getItem()));
        player.sendMessage("§fТовар: §7" + shop.getItem() + " x" + shop.getAmount());
        player.sendMessage("§fЦіна: §7" + shop.getPrice());
        player.sendMessage("§fКлюч доступу: §e" + shop.getKey());
        player.sendMessage("§fКоординати: §7" + formatCoords(shop.getLocation()));
        player.sendMessage("§7--------------------");
    }

    private void printAdminShopDetails(Player player, Shop shop) {
        OfflinePlayer owner = Bukkit.getOfflinePlayer(shop.getOwner());
        String ownerName = owner.getName() != null ? owner.getName() : "Невідомий";
        String status = shop.isBroken() ? "§c🔴 BROKEN" : "§a🟢 ACTIVE";

        player.sendMessage(status);
        player.sendMessage("§fНазва: §7" + formatMaterialName(shop.getItem()));
        player.sendMessage("§fТовар: §7" + shop.getItem() + " x" + shop.getAmount());
        player.sendMessage("§fЦіна: §7" + shop.getPrice());
        player.sendMessage("§fВласник: §7" + ownerName);
        player.sendMessage("§fКоординати: §7" + formatCoords(shop.getLocation()));
        player.sendMessage("§7--------------------");
    }

    private String formatCoords(Location loc) {
        if (loc == null || loc.getWorld() == null) return "невідомо";
        return loc.getWorld().getName() + " " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }

    private boolean isDoubleChest(Block block) {
        Block[] neighbors = {
                block.getRelative(1, 0, 0),
                block.getRelative(-1, 0, 0),
                block.getRelative(0, 0, 1),
                block.getRelative(0, 0, -1)
        };

        for (Block neighbor : neighbors) {
            if (neighbor.getType() == Material.CHEST) {
                return true;
            }
        }
        return false;
    }

    private String formatMaterialName(Material material) {
        String[] words = material.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }
}