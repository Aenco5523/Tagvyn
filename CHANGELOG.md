# Changelog

## 0.4.1

- Fixed the first-login nickname prompt opening the removed `/tagvyn nick gui` command path.
- The clickable prompt now opens the nickname GUI through `/tagvyn nick`.
- Updated the Korean prompt to: `닉네임이 설정되지 않았습니다. 이 메시지를 클릭하여 닉네임을 설정하세요.`
- Updated the English prompt to match the same meaning.

## 0.3.0

- Added an OP-only title manager GUI opened with `/tagvyn titles`.
- Added drag-and-drop PNG title uploads from the in-game GUI.
- Uploaded PNG files are validated and stored on the server in `config/tagvyn/images/`.
- Added automatic title-image synchronization to connected clients.
- Added an always-active generated client resource pack so admins no longer create bitmap fonts/glyphs manually.
- Removed the manual `/tagvyn title create image <font> <glyph>` workflow.
- Added API v2 methods for registering and reading uploaded PNG title images.

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
