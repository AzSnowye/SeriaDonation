package me.aglerr.donations.objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.aglerr.donations.DonationPlugin;
import me.aglerr.donations.managers.DonationGoal;
import org.bukkit.configuration.ConfigurationSection;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;

public class DiscordWebhook {

    public static void sendDonation(QueueDonation donation) {
        ConfigurationSection config = DonationPlugin.DEFAULT_CONFIG.getConfig().getConfigurationSection("discordWebhook");
        if (config == null || !config.getBoolean("enabled", false)) {
            return;
        }

        String urlString = config.getString("url");
        if (urlString == null || urlString.isEmpty() || urlString.contains("YOUR_WEBHOOK")) {
            return;
        }

        try {
            JsonObject json = new JsonObject();
            JsonArray embeds = new JsonArray();
            JsonObject embed = new JsonObject();

            ConfigurationSection embedConfig = config.getConfigurationSection("embed");
            if (embedConfig != null) {
                if (embedConfig.contains("title")) {
                    embed.addProperty("title", parse(embedConfig.getString("title"), donation));
                }
                if (embedConfig.contains("description")) {
                    embed.addProperty("description", parse(embedConfig.getString("description"), donation));
                }
                if (embedConfig.contains("color")) {
                    String colorString = embedConfig.getString("color", "#ffffff").replace("#", "");
                    try {
                        embed.addProperty("color", Integer.parseInt(colorString, 16));
                    } catch (Exception ignored) {}
                }
                if (embedConfig.contains("thumbnail")) {
                    JsonObject thumbnail = new JsonObject();
                    thumbnail.addProperty("url", parse(embedConfig.getString("thumbnail"), donation));
                    embed.add("thumbnail", thumbnail);
                }

                if (embedConfig.contains("fields")) {
                    JsonArray fields = new JsonArray();
                    for (Map<?, ?> map : embedConfig.getMapList("fields")) {
                        JsonObject field = new JsonObject();
                        field.addProperty("name", parse((String) map.get("name"), donation));
                        field.addProperty("value", parse((String) map.get("value"), donation));
                        field.addProperty("inline", (Boolean) map.get("inline"));
                        fields.add(field);
                    }
                    embed.add("fields", fields);
                }
            }

            embeds.add(embed);
            json.add("embeds", embeds);

            URL url = new URL(urlString);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.addRequestProperty("Content-Type", "application/json");
            connection.addRequestProperty("User-Agent", "Java-DiscordWebhook");
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            try (OutputStream stream = connection.getOutputStream()) {
                stream.write(json.toString().getBytes());
                stream.flush();
            }

            connection.getInputStream().close();
            connection.disconnect();

        } catch (Exception e) {
            org.bukkit.Bukkit.getLogger().warning("Failed to send Discord Webhook: " + e.getMessage());
        }
    }

    private static String parse(String string, QueueDonation donation) {
        if (string == null) return "";
        Product product = donation.getProduct();
        int amount = donation.getAmount();
        double totalPrice = product.getPrice() * amount;
        double totalOriginalPrice = product.getOriginalPrice() * amount;

        String formattedPrice = java.text.NumberFormat.getInstance(new java.util.Locale("id", "ID")).format(totalPrice);
        String formattedOriginalPrice = java.text.NumberFormat.getInstance(new java.util.Locale("id", "ID")).format(totalOriginalPrice);
        
        String result = string
                .replace("{product_name}", product.getName())
                .replace("{product_displayname}", product.getDisplayName())
                .replace("{product_price}", formattedPrice)
                .replace("{product_original_price}", formattedOriginalPrice)
                .replace("{amount}", String.valueOf(amount))
                .replace("{player}", donation.getPlayer().getName() == null ? "" : donation.getPlayer().getName())
                .replace("{goal_progress_bar}", DonationGoal.getProgressBar())
                .replace("{goal_percentage}", DonationGoal.getDonationPercentage())
                .replace("{goal_donation_goal}", DonationGoal.getDonationGoal())
                .replace("{goal_current_donation}", DonationGoal.getCurrentDonation());

        // Strip Hex Colors (&#RRGGBB)
        result = result.replaceAll("&#[a-fA-F0-9]{6}", "");
        // Strip Standard Bukkit Colors (&a, &b, &1, &l, dll)
        result = result.replaceAll("&[0-9a-fA-Fk-oK-OrR]", "");
        // Strip translated section signs (§a, §b) just in case
        result = org.bukkit.ChatColor.stripColor(result);

        return result;
    }
}
