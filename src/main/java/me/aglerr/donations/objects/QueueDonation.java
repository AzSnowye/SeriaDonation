package me.aglerr.donations.objects;

import com.muhammaddaffa.mdlib.utils.Executor;
import me.aglerr.donations.managers.DonationGoal;
import me.aglerr.donations.utils.Events;
import me.aglerr.donations.utils.Utils;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class QueueDonation {

    @NotNull private final OfflinePlayer player;
    @NotNull private final Product product;
    private final int amount;

    public QueueDonation(@NotNull OfflinePlayer player, @NotNull Product product, int amount) {
        this.player = player;
        this.product = product;
        this.amount = amount;
    }

    public QueueDonation(@NotNull OfflinePlayer player, @NotNull Product product) {
        this(player, product, 1);
    }

    @NotNull
    public OfflinePlayer getPlayer() {
        return player;
    }

    @NotNull
    public Product getProduct() {
        return product;
    }

    public int getAmount() {
        return amount;
    }

    public void announceDonation(){
        Executor.sync(() -> Events.playAllEvents(this.getPlayer()));
        Executor.sync(() -> DonationGoal.handleDonation(this));
        Executor.async(() -> {
            Utils.broadcastDonation(this);
            DiscordWebhook.sendDonation(this);
        });
    }
}
