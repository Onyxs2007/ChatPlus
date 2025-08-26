package com.chatplus.managers;

import com.chatplus.ChatPlusPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class EmojiManager {
    
    private final ChatPlusPlugin plugin;
    private Pattern wordBoundaryPattern;
    
    public EmojiManager(ChatPlusPlugin plugin) {
        this.plugin = plugin;
        updateWordBoundaryPattern();
    }
    
    public String replaceEmojis(Player player, String message) {
        if (!plugin.getConfigManager().isEmojiEnabled()) {
            return message;
        }
        
        if (!areEmojisEnabledForPlayer(player)) {
            return message;
        }
        
        // Don't replace in commands unless configured to do so
        if (message.startsWith("/") && !plugin.getConfigManager().isCommandReplacementEnabled()) {
            return message;
        }
        
        Map<String, String> emojiMappings = plugin.getConfigManager().getEmojiMappings();
        if (emojiMappings.isEmpty()) {
            return message;
        }
        
        String processedMessage = message;
        
        // Use replacement order if specified
        List<String> replacementOrder = plugin.getConfigManager().getEmojiReplacementOrder();
        if (!replacementOrder.isEmpty()) {
            for (String code : replacementOrder) {
                if (emojiMappings.containsKey(code)) {
                    processedMessage = replaceEmoji(processedMessage, code, emojiMappings.get(code));
                }
            }
        } else {
            // Replace all emojis
            for (Map.Entry<String, String> entry : emojiMappings.entrySet()) {
                processedMessage = replaceEmoji(processedMessage, entry.getKey(), entry.getValue());
            }
        }
        
        return processedMessage;
    }
    
    private String replaceEmoji(String message, String code, String emoji) {
        if (plugin.getConfigManager().isWordBoundaryEnabled()) {
            // Use word boundary regex for more precise replacement
            String regex = "\\b" + Pattern.quote(code) + "\\b";
            return message.replaceAll(regex, emoji);
        } else {
            // Simple string replacement
            return message.replace(code, emoji);
        }
    }
    
    public boolean areEmojisEnabledForPlayer(Player player) {
        if (!player.hasPermission("chatplus.emojis.use")) {
            return false;
        }
        
        // Check group-based emoji settings
        String group = plugin.getCooldownManager().getPlayerGroup(player);
        if (group != null) {
            ConfigurationSection groupSection = plugin.getConfigManager().getEmojiGroups();
            if (groupSection != null && groupSection.contains(group)) {
                return groupSection.getBoolean(group, true);
            }
        }
        
        // Check world-based emoji settings
        String world = player.getWorld().getName();
        ConfigurationSection worldSection = plugin.getConfigManager().getEmojiWorlds();
        if (worldSection != null && worldSection.contains(world)) {
            return worldSection.getBoolean(world, true);
        }
        
        return true;
    }
    
    public boolean containsEmojiCode(String message) {
        if (!plugin.getConfigManager().isEmojiEnabled()) {
            return false;
        }
        
        Map<String, String> emojiMappings = plugin.getConfigManager().getEmojiMappings();
        
        for (String code : emojiMappings.keySet()) {
            if (message.contains(code)) {
                return true;
            }
        }
        
        return false;
    }
    
    private void updateWordBoundaryPattern() {
        if (plugin.getConfigManager().isWordBoundaryEnabled()) {
            // Pre-compile pattern for better performance
            this.wordBoundaryPattern = Pattern.compile("\\b");
        }
    }
}