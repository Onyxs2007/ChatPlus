package com.chatplus.managers;

import com.chatplus.ChatPlusPlugin;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class ChatToggleManager {
    
    private final ChatPlusPlugin plugin;
    private final Set<String> mutedWorlds = new HashSet<>();
    private boolean globalChatMuted = false;
    
    public ChatToggleManager(ChatPlusPlugin plugin) {
        this.plugin = plugin;
    }
    
    public boolean isChatMuted() {
        return globalChatMuted;
    }
    
    public void setChatMuted(boolean muted) {
        this.globalChatMuted = muted;
    }
    
    public boolean isWorldMuted(String world) {
        return mutedWorlds.contains(world.toLowerCase());
    }
    
    public void setWorldMuted(String world, boolean muted) {
        if (muted) {
            mutedWorlds.add(world.toLowerCase());
        } else {
            mutedWorlds.remove(world.toLowerCase());
        }
    }
    
    public boolean canPlayerChat(Player player) {
        // Check bypass permission first
        if (player.hasPermission("chatplus.bypass.toggle")) {
            return true;
        }
        
        // Check global mute
        if (globalChatMuted) {
            return false;
        }
        
        // Check world-specific mute
        String worldName = player.getWorld().getName();
        return !isWorldMuted(worldName);
    }
    
    public String getToggleMessage(boolean enabled) {
        if (enabled) {
            return plugin.getConfigManager().getMessage("chat-toggle-enabled");
        } else {
            return plugin.getConfigManager().getMessage("chat-toggle-disabled");
        }
    }
    
    public String getMutedMessage() {
        return plugin.getConfigManager().getMessage("chat-muted");
    }
}