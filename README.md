# ChatPlus

ChatPlus is a **lightweight but flexible** Paper/Spigot plugin that improves the chat experience by combining **anti-spam tools and fun emoji replacements.** It’s designed to be simple to use but highly configurable, with support for multiple config files, per-group/per-world rules, and optional advanced filters.

# ✨ Features
```
Chat Cooldown

Prevents spam by adding a delay between messages.

Configurable per-world, per-group, or globally.

Duplicate-message detection (anti-paste spam).

Optional movement-reset, quiet hours, and cooldown by message length.

Advanced Config System
```
# Multiple YAML files:
```
config.yml → global settings.

cooldowns.yml → cooldown rules.

emojis.yml → emoji definitions.

messages.yml → all player-facing text.

filters.yml → optional caps/repeat/blacklist filter.

Hot-reloadable with /chatplus reload.
```
# Optional Tools
```
Player toggles (/chatplus toggle emojis).
Group-specific join/leave messages.
Slowmode command for staff.
PlaceholderAPI integration.
Toggle Chat 
```
# 🔑 Permissions
```
chatplus.bypass.cooldown → no cooldown.
chatplus.bypass.duplicate → no duplicate check.
chatplus.emojis.use → allow emoji replacements.
chatplus.admin → admin commands.
```
# ⌨️ Commands
```
/chatplus reload → reload configs.
/chatplus status → see your cooldown/emoji status.
/chatplus toggle <feature> → toggle features for yourself.
/slowmode <seconds> → staff-only global cooldown.
```
# 📦 Compatibility
```
Minecraft: 1.20+ → 1.21+
Server: Paper / Spigot
Integrates with: PlaceholderAPI, LuckPerms, LiteBans, Vault
```
# 🛠️ Performance
```
Asynchronous chat handling where possible.
No database required, YAML only.
Lightweight (under 150kb).
```
