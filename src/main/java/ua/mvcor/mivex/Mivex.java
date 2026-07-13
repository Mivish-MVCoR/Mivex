package ua.mvcor.mivex;

import org.bukkit.plugin.java.JavaPlugin;
import ua.mvcor.mivex.command.CShopCommand;
import ua.mvcor.mivex.command.MivexCommand;
import ua.mvcor.mivex.config.BlockedItemsConfig;
import ua.mvcor.mivex.listener.ShopListener;
import ua.mvcor.mivex.shop.ShopManager;
import ua.mvcor.mivex.storage.ShopStorage;

public class Mivex extends JavaPlugin {

    private ShopManager shopManager;
    private ShopStorage shopStorage;

    @Override
    public void onEnable() {

        saveDefaultConfig();
        BlockedItemsConfig.load(getConfig());

        shopManager = new ShopManager();
        shopStorage = new ShopStorage(this, shopManager);

        shopStorage.loadShops();

        CShopCommand cShopCommand = new CShopCommand(shopManager, shopStorage);
        getCommand("cshop").setExecutor(cShopCommand);
        getCommand("cshop").setTabCompleter(cShopCommand);
        getCommand("mivex").setExecutor(new MivexCommand());

        getServer().getPluginManager().registerEvents(new ShopListener(shopManager, shopStorage), this);

        getLogger().info("Mivex enabled!");
    }

    @Override
    public void onDisable() {

        shopStorage.saveShops();

        getLogger().info("Mivex disabled!");
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public ShopStorage getShopStorage() {
        return shopStorage;
    }
}