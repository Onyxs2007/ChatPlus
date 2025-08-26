package com.chatplus.managers;

import com.chatplus.ChatPlusPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    
    private final ChatPlusPlugin plugin;
    private final Map<UUID, Long> cooldowns;
    private final Map<UUID, String> lastMessages;
    private final Map<UUID, Location> lastLocations;
    
    public CooldownManager(ChatPlusPlugin plugin) {
        this.plugin = plugin;
        this.cooldowns = new HashMap<>();
        this.lastMessages = new HashMap<>();
        this.lastLocations = new HashMap<>();
    }
    
    public boolean isOnCooldown(Player player) {
        if (!plugin.getConfigManager().isCooldownEnabled()) {
            return false;
        }
        
        if (player.hasPermission("chatplus.bypass.cooldown")) {
            return false;
        }
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        if (!cooldowns.containsKey(playerId)) {
            return false;
        }
        
        // Check movement reset
        if (plugin.getConfigManager().isMovementResetEnabled()) {
            Location lastLoc = lastLocations.get(playerId);
            if (lastLoc != null && !lastLoc.equals(player.getLocation())) {
                cooldowns.remove(playerId);
                lastLocations.put(playerId, player.getLocation());
                return false;
            }
        }
        
        long lastMessageTime = cooldowns.get(playerId);
        long cooldownTime = getEffectiveCooldown(player) * 1000L;
        
        return (currentTime - lastMessageTime) < cooldownTime;
    }
    
    public boolean isDuplicateMessage(Player player, String message) {
        if (!plugin.getConfigManager().isDuplicateCheckEnabled()) {
            return false;
        }
        
        if (player.hasPermission("chatplus.bypass.duplicate")) {
            return false;
        }
        
        UUID playerId = player.getUniqueId();
        String lastMessage = lastMessages.get(playerId);
        
        return message.equals(lastMessage);
    }
    
    public long getRemainingCooldown(Player player) {
        if (!plugin.getConfigManager().isCooldownEnabled()) {
            return 0;
        }
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        if (!cooldowns.containsKey(playerId)) {
            return 0;
        }
        
        long lastMessageTime = cooldowns.get(playerId);
        long cooldownTime = getEffectiveCooldown(player) * 1000L;
        long elapsed = currentTime - lastMessageTime;
        
        if (elapsed >= cooldownTime) {
            return 0;
        }
        
        return (cooldownTime - elapsed) / 1000L + 1;
    }
    
    public void setCooldown(Player player, String message) {
        if (!plugin.getConfigManager().isCooldownEnabled()) {
            return;
        }
        
        UUID playerId = player.getUniqueId();
        cooldowns.put(playerId, System.currentTimeMillis());
        lastMessages.put(playerId, message);
        lastLocations.put(playerId, player.getLocation());
    }
    
    public void removeCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        cooldowns.remove(playerId);
        lastMessages.remove(playerId);
        lastLocations.remove(playerId);
    }
    
    public void clearAllCooldowns() {
        cooldowns.clear();
        lastMessages.clear();
        lastLocations.clear();
    }
    
    public long getEffectiveCooldown(Player player) {
        long cooldown = plugin.getConfigManager().getDefaultCooldown();
        
        // Check group-based cooldowns
        String group = getPlayerGroup(player);
        if (group != null) {
            ConfigurationSection groupSection = plugin.getConfigManager().getGroupCooldowns();
            if (groupSection != null && groupSection.contains(group)) {
                cooldown = groupSection.getLong(group, cooldown);
            }
        }
        
        // Check world-based cooldowns
        String world = player.getWorld().getName();
        ConfigurationSection worldSection = plugin.getConfigManager().getWorldCooldowns();
        if (worldSection != null && worldSection.contains(world)) {
            cooldown = worldSection.getLong(world, cooldown);
        }
        
        // Check time-based cooldowns
        ConfigurationSection timeSection = plugin.getConfigManager().getTimeBasedCooldowns();
        if (timeSection != null) {
            LocalTime now = LocalTime.now();
            for (String timeRange : timeSection.getKeys(false)) {
                if (isTimeInRange(now, timeRange)) {
                    cooldown = timeSection.getLong(timeRange, cooldown);
                    break;
                }
            }
        }
        
        // Check length-based cooldowns
        // This would be applied when processing a message
        
        return cooldown;
    }
    
    public long getLengthBasedCooldown(Player player, String message) {
        long baseCooldown = getEffectiveCooldown(player);
        
        ConfigurationSection lengthSection = plugin.getConfigManager().getLengthBasedCooldowns();
        if (lengthSection != null) {
            int messageLength = message.length();
            
            for (String lengthRange : lengthSection.getKeys(false)) {
                if (isLengthInRange(messageLength, lengthRange)) {
                    return lengthSection.getLong(lengthRange, baseCooldown);
                }
            }
        }
        
        return baseCooldown;
    }
    
    public String getPlayerGroup(Player player) {
        // Try LuckPerms first
        if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            try {
                net.luckperms.api.LuckPerms luckPerms = net.luckperms.api.LuckPermsProvider.get();
                net.luckperms.api.model.user.User user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    return user.getPrimaryGroup();
                }
            } catch (Exception e) {
                // LuckPerms not available or error occurred
            }
        }
        
        // Try Vault
        if (plugin.getServer().getPluginManager().getPlugin("Vault") != null) {
            try {
                net.milkbowl.vault.permission.Permission permission = plugin.getServer().getServicesManager()
                    .getRegistration(net.milkbowl.vault.permission.Permission.class).getProvider();
                if (permission != null) {
                    return permission.getPrimaryGroup(player);
                }
            } catch (Exception e) {
                // Vault not available or error occurred
            }
        }
        
        return "default";
    }
    
    private boolean isTimeInRange(LocalTime time, String range) {
        try {
            String[] parts = range.split("-");
            if (parts.length == 2) {
                LocalTime start = LocalTime.parse(parts[0]);
                LocalTime end = LocalTime.parse(parts[1]);
                
                if (start.isBefore(end)) {
                    return !time.isBefore(start) && !time.isAfter(end);
                } else {
                    // Range crosses midnight
                    return !time.isBefore(start) || !time.isAfter(end);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid time range format: " + range);
        }
        return false;
    }
    
    private boolean isLengthInRange(int length, String range) {
        try {
            if (range.contains("-")) {
                String[] parts = range.split("-");
                if (parts.length == 2) {
                    int min = Integer.parseInt(parts[0]);
                    int max = Integer.parseInt(parts[1]);
                    return length >= min && length <= max;
                }
            } else if (range.startsWith(">")) {
                int min = Integer.parseInt(range.substring(1));
                return length > min;
            } else if (range.startsWith("<")) {
                int max = Integer.parseInt(range.substring(1));
                return length < max;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid length range format: " + range);
        }
        return false;
    }
    
    public String getCooldownMessage(Player player) {
        long remainingTime = getRemainingCooldown(player);
        return plugin.getConfigManager().getCooldownMessage()
                .replace("{time}", String.valueOf(remainingTime))
                .replace("{player}", player.getName());
    }
}