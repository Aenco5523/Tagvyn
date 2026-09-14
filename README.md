# Tagvyn

Tagvyn is a Minecraft 1.21.1 NeoForge nickname/title mod using the package `dev.aenco.tagvyn`.

## Features

- Configurable player nicknames with localized first-login nickname GUI.
- Text and PNG image titles with RGB colors.
- Titles/nicknames in display names, TAB, chat/name tags and vanilla feedback that uses player display names.
- OP-only management commands without an `admin` command layer.
- OP-only title manager GUI.
- Public integration API (`dev.aenco.tagvyn.api.TagvynAPI`).
- Persistent synced identity data.
- Layout intended for future Fabric/Forge and newer-version ports.

## Title manager GUI

Operators can run:

```text
/tagvyn titles
```

The screen lets an operator enter a title ID, optional text and `#RRGGBB` color. To create an image title, drag a normal PNG file from the desktop onto the Minecraft window and click **Upload PNG title**.

Limits in 0.3.0:

- PNG only
- Up to 512KB
- Up to 256x256 pixels

The server stores uploaded images under:

```text
config/tagvyn/images/<title-id>.png
```

Tagvyn automatically synchronizes uploaded images to clients. Clients build an always-active generated resource pack under their local Tagvyn config directory. The private-use bitmap-font mapping required by Minecraft text rendering is generated internally; server admins do **not** need to make a resource pack, choose a glyph, or type a font ID.

This lets the uploaded image render in the same text-component locations used by Tagvyn, including TAB and player display names/name tags.

## Commands

Player commands:

```text
/tagvyn nick gui
/tagvyn nick set <nickname>
/tagvyn nick clear
/tagvyn nick info
/tagvyn title list
```

OP-only commands:

```text
/tagvyn titles
/tagvyn nick setfor <player> <nickname>
/tagvyn nick clearfor <player>
/tagvyn nick resetcount <player>
/tagvyn title set <player> <title>
/tagvyn title clear <player>
/tagvyn title create text <id> <#RRGGBB> <text>
/tagvyn title delete <id>
/tagvyn reload
```

Non-operators do not receive the management branches in the Brigadier command tree.

## Public API

API version: `2`

```java
TagvynApi api = TagvynAPI.get();

api.setNickname(serverPlayer, "Aenco", false);
api.registerImageTitle(
        "vip",
        "VIP",
        0xFFD700,
        pngBytes,
        false
);
api.refreshTitles(server);
api.setTitle(serverPlayer, "vip");
```

`refreshTitles(server)` refreshes online player title snapshots and synchronizes uploaded image assets to clients.

The older `registerTitle(TagvynTitle, boolean)` method remains available for text titles and advanced external font/glyph integrations, but normal image integrations should use `registerImageTitle(...)`.

## Nickname configuration

NeoForge creates `config/tagvyn-common.toml` automatically.

```toml
[nickname]
changeLimit = 3
minLength = 1
maxLength = 24
allowSpaces = true

[display]
showTitleInDisplayName = true
showTitleInTab = true
```

OPs bypass the nickname change limit.

## Build

JDK 21 is required.

```bash
gradle build
```

The included GitHub Actions workflow builds with Java 21 and NeoForge 1.21.1.
