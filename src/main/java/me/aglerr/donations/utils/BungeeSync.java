package me.aglerr.donations.utils;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.aglerr.donations.DonationPlugin;
import me.aglerr.donations.objects.Product;
import me.aglerr.donations.objects.QueueDonation;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class BungeeSync implements PluginMessageListener {

    private final DonationPlugin plugin;
    private static final String CHANNEL = "BungeeCord";
    private static final String SUB_CHANNEL = "SeriaDonationSync";

    public BungeeSync(DonationPlugin plugin) {
        this.plugin = plugin;
    }

    public void sendBroadcast(QueueDonation donation) {
        Player sender = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (sender == null) {
            return; // Needs at least 1 online player to send PluginMessage
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF(SUB_CHANNEL);

        ByteArrayDataOutput msg = ByteStreams.newDataOutput();
        msg.writeUTF(donation.getPlayer().getUniqueId().toString());
        msg.writeUTF(donation.getPlayer().getName() != null ? donation.getPlayer().getName() : "");
        msg.writeUTF(donation.getProduct().getName());
        msg.writeInt(donation.getAmount());

        byte[] payload = msg.toByteArray();
        out.writeShort(payload.length);
        out.write(payload);

        sender.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (!channel.equals(CHANNEL)) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals(SUB_CHANNEL)) {
            short len = in.readShort();
            byte[] msgbytes = new byte[len];
            in.readFully(msgbytes);

            ByteArrayDataInput msgIn = ByteStreams.newDataInput(msgbytes);
            String uuidStr = msgIn.readUTF();
            String nameStr = msgIn.readUTF();
            String productName = msgIn.readUTF();
            int amount = msgIn.readInt();

            OfflinePlayer targetPlayer;
            try {
                UUID uuid = UUID.fromString(uuidStr);
                targetPlayer = Bukkit.getOfflinePlayer(uuid);
            } catch (Exception e) {
                targetPlayer = Bukkit.getOfflinePlayer(nameStr);
            }

            Product product = plugin.getProductManager().getProduct(productName);
            if (product == null) return;

            QueueDonation donation = new QueueDonation(targetPlayer, product, amount);
            
            // Execute the visual broadcast directly without processing commands/economy again
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                Utils.broadcastDonation(donation);
            });
        }
    }
}
