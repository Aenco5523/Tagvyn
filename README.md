# Tagvyn

Tagvyn is a Minecraft 1.21.1 NeoForge nickname/title mod using the package `dev.aenco.tagvyn`.

## Current features

- Player nicknames with a configurable self-change limit.
- Server operators bypass nickname limits and can manage other players.
- Players without a nickname receive a localized clickable setup message when they join.
- Clicking the setup message opens a custom nickname input GUI.
- Persistent, client-synced player identity data using NeoForge data attachments.
- Titles with text, RGB hex color, and optional image glyphs.
- Operators can create/delete title definitions in-game; changes persist to `config/tagvyn/titles.json`.
- Nickname/title display in player display names (chat, name tags, and command feedback where Minecraft uses display names) and TAB.
- Public `TagvynAPI` for integrations from other mods.
- Korean and English UI/messages.
- Common-facing API/model code is separated from NeoForge wiring to make later Fabric/Forge/version ports easier.

## Commands

Player commands:

```text
/tagvyn nick gui
/tagvyn nick set <nickname>
/tagvyn nick clear
/tagvyn nick info
/tagvyn title list
```

OP-only management commands (vanilla permission level 2):

```text
/tagvyn nick setfor <player> <nickname>
/tagvyn nick clearfor <player>
/tagvyn nick resetcount <player>

/tagvyn title set <player> <title>
/tagvyn title clear <player>
/tagvyn title create text <id> <#RRGGBB> <text>
/tagvyn title create image <id> <#RRGGBB> <font> <glyph> [text]
/tagvyn title delete <id>

/tagvyn reload
```

There is no `/tagvyn admin ...` layer. If a player is not an OP, management branches are hidden by Brigadier's permission requirement.

## Nickname setup flow

If a player joins without a nickname, Tagvyn sends a translatable clickable message. For `ko_kr` clients it appears as:

```text
이 메시지를 클릭하여 닉네임을 설정하세요.
```

The same component is translated by the client for other languages. Clicking it runs `/tagvyn nick gui`, and the server opens Tagvyn's nickname input screen. The GUI receives the server's min/max length, space rule, and remaining-change count and submits the nickname back through a NeoForge payload.

## Nickname configuration

NeoForge creates `config/tagvyn-common.toml` automatically:

```toml
[nickname]
changeLimit = 3 # -1 = unlimited; OPs always bypass this limit
minLength = 1
maxLength = 24
allowSpaces = true

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

Minecraft text components cannot embed arbitrary PNG/JPG files directly. Tagvyn uses Minecraft's bitmap-font glyph system for image titles. Put the image in a client resource pack/mod resource font, map it to a private-use glyph such as `U+E001`, then reference that font and glyph in the title definition.

Operators can also register definitions in-game. For example:

```text
/tagvyn title create text vip #FFD700 VIP
/tagvyn title create image founder #FFB347 my_pack:badges <glyph> FOUNDER
```

Definitions created through commands or the API are written back to `titles.json` immediately.

## Public API

The stable entry point is `dev.aenco.tagvyn.api.TagvynAPI`.

```java
import dev.aenco.tagvyn.api.TagvynAPI;
import dev.aenco.tagvyn.api.TagvynApi;
import dev.aenco.tagvyn.api.TagvynTitle;

TagvynApi api = TagvynAPI.get();

String shownName = api.getEffectiveName(player);
api.setNickname(serverPlayer, "Aenco", false);

api.registerTitle(new TagvynTitle(
        "vip",
        "VIP",
        0xFFD700,
        "",
        ""
), false);
api.refreshTitles(server);
api.setTitle(serverPlayer, "vip");
```

Main API operations include:

- Read a player's immutable `IdentitySnapshot`.
- Read/set/clear nicknames with an explicit limit-bypass flag.
- Read/set/clear player titles.
- Register/delete persistent title definitions.
- Refresh online players after title definition changes.
- Build Tagvyn-formatted display components.

`TagvynAPI.API_VERSION` is currently `1`.

## Command targeting note

Tagvyn does **not** replace the authenticated Mojang/GameProfile username. Vanilla target selectors and commands still target the real username. This avoids breaking authentication, selectors, permissions, whitelists, and other mods. Wherever vanilla/NeoForge uses the player's display name, Tagvyn supplies the nickname/title instead.

## Build

JDK 21 is required.

```bash
gradle build
```

A GitHub Actions workflow is included and installs Gradle automatically.

## Porting layout

The current implementation is NeoForge 1.21.1, but the code is split by responsibility:

- `api/` — loader-facing public integration API and immutable models
- `data/` — serializable identity model + NeoForge attachment adapter
- `title/` — persistent title definitions/registry
- `display/` — component formatting
- `service/` — nickname/title rules + API implementation
- `network/` — NeoForge GUI payloads
- `client/` — nickname screen/client handlers
- `command/` and `event/` — command/event integration

A later Fabric/Forge port can preserve the public API/model contracts and replace the attachment, networking, and event adapters.
