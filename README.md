# Tagvyn

Tagvyn is a Minecraft 1.21.1 NeoForge nickname/title mod using the package `dev.aenco.tagvyn`.

## Current features

- Player nicknames with a configurable self-change limit.
- Admins bypass nickname limits and can set/clear other players' nicknames.
- Persistent, client-synced player identity data using NeoForge data attachments.
- Titles with text, RGB hex color, and optional image glyphs.
- Nickname/title display in player display names (chat, name tags, and command feedback where Minecraft uses display names) and TAB.
- Title definitions reloadable from `config/tagvyn/titles.json`.
- Korean and English command messages.
- Structure intentionally separates identity/title/display logic from NeoForge event wiring so future Fabric/Forge ports can reuse the model/service layer.

## Commands

```text
/tagvyn nick set <nickname>
/tagvyn nick clear
/tagvyn nick info
/tagvyn title list

/tagvyn admin nick set <player> <nickname>
/tagvyn admin nick clear <player>
/tagvyn admin nick reset-count <player>
/tagvyn admin title set <player> <title>
/tagvyn admin title clear <player>
/tagvyn admin reload
```

Admins are determined by `permissions.adminPermissionLevel` in `tagvyn-common.toml` and have unlimited nickname changes.

## Nickname configuration

NeoForge creates `config/tagvyn-common.toml` automatically. Important options:

```toml
[nickname]
changeLimit = 3 # -1 = unlimited
minLength = 1
maxLength = 24
allowSpaces = true

[permissions]
adminPermissionLevel = 2

[display]
showTitleInDisplayName = true
showTitleInTab = true
```

## Titles and image support

On first server start, Tagvyn creates `config/tagvyn/titles.json`.

```json
{
  "titles": [
    {
      "id": "founder",
      "text": "FOUNDER",
      "color": "#FFB347",
      "image": {
        "font": "my_pack:badges",
        "glyph": "\uE001"
      }
    }
  ]
}
```

Minecraft text components cannot embed arbitrary PNG/JPG files directly. Tagvyn therefore uses Minecraft's bitmap-font glyph system for image titles. Put the image in a client resource pack/mod resource font, map it to a private-use glyph such as `U+E001`, then reference that font and glyph in `titles.json`. This makes the same image badge usable in TAB, chat/display names, command feedback, and name tags.

After editing title definitions, run `/tagvyn admin reload`.

## Command targeting note

Tagvyn does **not** replace the authenticated Mojang/GameProfile username. Vanilla target selectors and commands still target the real username. This avoids breaking authentication, selectors, permissions, whitelists, and other mods. Wherever vanilla/NeoForge uses the player's display name, Tagvyn supplies the nickname/title instead.

## Build

JDK 21 is required.

```bash
gradle build
```

A GitHub Actions workflow is included and installs Gradle 8.14.3 automatically.

## Porting layout

The current implementation is NeoForge 1.21.1, but the code is split by responsibility:

- `data/` — serializable player identity model + NeoForge attachment adapter
- `title/` — data-driven title definitions
- `display/` — component formatting
- `service/` — nickname/title rules
- `command/` and `event/` — NeoForge/Minecraft integration

For a later Fabric/Forge port, keep the identity/title/service model and replace the attachment/event/registration adapters.
