package me.aglerr.donations.utils.color;

import net.md_5.bungee.api.ChatColor;
import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorParser {

    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<GRADIENT:([0-9a-fA-F]{6})>(.*?)</GRADIENT:([0-9a-fA-F]{6})>");
    private static final Pattern RAINBOW_PATTERN = Pattern.compile("<RAINBOW(?:([0-9]+))?>(.*?)</RAINBOW>");
    private static final Pattern HEX_PATTERN_1 = Pattern.compile("&#([a-fA-F0-9]{6})");
    private static final Pattern HEX_PATTERN_2 = Pattern.compile("<#([a-fA-F0-9]{6})>");

    public static String color(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        message = processGradient(message);
        message = processRainbow(message);
        message = processHex(message);
        
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static String processGradient(String message) {
        Matcher matcher = GRADIENT_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String startHex = matcher.group(1);
            String text = matcher.group(2);
            String endHex = matcher.group(3);
            
            Color start = Color.decode("#" + startHex);
            Color end = Color.decode("#" + endHex);
            
            matcher.appendReplacement(buffer, createGradient(text, start, end));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String processRainbow(String message) {
        Matcher matcher = RAINBOW_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String phaseStr = matcher.group(1);
            String text = matcher.group(2);
            int phase = phaseStr != null ? Integer.parseInt(phaseStr) : 1;
            
            matcher.appendReplacement(buffer, createRainbow(text, phase));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String processHex(String message) {
        Matcher matcher1 = HEX_PATTERN_1.matcher(message);
        StringBuffer buffer1 = new StringBuffer();
        while (matcher1.find()) {
            try {
                ChatColor color = ChatColor.of("#" + matcher1.group(1));
                matcher1.appendReplacement(buffer1, color.toString());
            } catch (NoSuchMethodError e) {
                // Pre 1.16 fallback
                matcher1.appendReplacement(buffer1, "");
            }
        }
        matcher1.appendTail(buffer1);
        
        Matcher matcher2 = HEX_PATTERN_2.matcher(buffer1.toString());
        StringBuffer buffer2 = new StringBuffer();
        while (matcher2.find()) {
            try {
                ChatColor color = ChatColor.of("#" + matcher2.group(1));
                matcher2.appendReplacement(buffer2, color.toString());
            } catch (NoSuchMethodError e) {
                matcher2.appendReplacement(buffer2, "");
            }
        }
        matcher2.appendTail(buffer2);
        
        return buffer2.toString();
    }

    private static String createGradient(String text, Color start, Color end) {
        StringBuilder builder = new StringBuilder();
        int length = text.length();
        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (float) (length - 1 == 0 ? 1 : length - 1);
            int red = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
            int green = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
            int blue = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));
            
            Color stepColor = new Color(red, green, blue);
            try {
                builder.append(ChatColor.of(stepColor)).append(text.charAt(i));
            } catch (NoSuchMethodError e) {
                // Pre 1.16 fallback
                builder.append(text.charAt(i));
            }
        }
        return builder.toString();
    }

    private static String createRainbow(String text, int phase) {
        StringBuilder builder = new StringBuilder();
        int length = text.length();
        for (int i = 0; i < length; i++) {
            float hue = (float) (i + phase) / (float) length;
            int rgb = Color.HSBtoRGB(hue, 1.0f, 1.0f);
            Color stepColor = new Color(rgb);
            try {
                builder.append(ChatColor.of(stepColor)).append(text.charAt(i));
            } catch (NoSuchMethodError e) {
                // Pre 1.16 fallback
                builder.append(text.charAt(i));
            }
        }
        return builder.toString();
    }
}
