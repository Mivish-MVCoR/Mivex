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
import ua.mvcor.mivex.config.BlockedItemsConfig;
import ua.mvcor.mivex.shop.Shop;
import ua.mvcor.mivex.shop.ShopManager;
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

    public CShopCommand(ShopManager shopManager, ShopStorage shopStorage) {
        this.shopManager = shopManager;
        this.shopStorage = shopStorage;
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

        if (args[0].equalsIgnoreCase("unbroken")) {
            handleUnbroken(player, args);
            return true;
        }

        if (args[0].equalsIgnoreCase("delete")) {
            handleDelete(player, args);
            return true;
        }

        if (args[0].equalsIgnoreCase("edit")) {
            handleEdit(player, args);
            return true;
        }

        if (args[0].equalsIgnoreCase("mylist")) {
            handleMyList(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {

            if (args.length == 1 || args[1].equalsIgnoreCase("allplayer")) {
                handleListAll(player);
                return true;
            }

            handleListPlayer(player, args[1]);
            return true;
        }

        if (args[0].equalsIgnoreCase("about")) {
            handleAbout(player);
            return true;
        }

        handleCreate(player, args);
        return true;
    }

    // -------------------------------------------------------------------
    // ДОПОМОГА
    // -------------------------------------------------------------------

    private void sendHelp(Player player) {
        player.sendMessage("§b=== Mivex Shop — команди ===");
        player.sendMessage("§e/cshop unbroken key[KEY] §f- відновити зламаний магазин");
        player.sendMessage("§e/cshop delete key[KEY] §f- видалити магазин, на який дивишся");
        player.sendMessage("§e/cshop mylist §f- твої магазини");
        player.sendMessage("§e/cshop edit KEY ITEM amXX prYY sell/buy §f- редагувати");
        player.sendMessage("§7/cshop list [player|allplayer] §7(адмін)");
        player.sendMessage("§7/cshop about");
        player.sendMessage("");
        player.sendMessage("§aСтворення магазину:");
        player.sendMessage("§f/cshop ITEM amXX prYY sell/buy key[TEXT]");
        player.sendMessage("§7Приклад: §f/cshop OAK_DOOR am32 pr2 sell key[MV]");
        player.sendMessage("§7(дивись на звичайну скриню в момент виконання команди)");
    }

    // -------------------------------------------------------------------
    // TAB-АВТОДОПОВНЕННЯ
    // -------------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        int position = args.length;
        String current = args.length > 0 ? args[args.length - 1] : "";
        boolean isEdit = args.length > 0 && args[0].equalsIgnoreCase("edit");
        boolean isDelete = args.length > 0 && args[0].equalsIgnoreCase("delete");
        boolean isUnbroken = args.length > 0 && args[0].equalsIgnoreCase("unbroken");

        if (position == 1) {
            List<String> options = new ArrayList<>();
            options.add("unbroken");
            options.add("delete");
            options.add("mylist");
            options.add("edit");
            options.add("list");
            options.add("about");
            options.addAll(getItemNames());
            return filter(options, current);
        }

        if (isDelete || isUnbroken) {
            if (position == 2) return filter(List.of("key["), current);
            return List.of();
        }

        if (isEdit) {
            if (position == 2) return List.of();
            if (position == 3) return filter(getItemNames(), current);
            if (position == 4) return filter(getAmountSuggestions(), current);
            if (position == 5) return filter(getPriceSuggestions(), current);
            if (position == 6) return filter(List.of("sell", "buy"), current);
            return List.of();
        }

        if (position == 2) return filter(getAmountSuggestions(), current);
        if (position == 3) return filter(getPriceSuggestions(), current);
        if (position == 4) return filter(List.of("sell", "buy"), current);
        if (position == 5) return filter(List.of("key["), current);
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

    private List<String> getAmountSuggestions() {
        List<String> options = new ArrayList<>();
        for (int i = 1; i <= TAB_SUGGESTION_LIMIT; i++) {
            options.add("am" + i);
        }
        return options;
    }

    private List<String> getPriceSuggestions() {
        List<String> options = new ArrayList<>();
        for (int i = 1; i <= TAB_SUGGESTION_LIMIT; i++) {
            options.add("pr" + i);
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
    // СТВОРЕННЯ
    // -------------------------------------------------------------------

    private void handleCreate(Player player, String[] args) {

        Material item = Material.matchMaterial(args[0]);

        if (item == null) {
            player.sendMessage("§cНевідомий предмет: " + args[0]);
            player.sendMessage("§7Підказка: /cshop ITEM amXX prYY sell/buy key[TEXT]");
            return;
        }

        if (BlockedItemsConfig.isBlocked(item)) {
            player.sendMessage("§cЦей предмет заборонено використовувати як товар.");
            return;
        }

        int amount = 0;
        int price = 0;
        String type = null;
        String key = null;

        try {
            for (int i = 1; i < args.length; i++) {
                String arg = args[i];

                if (arg.startsWith("am")) {
                    amount = Integer.parseInt(arg.substring(2));
                } else if (arg.startsWith("pr")) {
                    price = Integer.parseInt(arg.substring(2));
                } else if (arg.equalsIgnoreCase("sell")) {
                    type = "sell";
                } else if (arg.equalsIgnoreCase("buy")) {
                    type = "buy";
                } else if (arg.startsWith("key[") && arg.endsWith("]")) {
                    key = arg.substring(4, arg.length() - 1);
                }
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cКількість та ціна повинні бути числами.");
            player.sendMessage("§7Підказка: amXX (наприклад am32), prYY (наприклад pr2)");
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

        if (type == null) {
            player.sendMessage("§cВкажи тип магазину: sell або buy.");
            return;
        }

        if (key == null || key.isBlank()) {
            player.sendMessage("§cКлюч не може бути порожнім.");
            player.sendMessage("§7Підказка: key[MV], key[HOME], key[shop_1] тощо.");
            return;
        }

        if (!KEY_PATTERN.matcher(key).matches()) {
            player.sendMessage("§cКлюч може містити лише англійські літери, цифри, \"_\" та \"-\".");
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
                player.sendMessage("§cНа цьому місці знаходиться зламаний магазин.");
                player.sendMessage("§7Відновіть: /cshop unbroken key[...]");
                player.sendMessage("§7Або видаліть: /cshop delete key[...]");
            } else {
                player.sendMessage("§cНа цій скрині вже є магазин.");
            }
            return;
        }

        UUID id = UUID.randomUUID();
        UUID owner = player.getUniqueId();

        Shop shop = new Shop(
                id, owner, targetBlock.getLocation(),
                item, amount, price, type, key
        );

        shopManager.addShop(shop);
        shopStorage.saveShops();

        player.sendMessage("§aМагазин успішно створено!");

        if (isDoubleChest(targetBlock)) {
            player.sendMessage("§e⚠️ Ви створюєте магазин на подвійній скрині.");
            player.sendMessage("");
            player.sendMessage("§7Щоб друга половина також стала магазином,");
            player.sendMessage("§7подивіться на неї та повторіть цю саму команду.");
            player.sendMessage("");
            player.sendMessage("§aПідтримка автоматичних подвійних магазинів");
            player.sendMessage("§aз'явиться в майбутньому оновленні.");
        } else {
            player.sendMessage("§e⚠️ Ви створили магазин на одинарній скрині.");
            player.sendMessage("");
            player.sendMessage("§7Якщо пізніше приєднаєте другу скриню,");
            player.sendMessage("§7вона НЕ стане магазином автоматично.");
            player.sendMessage("");
            player.sendMessage("§7Щоб зробити магазин на другій половині,");
            player.sendMessage("§7подивіться на неї та повторіть цю саму команду.");
            player.sendMessage("");
            player.sendMessage("§c⚠️ Увага! Поки друга половина не є магазином,");
            player.sendMessage("§cїї можна буде відкрити без ключа.");
        }
    }

    // -------------------------------------------------------------------
    // РЕДАГУВАННЯ
    // -------------------------------------------------------------------

    private void handleEdit(Player player, String[] args) {

        if (args.length < 6) {
            player.sendMessage("§cВикористання: /cshop edit KEY ITEM amXX prYY sell/buy");
            return;
        }

        String providedKey = args[1];

        Material item = Material.matchMaterial(args[2]);

        if (item == null) {
            player.sendMessage("§cНевідомий предмет: " + args[2]);
            return;
        }

        if (BlockedItemsConfig.isBlocked(item)) {
            player.sendMessage("§cЦей предмет заборонено використовувати як товар.");
            return;
        }

        int amount = 0;
        int price = 0;
        String type = null;

        try {
            for (int i = 3; i < args.length; i++) {
                String arg = args[i];

                if (arg.startsWith("am")) {
                    amount = Integer.parseInt(arg.substring(2));
                } else if (arg.startsWith("pr")) {
                    price = Integer.parseInt(arg.substring(2));
                } else if (arg.equalsIgnoreCase("sell")) {
                    type = "sell";
                } else if (arg.equalsIgnoreCase("buy")) {
                    type = "buy";
                }
            }
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

        if (type == null) {
            player.sendMessage("§cВкажи тип магазину: sell або buy.");
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
            player.sendMessage("§cЦей магазин зламано. Спочатку виконай /cshop unbroken key[...]");
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
    // ВІДНОВЛЕННЯ ПІСЛЯ ЗЛАМУ
    // -------------------------------------------------------------------

    private void handleUnbroken(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage("§cВикористання: /cshop unbroken key[KEY]");
            return;
        }

        String keyArg = args[1];

        if (!(keyArg.startsWith("key[") && keyArg.endsWith("]"))) {
            player.sendMessage("§cВкажи ключ у форматі key[ТЕКСТ].");
            return;
        }

        String providedKey = keyArg.substring(4, keyArg.length() - 1);

        Block targetBlock = player.getTargetBlockExact(MAX_TARGET_DISTANCE);

        if (targetBlock == null || targetBlock.getType() != Material.CHEST) {
            player.sendMessage("§cПостав скриню на місце зламаного магазину і дивись на неї.");
            return;
        }

        Shop shop = shopManager.getShop(targetBlock.getLocation());

        if (shop == null) {
            player.sendMessage("§cНа цьому місці немає зламаного магазину.");
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

        shop.setBroken(false);
        shopStorage.saveShops();

        player.sendMessage("§aМагазин відновлено і знову працює!");
    }

    // -------------------------------------------------------------------
    // ВИДАЛЕННЯ
    // -------------------------------------------------------------------

    private void handleDelete(Player player, String[] args) {

        Block targetBlock = player.getTargetBlockExact(MAX_TARGET_DISTANCE);

        if (targetBlock == null || targetBlock.getType() != Material.CHEST) {
            player.sendMessage("§cПостав скриню на місце магазину (якщо він зламаний) і дивись на неї.");
            return;
        }

        Shop shop = shopManager.getShop(targetBlock.getLocation());

        if (shop == null) {
            player.sendMessage("§cЦя скриня не є магазином.");
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

            String keyArg = args[1];

            if (!(keyArg.startsWith("key[") && keyArg.endsWith("]"))) {
                player.sendMessage("§cВкажи ключ у форматі key[ТЕКСТ].");
                return;
            }

            String providedKey = keyArg.substring(4, keyArg.length() - 1);

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