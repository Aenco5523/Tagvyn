# Tagvyn

Tagvyn is a Minecraft 1.21.1 NeoForge nickname/title mod using the package `dev.aenco.tagvyn`.

## Features

- Configurable player nicknames with a localized first-login nickname GUI.
- Text and PNG image titles with RGB colors.
- Titles/nicknames in display names, TAB, chat/name tags and vanilla feedback that uses player display names.
- GUI-first OP administration: player management, title management and reload/sync actions are handled from the admin dashboard.
- Searchable player manager for larger servers.
- Searchable/selectable title manager with in-place editing and PNG replacement.
- Public integration API (`dev.aenco.tagvyn.api.TagvynAPI`).
- Persistent synced identity data.
- Layout intended for future Fabric/Forge and newer-version ports.

## Admin workflow

Tagvyn is designed so server operators do not need to memorize management commands.

Run:

```text
/tagvyn
```

For an OP, this opens the **Tagvyn Admin** dashboard. From there you can:

- Search online players by account name or Tagvyn nickname.
- Select a player from a scrollable list.
- Set/clear their nickname and reset their nickname-change count.
- Search titles and apply/clear a title for the selected player.
- Open the title manager.
- Reload Tagvyn configuration and synchronize title images.
- Open your own nickname editor.

Non-operators using `/tagvyn` are sent directly to their own nickname editor instead of the admin dashboard.

## Title manager GUI

Open **Title Manager** from the admin dashboard.

Registered titles are shown in a searchable, scrollable list. Click a title to load its ID, display text and color into the editor. Existing title IDs are treated as fixed identifiers; you can edit text/color, replace or add a PNG, or delete the selected title without typing its ID again.

Click **New Title** to return to new-title mode. To create or replace an image title, drag a normal PNG file from the desktop onto the Minecraft window and use **Add / Replace PNG**.

PNG limits:

- PNG only
- Up to 512KB
- Up to 256x256 pixels

The server stores uploaded images under:

```text
config/tagvyn/images/<title-id>.png
```

Tagvyn automatically synchronizes uploaded images to clients. Clients build an always-active generated resource pack under their local Tagvyn config directory. The private-use bitmap-font mapping required by Minecraft text rendering is generated internally; server admins do **not** need to make a resource pack, choose a glyph, or type a font ID.

## Commands

These are the commands that currently exist:

```text
/tagvyn
/tagvyn nick
```

`/tagvyn`
- OP: opens the admin dashboard.
- Non-OP: opens the player's nickname editor.

`/tagvyn nick`
- Opens the player's nickname editor directly.

There are **no** `/tagvyn admin`, `/tagvyn titles`, `/tagvyn title create`, `setfor`, or other management subcommands in the current GUI-first command tree. Administrative work is intentionally performed through the dashboard.

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

The included GitHub Actions workflow builds with Java 21 and NeoForge 1.21.1. Build output follows the loader-aware naming rule:

```text
Tagvyn-neoforge-<version>.jar
```
