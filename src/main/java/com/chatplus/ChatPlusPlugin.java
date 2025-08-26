package com.chatplus;

import com.chatplus.commands.ChatPlusCommand;
import com.chatplus.config.ConfigManager;
import com.chatplus.integrations.PlaceholderAPIIntegration;
import com.chatplus.listeners.ChatListener;
import com.chatplus.managers.ChatToggleManager;
import com.chatplus.managers.CooldownManager;
import com.chatplus.managers.EmojiManager;
import com.chatplus.managers.FilterManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class ChatPlusPlugin extends JavaPlugin {
    
    private ConfigManager configManager;
    private CooldownManager cooldownManager;
    private EmojiManager emojiManager;
    private FilterManager filterManager;
    private ChatToggleManager chatToggleManager;
    private PlaceholderAPIIntegration placeholderIntegration;
    
    @Override
    public void onEnable() {
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.cooldownManager = new CooldownManager(this);
        this.emojiManager = new EmojiManager(this);
        this.filterManager = new FilterManager(this);
        this.chatToggleManager = new ChatToggleManager(this);
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        
        // Register commands
        ChatPlusCommand commandExecutor = new ChatPlusCommand(this);
        getCommand("chatplus").setExecutor(commandExecutor);
        getCommand("chatplus").setTabCompleter(commandExecutor);
        
        // Initialize PlaceholderAPI integration if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            this.placeholderIntegration = new PlaceholderAPIIntegration(this);
            this.placeholderIntegration.register();
            getLogger().info("PlaceholderAPI integration enabled!");
        }
        
        getLogger().info("ChatPlus has been enabled!");
        getLogger().info("Features: " + 
            (configManager.isCooldownEnabled() ? "Cooldown " : "") +
            (configManager.isEmojiEnabled() ? "Emojis " : "") +
            (configManager.isFilterEnabled() ? "Filters " : "") +
            "ChatToggle");
    }
    
    @Override
    public void onDisable() {
        if (cooldownManager != null) {
            cooldownManager.clearAllCooldowns();
        }
        
        if (placeholderIntegration != null) {
            placeholderIntegration.unregister();
        }
        
        getLogger().info("ChatPlus has been disabled!");
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
    
    public EmojiManager getEmojiManager() {
        return emojiManager;
    }
    
    public FilterManager getFilterManager() {
        return filterManager;
    }
    
    public ChatToggleManager getChatToggleManager() {
        return chatToggleManager;
    }
    
    public PlaceholderAPIIntegration getPlaceholderIntegration() {
        return placeholderIntegration;
    }
}