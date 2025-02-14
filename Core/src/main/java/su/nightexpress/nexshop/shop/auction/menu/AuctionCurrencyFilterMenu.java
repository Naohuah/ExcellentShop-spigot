package su.nightexpress.nexshop.shop.auction.menu;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import su.nexmedia.engine.api.config.JYML;
import su.nexmedia.engine.api.menu.AutoPaged;
import su.nexmedia.engine.api.menu.MenuItemType;
import su.nexmedia.engine.api.menu.click.ClickHandler;
import su.nexmedia.engine.api.menu.click.ItemClick;
import su.nexmedia.engine.api.menu.impl.ConfigMenu;
import su.nexmedia.engine.api.menu.impl.MenuOptions;
import su.nexmedia.engine.api.menu.impl.MenuViewer;
import su.nexmedia.engine.utils.Colorizer;
import su.nexmedia.engine.utils.ItemUtil;
import su.nightexpress.nexshop.ExcellentShop;
import su.nightexpress.nexshop.api.currency.Currency;
import su.nightexpress.nexshop.shop.auction.AuctionManager;
import su.nightexpress.nexshop.shop.auction.Placeholders;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AuctionCurrencyFilterMenu extends ConfigMenu<ExcellentShop> implements AutoPaged<Currency> {

    private final AuctionManager auctionManager;
    private final int[]          objectSlots;
    private final String       itemName;
    private final List<String> itemLore;
    private final ItemStack selectedIcon;

    public AuctionCurrencyFilterMenu(@NotNull AuctionManager auctionManager, @NotNull JYML cfg) {
        super(auctionManager.plugin(), cfg);
        this.auctionManager = auctionManager;
        this.itemName = Colorizer.apply(cfg.getString("Items.Name", Placeholders.CURRENCY_NAME));
        this.itemLore = Colorizer.apply(cfg.getStringList("Items.Lore"));
        this.objectSlots = cfg.getIntArray("Items.Slots");
        this.selectedIcon = cfg.getItem("Selected");

        this.registerHandler(MenuItemType.class)
            .addClick(MenuItemType.PAGE_NEXT, ClickHandler.forNextPage(this))
            .addClick(MenuItemType.PAGE_PREVIOUS, ClickHandler.forPreviousPage(this))
            .addClick(MenuItemType.RETURN, (viewer, event) -> this.auctionManager.getMainMenu().openNextTick(viewer, 1))
            .addClick(MenuItemType.CONFIRMATION_DECLINE, (viewer, event) -> this.auctionManager.getMainMenu().openNextTick(viewer, 1))
            .addClick(MenuItemType.CONFIRMATION_ACCEPT, (viewer, event) -> this.auctionManager.getMainMenu().openNextTick(viewer, 1))
        ;

        this.load();
    }

    @Override
    public void onPrepare(@NotNull MenuViewer viewer, @NotNull MenuOptions options) {
        super.onPrepare(viewer, options);
        this.getItemsForPage(viewer).forEach(this::addItem);
    }

    @Override
    public int[] getObjectSlots() {
        return this.objectSlots;
    }

    @Override
    @NotNull
    public List<Currency> getObjects(@NotNull Player player) {
        return new ArrayList<>(this.auctionManager.getCurrencies());
    }

    @Override
    @NotNull
    public ItemStack getObjectStack(@NotNull Player player, @NotNull Currency currency) {
        Set<Currency> currencies = AuctionMainMenu.getCurrencies(player);
        boolean isSelected = currencies.contains(currency);

        ItemStack item = isSelected ? new ItemStack(this.selectedIcon) : currency.getIcon();
        ItemMeta meta = item.getItemMeta();
        if (meta != null && !isSelected) {
            meta.setDisplayName(this.itemName);
            meta.setLore(this.itemLore);
            item.setItemMeta(meta);
        }

        ItemUtil.replace(item, currency.replacePlaceholders());
        return item;
    }

    @Override
    @NotNull
    public ItemClick getObjectClick(@NotNull Currency currency) {
        return (viewer, event) -> {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) return;

            Player player = viewer.getPlayer();
            Set<Currency> categories = AuctionMainMenu.getCurrencies(player);
            if (categories.add(currency) || categories.remove(currency)) {
                this.openNextTick(viewer, viewer.getPage());
            }
        };
    }
}
