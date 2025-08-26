package com.chatplus.integrations;

import com.chatplus.ChatPlusPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class PlaceholderAPIIntegration extends PlaceholderExpansion {
    
    private final ChatPlusPlugin plugin;
    
    public PlaceholderAPIIntegration(ChatPlusPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getIdentifier() {
        return "chatplus";
    }
    
    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }
    
    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) {
            return "";
        }
        
        switch (params.toLowerCase()) {
            case "cooldown_remaining":
                return String.valueOf(plugin.getCooldownManager().getRemainingCooldown(player));
                
            case "cooldown_active":
                return String.valueOf(plugin.getCooldownManager().isOnCooldown(player));
                
            case "emojis_enabled":
                return String.valueOf(plugin.getEmojiManager().areEmojisEnabledForPlayer(player));
                
            default:
                return null;
        }
    }
}