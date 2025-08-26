package com.chatplus.config;

import com.chatplus.ChatPlusPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    
    private final ChatPlusPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration cooldownsConfig;
    private FileConfiguration emojisConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration filtersConfig;
    
    public ConfigManager(ChatPlusPlugin plugin) {
        this.plugin = plugin;
        loadAllConfigs();
    }
    
    public void loadAllConfigs() {
        // Main config
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        
        // Load other configs
        this.cooldownsConfig = loadConfig("cooldowns.yml");
        this.emojisConfig = loadConfig("emojis.yml");
        this.messagesConfig = loadConfig("messages.yml");
        this.filtersConfig = loadConfig("filters.yml");
    }
    
    public void reloadAllConfigs() {
        loadAllConfigs();
    }
    
    private FileConfiguration loadConfig(String fileName) {
        File configFile = new File(plugin.getDataFolder(), fileName);
        
        if (!configFile.exists()) {
            plugin.saveResource(fileName, false);
        }
        
        return YamlConfiguration.loadConfiguration(configFile);
    }
    
    // Main config methods
    public boolean isCooldownEnabled() {
        return config.getBoolean("cooldown.enabled", true);
    }
    
    public boolean isEmojiEnabled() {
        return config.getBoolean("emoji.enabled", true);
    }
    
    public boolean isFilterEnabled() {
        return config.getBoolean("filters.enabled", false);
    }
    
    public boolean isAsyncChatEnabled() {
        return config.getBoolean("async-chat.enabled", true);
    }
    
    public String getPrefix() {
        return colorize(config.getString("general.prefix", "&7[&6ChatPlus&7] "));
    }
    
    // Cooldown config methods
    public int getDefaultCooldown() {
        return cooldownsConfig.getInt("default.time", 3);
    }
    
    public boolean isSoftThrottleEnabled() {
        return cooldownsConfig.getBoolean("soft-throttle.enabled", false);
    }
    
    public boolean isMovementResetEnabled() {
        return cooldownsConfig.getBoolean("movement-reset.enabled", false);
    }
    
    public boolean isDuplicateCheckEnabled() {
        return cooldownsConfig.getBoolean("duplicate-check.enabled", true);
    }
    
    public ConfigurationSection getGroupCooldowns() {
        return cooldownsConfig.getConfigurationSection("groups");
    }
    
    public ConfigurationSection getWorldCooldowns() {
        return cooldownsConfig.getConfigurationSection("worlds");
    }
    
    public ConfigurationSection getTimeBasedCooldowns() {
        return cooldownsConfig.getConfigurationSection("time-based");
    }
    
    public ConfigurationSection getLengthBasedCooldowns() {
        return cooldownsConfig.getConfigurationSection("length-based");
    }
    
    // Emoji config methods
    public Map<String, String> getEmojiMappings() {
        Map<String, String> mappings = new HashMap<>();
        ConfigurationSection section = emojisConfig.getConfigurationSection("mappings");
        
        if (section != null) {
            for (String key : section.getKeys(false)) {
                mappings.put(key, section.getString(key));
            }
        }
        
        return mappings;
    }
    
    public List<String> getEmojiReplacementOrder() {
        return emojisConfig.getStringList("replacement-order");
    }
    
    public boolean isWordBoundaryEnabled() {
        return emojisConfig.getBoolean("word-boundary", false);
    }
    
    public boolean isCommandReplacementEnabled() {
        return emojisConfig.getBoolean("replace-in-commands", false);
    }
    
    public ConfigurationSection getEmojiGroups() {
        return emojisConfig.getConfigurationSection("groups");
    }
    
    public ConfigurationSection getEmojiWorlds() {
        return emojisConfig.getConfigurationSection("worlds");
    }
    
    // Filter config methods
    public int getMaxRepeatedChars() {
        return filtersConfig.getInt("max-repeated-chars", 3);
    }
    
    public double getMaxCapsRatio() {
        return filtersConfig.getDouble("max-caps-ratio", 0.8);
    }
    
    public int getMinCapsLength() {
        return filtersConfig.getInt("min-caps-length", 5);
    }
    
    // Messages config methods
    public String getMessage(String key) {
        String message = messagesConfig.getString(key, "&cMessage not found: " + key);
        return colorize(message);
    }
    
    public String getCooldownMessage() {
        return getMessage("cooldown");
    }
    
    public String getDuplicateMessage() {
        return getMessage("duplicate");
    }
    
    public String getFilteredMessage() {
        return getMessage("filtered");
    }
    
    private String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}