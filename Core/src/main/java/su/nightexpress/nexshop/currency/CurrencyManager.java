package su.nightexpress.nexshop.currency;

import me.xanium.gemseconomy.GemsEconomy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nexmedia.engine.api.config.JYML;
import su.nexmedia.engine.api.manager.AbstractManager;
import su.nexmedia.engine.integration.VaultHook;
import su.nexmedia.engine.utils.EngineUtils;
import su.nightexpress.nexshop.ExcellentShop;
import su.nightexpress.nexshop.api.currency.Currency;
import su.nightexpress.nexshop.api.currency.CurrencyHandler;
import su.nightexpress.nexshop.currency.handler.*;
import su.nightexpress.nexshop.currency.impl.CoinsEngineCurrency;
import su.nightexpress.nexshop.currency.impl.ConfigCurrency;
import su.nightexpress.nexshop.currency.impl.ItemCurrency;
import su.nightexpress.nexshop.hook.HookId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class CurrencyManager extends AbstractManager<ExcellentShop> {

    public static final String DIR_DEFAULT = "/currency/default/";
    public static final String DIR_CUSTOM  = "/currency/custom_item/";

    public static final String EXP           = "exp";
    public static final String VAULT         = "vault";

    private final Map<String, Currency> currencyMap;

    public CurrencyManager(@NotNull ExcellentShop plugin) {
        super(plugin);
        this.currencyMap = new HashMap<>();
    }

    @Override
    protected void onLoad() {
        this.plugin.getConfigManager().extractResources(DIR_DEFAULT);
        this.plugin.getConfigManager().extractResources(DIR_CUSTOM);

        this.registerCurrency(EXP, ExpPointsHandler::new);

        if (EngineUtils.hasVault() && VaultHook.hasEconomy()) {
            this.registerCurrency(VAULT, VaultEconomyHandler::new);
        }
        if (EngineUtils.hasPlugin(HookId.PLAYER_POINTS)) {
            this.registerCurrency(HookId.PLAYER_POINTS, PlayerPointsHandler::new);
            this.deprecatedCurrency(HookId.PLAYER_POINTS);
        }
        if (EngineUtils.hasPlugin(HookId.GAME_POINTS)) {
            this.registerCurrency(HookId.GAME_POINTS, GamePointsHandler::new);
            this.deprecatedCurrency(HookId.GAME_POINTS);
        }
        if (EngineUtils.hasPlugin(HookId.ELITEMOBS)) {
            this.registerCurrency(HookId.ELITEMOBS, EliteMobsHandler::new);
        }
        if (EngineUtils.hasPlugin(HookId.COINS_ENGINE)) {
            CoinsEngineCurrency.getCurrencies().forEach(this::registerCurrency);
        }
        if (EngineUtils.hasPlugin(HookId.GEMS_ECONOMY)) {
            for (me.xanium.gemseconomy.currency.Currency currency : GemsEconomy.getInstance().getCurrencyManager().getCurrencies()) {
                this.registerCurrency("gemseconomy_" + currency.getSingular(), () -> new GemsEconomyHandler(currency));
            }
        }

        for (JYML cfg : JYML.loadAll(plugin.getDataFolder() + DIR_CUSTOM, true)) {
            ItemCurrency currency = new su.nightexpress.nexshop.currency.impl.ItemCurrency(plugin, cfg);
            if (currency.load()) {
                this.registerCurrency(currency);
            }
        }
    }

    private void deprecatedCurrency(@NotNull String plugin) {
        this.plugin.warn("=".repeat(15));
        this.plugin.warn("Support for the '" + plugin + "' plugin is deprecated!");
        this.plugin.warn("Please, consider to switch to our new free custom currency " + HookId.COINS_ENGINE + " plugin instead.");
        this.plugin.warn("=".repeat(15));
    }

    @Override
    protected void onShutdown() {
        this.currencyMap.clear();
    }

    public boolean registerCurrency(@NotNull String id, @NotNull Supplier<CurrencyHandler> supplier) {
        JYML cfg = JYML.loadOrExtract(plugin, DIR_DEFAULT + id + ".yml");
        ConfigCurrency currency = new ConfigCurrency(plugin, cfg, supplier.get());
        if (!currency.load()) return false;

        return this.registerCurrency(currency);
    }

    public boolean registerCurrency(@NotNull Currency currency) {
        this.currencyMap.put(currency.getId(), currency);
        this.plugin.info("Registered currency: " + currency.getId());
        return true;
    }

    public boolean hasCurrency() {
        return !this.currencyMap.isEmpty();
    }

    @NotNull
    public Collection<Currency> getCurrencies() {
        return currencyMap.values();
    }

    @NotNull
    public Set<String> getCurrencyIds() {
        return this.currencyMap.keySet();
    }

    @Nullable
    public Currency getCurrency(@NotNull String id) {
        return this.currencyMap.get(id.toLowerCase());
    }

    @NotNull
    public Currency getAny() {
        return this.getCurrencies().stream().findFirst().orElseThrow();
    }
}
