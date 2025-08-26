package com.chatplus.commands;

import com.chatplus.ChatPlusPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatPlusCommand implements CommandExecutor, TabCompleter {
    
    private final ChatPlusPlugin plugin;
    
    public ChatPlusCommand(ChatPlusPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;
            case "test":
                handleTest(sender);
                break;
            case "status":
                handleStatus(sender);
                break;
            case "toggle":
                handleToggle(sender, args);
                break;
            case "help":
                sendHelpMessage(sender);
                break;
            default:
                sender.sendMessage(plugin.getConfigManager().getMessage("unknown-command")
                    .replace("{command}", subCommand));
                sendHelpMessage(sender);
        }
        
        return true;
    }
    
    private void handleToggle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chatplus.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /chatplus toggle <chat|world> [world-name]");
            return;
        }
        
        String toggleType = args[1].toLowerCase();
        
        switch (toggleType) {
            case "chat":
                boolean currentState = plugin.getChatToggleManager().isChatMuted();
                plugin.getChatToggleManager().setChatMuted(!currentState);
                
                String message = !currentState ? 
                    plugin.getConfigManager().getMessage("chat-toggle-disabled") :
                    plugin.getConfigManager().getMessage("chat-toggle-enabled");
                
                sender.sendMessage(message);
                
                // Broadcast to all players (except those with bypass)
                plugin.getServer().getOnlinePlayers().forEach(player -> {
                    if (!player.hasPermission("chatplus.bypass.toggle")) {
                        player.sendMessage(message);
                    }
                });
                break;
                
            case "world":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /chatplus toggle world <world-name>");
                    return;
                }
                
                String worldName = args[2];
                boolean worldMuted = plugin.getChatToggleManager().isWorldMuted(worldName);
                plugin.getChatToggleManager().setWorldMuted(worldName, !worldMuted);
                
                String worldMessage = !worldMuted ?
                    plugin.getConfigManager().getMessage("world-chat-disabled").replace("{world}", worldName) :
                    plugin.getConfigManager().getMessage("world-chat-enabled").replace("{world}", worldName);
                
                sender.sendMessage(worldMessage);
                
                // Notify players in that world
                plugin.getServer().getOnlinePlayers().forEach(player -> {
                    if (player.getWorld().getName().equals(worldName) && 
                        !player.hasPermission("chatplus.bypass.toggle")) {
                        player.sendMessage(worldMessage);
                    }
                });
                break;
                
            default:
                sender.sendMessage(ChatColor.RED + "Usage: /chatplus toggle <chat|world> [world-name]");
        }
    }
    
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("chatplus.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        try {
            plugin.getConfigManager().reloadAllConfigs();
            plugin.getCooldownManager().clearAllCooldowns();
            sender.sendMessage(plugin.getConfigManager().getMessage("reload-success"));
        } catch (Exception e) {
            sender.sendMessage(plugin.getConfigManager().getMessage("reload-error")
                .replace("{error}", e.getMessage()));
            plugin.getLogger().severe("Error reloading configuration: " + e.getMessage());
        }
    }
    
    private void handleTest(CommandSender sender) {
        if (!sender.hasPermission("chatplus.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return;
        }
        
        Player player = (Player) sender;
        String prefix = plugin.getConfigManager().getPrefix();
        
        sender.sendMessage(prefix + ChatColor.YELLOW + "Rule Resolution Test:");
        
        // Cooldown info
        long cooldown = plugin.getCooldownManager().getEffectiveCooldown(player);
        sender.sendMessage(ChatColor.GRAY + "Cooldown: " + ChatColor.WHITE + cooldown + "s");
        
        // Group info
        String group = plugin.getCooldownManager().getPlayerGroup(player);
        sender.sendMessage(ChatColor.GRAY + "Group: " + ChatColor.WHITE + group);
        
        // World info
        String world = player.getWorld().getName();
        sender.sendMessage(ChatColor.GRAY + "World: " + ChatColor.WHITE + world);
        
        // Chat toggle status
        boolean canChat = plugin.getChatToggleManager().canPlayerChat(player);
        sender.sendMessage(ChatColor.GRAY + "Can Chat: " + ChatColor.WHITE + 
            (canChat ? "Yes" : "No"));
        
        // Emoji status
        boolean emojisEnabled = plugin.getEmojiManager().areEmojisEnabledForPlayer(player);
        sender.sendMessage(ChatColor.GRAY + "Emojis: " + ChatColor.WHITE + 
            (emojisEnabled ? "Enabled" : "Disabled"));
        
        // Permissions
        sender.sendMessage(ChatColor.GRAY + "Bypass Cooldown: " + ChatColor.WHITE + 
            player.hasPermission("chatplus.bypass.cooldown"));
        sender.sendMessage(ChatColor.GRAY + "Bypass Duplicate: " + ChatColor.WHITE + 
            player.hasPermission("chatplus.bypass.duplicate"));
        sender.sendMessage(ChatColor.GRAY + "Bypass Profanity: " + ChatColor.WHITE + 
            player.hasPermission("chatplus.bypass.profanity"));
        sender.sendMessage(ChatColor.GRAY + "Bypass Toggle: " + ChatColor.WHITE + 
            player.hasPermission("chatplus.bypass.toggle"));
    }
    
    private void handleStatus(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return;
        }
        
        Player player = (Player) sender;
        String prefix = plugin.getConfigManager().getPrefix();
        
        // Remaining cooldown
        long remaining = plugin.getCooldownManager().getRemainingCooldown(player);
        sender.sendMessage(prefix + ChatColor.GRAY + "Cooldown: " + ChatColor.WHITE + 
            (remaining > 0 ? remaining + "s remaining" : "Ready"));
        
        // Chat status
        boolean canChat = plugin.getChatToggleManager().canPlayerChat(player);
        sender.sendMessage(prefix + ChatColor.GRAY + "Chat: " + ChatColor.WHITE + 
            (canChat ? "Enabled" : "Disabled"));
        
        // Emoji status
        boolean emojisEnabled = plugin.getEmojiManager().areEmojisEnabledForPlayer(player);
        sender.sendMessage(prefix + ChatColor.GRAY + "Emojis: " + ChatColor.WHITE + 
            (emojisEnabled ? "Active" : "Inactive"));
    }
    
    private void sendHelpMessage(CommandSender sender) {
        String prefix = plugin.getConfigManager().getPrefix();
        sender.sendMessage(prefix + ChatColor.YELLOW + "ChatPlus Commands:");
        
        if (sender.hasPermission("chatplus.admin")) {
            sender.sendMessage(ChatColor.GOLD + "/chatplus reload" + ChatColor.WHITE + " - Reload all YAML files");
            sender.sendMessage(ChatColor.GOLD + "/chatplus test" + ChatColor.WHITE + " - Show current rule resolution");
            sender.sendMessage(ChatColor.GOLD + "/chatplus toggle chat" + ChatColor.WHITE + " - Toggle global chat on/off");
            sender.sendMessage(ChatColor.GOLD + "/chatplus toggle world <name>" + ChatColor.WHITE + " - Toggle chat for specific world");
        }
        
        sender.sendMessage(ChatColor.GOLD + "/chatplus status" + ChatColor.WHITE + " - Show your cooldown and emoji status");
        sender.sendMessage(ChatColor.GOLD + "/chatplus help" + ChatColor.WHITE + " - Show this help message");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            
            if (sender.hasPermission("chatplus.admin")) {
                completions.addAll(Arrays.asList("reload", "test", "toggle"));
            }
            
            completions.addAll(Arrays.asList("status", "help"));
            
            List<String> result = new ArrayList<>();
            for (String completion : completions) {
                if (completion.toLowerCase().startsWith(args[0].toLowerCase())) {
                    result.add(completion);
                }
            }
            
            return result;
        }
        
        if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
            List<String> completions = Arrays.asList("chat", "world");
            List<String> result = new ArrayList<>();
            
            for (String completion : completions) {
                if (completion.toLowerCase().startsWith(args[1].toLowerCase())) {
                    result.add(completion);
                }
            }
            
            return result;
        }
        
        if (args.length == 3 && args[0].equalsIgnoreCase("toggle") && args[1].equalsIgnoreCase("world")) {
            List<String> worlds = new ArrayList<>();
            plugin.getServer().getWorlds().forEach(world -> worlds.add(world.getName()));
            
            List<String> result = new ArrayList<>();
            for (String world : worlds) {
                if (world.toLowerCase().startsWith(args[2].toLowerCase())) {
                    result.add(world);
                }
            }
            
            return result;
        }
        
        return new ArrayList<>();
    }
}