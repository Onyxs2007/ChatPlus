package com.chatplus.listeners;

import com.chatplus.ChatPlusPlugin;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatListener implements Listener {
    
    private final ChatPlusPlugin plugin;
    
    public ChatListener(ChatPlusPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }
        
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // Check if chat is toggled off
        if (!plugin.getChatToggleManager().canPlayerChat(player)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getChatToggleManager().getMutedMessage());
            return;
        }
        
        // Check for filters first
        if (plugin.getFilterManager().shouldFilterMessage(player, message)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getFilterManager().getFilteredMessage(player));
            return;
        }
        
        // Check for duplicate messages
        if (plugin.getCooldownManager().isDuplicateMessage(player, message)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getDuplicateMessage()
                .replace("{player}", player.getName()));
            return;
        }
        
        // Check cooldown
        if (plugin.getCooldownManager().isOnCooldown(player)) {
            if (plugin.getConfigManager().isSoftThrottleEnabled()) {
                // Soft throttle: cancel event but don't notify player
                event.setCancelled(true);
                plugin.getLogger().info("Soft-throttled message from " + player.getName() + ": " + message);
                return;
            } else {
                // Hard throttle: cancel and notify player
                event.setCancelled(true);
                player.sendMessage(plugin.getCooldownManager().getCooldownMessage(player));
                return;
            }
        }
        
        // Process message through filters (profanity replacement, clickable actions)
        String processedMessage = plugin.getFilterManager().processMessage(player, message);
        
        // Replace emojis
        processedMessage = plugin.getEmojiManager().replaceEmojis(player, processedMessage);
        
        // Check if message contains clickable actions
        if (containsClickableActions(processedMessage)) {
            // Cancel the original event and send custom formatted message
            event.setCancelled(true);
            
            // Create clickable component
            TextComponent component = plugin.getFilterManager().createClickableComponent(processedMessage);
            
            // Send to all players who can see the chat
            String format = event.getFormat();
            String finalMessage = String.format(format, player.getDisplayName(), "");
            
            for (Player recipient : event.getRecipients()) {
                TextComponent finalComponent = new TextComponent(finalMessage);
                finalComponent.addExtra(component);
                recipient.spigot().sendMessage(finalComponent);
            }
        } else {
            // Set the processed message for normal chat
            event.setMessage(processedMessage);
        }
        
        // Set cooldown for next message (use length-based if configured)
        plugin.getCooldownManager().setCooldown(player, message);
    }
    
    private boolean containsClickableActions(String message) {
        return message.matches(".*:[^:]+:.*");
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // This is handled in the CooldownManager when checking cooldowns
        // We don't need to do anything here as movement reset is checked on demand
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up cooldown when player leaves
        plugin.getCooldownManager().removeCooldown(event.getPlayer());
    }
}