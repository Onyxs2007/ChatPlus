package com.chatplus.managers;

import com.chatplus.ChatPlusPlugin;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterManager {
    
    private final ChatPlusPlugin plugin;
    private final Map<String, Pattern> compiledPatterns = new HashMap<>();
    private final Map<String, ClickableAction> clickableActions = new HashMap<>();
    
    public FilterManager(ChatPlusPlugin plugin) {
        this.plugin = plugin;
        loadClickableActions();
    }
    
    private void loadClickableActions() {
        clickableActions.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("clickable-actions");
        
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection actionSection = section.getConfigurationSection(key);
                if (actionSection != null) {
                    ClickableAction action = new ClickableAction(
                        actionSection.getString("display", key),
                        actionSection.getString("hover", ""),
                        actionSection.getString("click-action", "OPEN_URL"),
                        actionSection.getString("click-value", "")
                    );
                    clickableActions.put(key, action);
                }
            }
        }
    }
    
    public boolean shouldFilterMessage(Player player, String message) {
        if (!plugin.getConfigManager().isFilterEnabled()) {
            return false;
        }
        
        // Check profanity/blacklist first
        if (containsProfanity(player, message)) {
            return true;
        }
        
        // Check for excessive repeated characters
        if (hasExcessiveRepeatedChars(message)) {
            return true;
        }
        
        // Check for excessive caps
        if (hasExcessiveCaps(message)) {
            return true;
        }
        
        return false;
    }
    
    private boolean containsProfanity(Player player, String message) {
        if (player.hasPermission("chatplus.bypass.profanity")) {
            return false;
        }
        
        List<String> blacklist = getBlacklist();
        String lowerMessage = message.toLowerCase();
        
        for (String word : blacklist) {
            if (word.trim().isEmpty()) continue;
            
            // Create pattern if not cached
            if (!compiledPatterns.containsKey(word)) {
                // Use word boundaries to avoid false positives
                String regex = "\\b" + Pattern.quote(word.toLowerCase()) + "\\b";
                compiledPatterns.put(word, Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
            }
            
            Pattern pattern = compiledPatterns.get(word);
            if (pattern.matcher(lowerMessage).find()) {
                return true;
            }
        }
        
        return false;
    }
    
    public String processMessage(Player player, String message) {
        String processedMessage = message;
        
        // Replace profanity with symbols
        processedMessage = replaceProfanity(player, processedMessage);
        
        // Process clickable actions
        processedMessage = processClickableActions(processedMessage);
        
        return processedMessage;
    }
    
    private String replaceProfanity(Player player, String message) {
        if (player.hasPermission("chatplus.bypass.profanity")) {
            return message;
        }
        
        String action = getProfanityAction();
        if (action.equals("block")) {
            // Message will be blocked by shouldFilterMessage
            return message;
        }
        
        if (!action.equals("replace")) {
            return message;
        }
        
        List<String> blacklist = getBlacklist();
        String result = message;
        
        for (String word : blacklist) {
            if (word.trim().isEmpty()) continue;
            
            Pattern pattern = compiledPatterns.get(word);
            if (pattern == null) {
                String regex = "\\b" + Pattern.quote(word.toLowerCase()) + "\\b";
                pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                compiledPatterns.put(word, pattern);
            }
            
            Matcher matcher = pattern.matcher(result);
            result = matcher.replaceAll(createReplacement(word));
        }
        
        return result;
    }
    
    private String createReplacement(String word) {
        String replacementType = getReplacementType();
        
        switch (replacementType.toLowerCase()) {
            case "asterisk":
                return "*".repeat(word.length());
            case "hash":
                return "#".repeat(word.length());
            case "dash":
                return "-".repeat(word.length());
            case "custom":
                String custom = getCustomReplacement();
                return custom.repeat(Math.max(1, word.length() / custom.length()));
            case "partial":
                // Keep first and last letter, replace middle with *
                if (word.length() <= 2) {
                    return "*".repeat(word.length());
                }
                return word.charAt(0) + "*".repeat(word.length() - 2) + word.charAt(word.length() - 1);
            default:
                return "*".repeat(word.length());
        }
    }
    
    private String processClickableActions(String message) {
        String result = message;
        
        for (Map.Entry<String, ClickableAction> entry : clickableActions.entrySet()) {
            String trigger = entry.getKey();
            ClickableAction action = entry.getValue();
            
            // Replace trigger with display text (for non-JSON chat)
            result = result.replace(":" + trigger + ":", action.display);
        }
        
        return result;
    }
    
    public TextComponent createClickableComponent(String message) {
        TextComponent component = new TextComponent("");
        
        String[] parts = message.split("(?=:[^:]+:)|(?<=:[^:]+:)");
        
        for (String part : parts) {
            if (part.matches(":[^:]+:")) {
                String trigger = part.substring(1, part.length() - 1);
                ClickableAction action = clickableActions.get(trigger);
                
                if (action != null) {
                    TextComponent clickable = new TextComponent(action.display);
                    
                    // Add hover text
                    if (!action.hoverText.isEmpty()) {
                        clickable.setHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', action.hoverText)).create()
                        ));
                    }
                    
                    // Add click action
                    if (!action.clickValue.isEmpty()) {
                        ClickEvent.Action clickAction = parseClickAction(action.clickAction);
                        if (clickAction != null) {
                            clickable.setClickEvent(new ClickEvent(clickAction, action.clickValue));
                        }
                    }
                    
                    component.addExtra(clickable);
                } else {
                    component.addExtra(new TextComponent(part));
                }
            } else {
                component.addExtra(new TextComponent(part));
            }
        }
        
        return component;
    }
    
    private ClickEvent.Action parseClickAction(String action) {
        try {
            return ClickEvent.Action.valueOf(action.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid click action: " + action);
            return null;
        }
    }
    
    private boolean hasExcessiveRepeatedChars(String message) {
        int maxRepeated = plugin.getConfigManager().getMaxRepeatedChars();
        if (maxRepeated <= 0) {
            return false;
        }
        
        char lastChar = 0;
        int count = 1;
        
        for (char c : message.toCharArray()) {
            if (c == lastChar) {
                count++;
                if (count > maxRepeated) {
                    return true;
                }
            } else {
                count = 1;
                lastChar = c;
            }
        }
        
        return false;
    }
    
    private boolean hasExcessiveCaps(String message) {
        double maxCapsRatio = plugin.getConfigManager().getMaxCapsRatio();
        int minCapsLength = plugin.getConfigManager().getMinCapsLength();
        
        if (maxCapsRatio <= 0 || message.length() < minCapsLength) {
            return false;
        }
        
        int capsCount = 0;
        int letterCount = 0;
        
        for (char c : message.toCharArray()) {
            if (Character.isLetter(c)) {
                letterCount++;
                if (Character.isUpperCase(c)) {
                    capsCount++;
                }
            }
        }
        
        if (letterCount == 0) {
            return false;
        }
        
        double capsRatio = (double) capsCount / letterCount;
        return capsRatio > maxCapsRatio;
    }
    
    private List<String> getBlacklist() {
        return plugin.getConfig().getStringList("filters.blacklist.words");
    }
    
    private String getProfanityAction() {
        return plugin.getConfig().getString("filters.blacklist.action", "replace");
    }
    
    private String getReplacementType() {
        return plugin.getConfig().getString("filters.blacklist.replacement-type", "asterisk");
    }
    
    private String getCustomReplacement() {
        return plugin.getConfig().getString("filters.blacklist.custom-replacement", "*");
    }
    
    public String getFilteredMessage(Player player) {
        return plugin.getConfigManager().getMessage("filtered")
                .replace("{player}", player.getName());
    }
    
    private static class ClickableAction {
        final String display;
        final String hoverText;
        final String clickAction;
        final String clickValue;
        
        ClickableAction(String display, String hoverText, String clickAction, String clickValue) {
            this.display = display;
            this.hoverText = hoverText;
            this.clickAction = clickAction;
            this.clickValue = clickValue;
        }
    }
}