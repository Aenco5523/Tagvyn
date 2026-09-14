# Changelog

## 0.6.1

- Simplified the README into a player/server-admin focused project page.
- Removed low-level API, configuration, resource-pack and implementation details from the main README in preparation for separate Wiki/developer documentation.
- Updated build naming to include the Minecraft version: `Tagvyn-<loader>-<minecraft-version>-<mod-version>.jar`.
- NeoForge 1.21.1 builds now use names such as `Tagvyn-neoforge-1.21.1-0.6.1.jar`.

## 0.6.0

- Added Name Tag based title items generated from the title manager GUI.
- Operators can select an existing title and receive a glowing title token without using a command.
- Right-clicking a title token equips the linked title and consumes one token on successful use, except in Creative mode.
- Tagvyn title Name Tags cannot be used to rename mobs.
- Title items store a stable title ID, so later text/color/PNG edits are reflected when the item is used.
- Deleted-title tokens become invalid and are not consumed.
- Added localized item name/lore and title-item feedback messages.

## 0.5.0

- Reworked player administration for larger servers: online players are now shown in a searchable, scrollable list instead of being selected one-by-one with arrow buttons.
- Player search matches both the authenticated Minecraft username and the Tagvyn nickname.
- Reworked title assignment in the player manager into a searchable, scrollable title list.
- Preserves the currently managed player after nickname/title actions instead of jumping back to the first online player.
- Reworked title management into a selectable list workflow: click a title to load it into the editor, then save text/color changes, replace its PNG, or delete it without retyping the ID.
- Added title search, scrolling, a clear New Title mode, and selected-title persistence after saves.
- Existing title IDs are treated as stable identifiers while editing.
- Added server-side title metadata updates that preserve existing PNG/font data.
- PNG uploads can replace the selected title image safely while retaining the existing image if the update fails.
- Bumped the network protocol for the updated manager payloads.
- Removed stale command documentation. The current command tree is `/tagvyn` and `/tagvyn nick`; OP management is GUI-first through `/tagvyn`.
- Updated the in-game mod description to match the GUI-first workflow.

## 0.4.3

- Reworked the title manager into a responsive layout so registered titles no longer fall off the bottom of short game windows.
- Uses a two-column layout on wider windows: title form on the left and registered titles on the right.
- Falls back to a compact single-column layout on narrow windows.
- Long title rows and PNG helper text are clipped with an ellipsis instead of drawing outside the available area.
- The visible title row count now adapts to the current screen height.

## 0.4.2

- Fixed GUI layer ordering so labels and helper text render above EditBox/Button backgrounds.
- Adjusted player-manager spacing so status and field labels no longer overlap.
- Stopped reloading all client resource packs on every login/title-image sync.
- The generated title-image pack now reloads only when its PNG/font contents actually change, reducing client texture side effects such as skins falling back to a default appearance.

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
