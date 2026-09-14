# Changelog

## 0.2.0

- Added public `TagvynAPI` integration surface for other mods.
- Added persistent in-game title registration and deletion for server operators.
- Removed the `/tagvyn admin ...` command layer; management commands are now direct OP-only subcommands.
- Added a localized clickable nickname prompt for players without a nickname.
- Added a custom nickname input GUI opened from the prompt or `/tagvyn nick gui`.
- Added NeoForge play payloads for opening/submitting the nickname GUI.

## 0.1.0

- Initial NeoForge 1.21.1 implementation.
- Configurable player nickname changes with unlimited admin changes.
- Text, RGB-color, and bitmap-font image titles.
- Display integration for player display names, TAB, chat/name tags, and command feedback where Minecraft uses display names.
- Synced persistent identity data and reloadable title definitions.
