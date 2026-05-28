package me.aglerr.donations.objects;

import me.aglerr.donations.managers.DependencyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class Product {

    private final String name;
    private final String displayName;
    private final double price;
    private final boolean discountable;
    private final List<String> command;

    private final String excellentEconomyCurrency;
    private final double excellentEconomyAmount;

    public Product(String name, String displayName, double price, boolean discountable, List<String> command, String excellentEconomyCurrency, double excellentEconomyAmount) {
        this.name = name;
        this.displayName = displayName;
        this.price = price;
        this.discountable = discountable;
        this.command = command;
        this.excellentEconomyCurrency = excellentEconomyCurrency;
        this.excellentEconomyAmount = excellentEconomyAmount;
    }

    public Product(String name, String displayName, double price, boolean discountable, List<String> command) {
        this(name, displayName, price, discountable, command, null, 0.0);
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getPrice() {
        if (this.discountable && me.aglerr.donations.ConfigValue.DISCOUNT > 0 && me.aglerr.donations.ConfigValue.DISCOUNT <= 100) {
            return price - (price * (me.aglerr.donations.ConfigValue.DISCOUNT / 100.0));
        }
        return price;
    }

    public double getOriginalPrice() {
        return price;
    }

    public List<String> getCommand() {
        return command;
    }

    public String getExcellentEconomyCurrency() {
        return excellentEconomyCurrency;
    }

    public double getExcellentEconomyAmount() {
        return excellentEconomyAmount;
    }

    public void execute(OfflinePlayer player, int amount) {
        if (this.command != null) {
            for (String cmd : this.command) {
                if (cmd == null || cmd.trim().isEmpty()) continue;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd
                        .replace("{player}", player.getName() == null ? "" : player.getName())
                        .replace("{amount}", String.valueOf(amount)));
            }
        }

        if (DependencyManager.EXCELLENT_ECONOMY_ENABLED && excellentEconomyCurrency != null && !excellentEconomyCurrency.isEmpty()) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("ExcellentEconomy");
            if (plugin != null) {
                try {
                    Object api = plugin.getClass().getMethod("getAPI").invoke(plugin);
                    if (api != null) {
                        java.lang.reflect.Method depositMethod = api.getClass().getMethod("depositAsync", java.util.UUID.class, String.class, double.class);
                        depositMethod.invoke(api, player.getUniqueId(), excellentEconomyCurrency, excellentEconomyAmount * amount);
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().warning("Failed to deposit ExcellentEconomy currency: " + e.getMessage());
                }
            }
        }
    }

    public void execute(OfflinePlayer player) {
        execute(player, 1);
    }
}
