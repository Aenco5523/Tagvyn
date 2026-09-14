# Tagvyn

> A GUI-first nickname and title mod for Minecraft.

**Minecraft 1.21.1 · NeoForge · Java 21 · Client + Server**

Tagvyn lets players use custom nicknames and lets server operators manage text titles, PNG image titles, and title items without memorizing a pile of commands.

## Features

- Custom player nicknames
- Text titles with RGB colors
- PNG image titles
- Nicknames and titles in TAB, chat/display names, and player name tags
- First-join nickname setup prompt and GUI
- GUI-first admin dashboard for operators
- Searchable player management for larger servers
- Searchable title manager with editing and PNG replacement
- Name Tag based title items that players can right-click to equip
- Persistent nickname/title data across reconnects

## Installation

1. Install **NeoForge for Minecraft 1.21.1**.
2. Put the Tagvyn JAR in the `mods` folder on the **server**.
3. Put the same Tagvyn JAR in the `mods` folder on every connecting **client**.
4. Start the game/server normally.

Current build naming format:

```text
Tagvyn-<loader>-<minecraft-version>-<mod-version>.jar
```

Example:

```text
Tagvyn-neoforge-1.21.1-0.6.1.jar
```

## Using Tagvyn

### Players

If a player joins without a nickname, Tagvyn shows a clickable message that opens the nickname setup screen.

Players can also open the nickname screen with:

```text
/tagvyn nick
```

### Operators

Operators only need one main command:

```text
/tagvyn
```

It opens the **Tagvyn Admin** dashboard. From there an operator can:

- Search and select online players
- Set or clear nicknames
- Reset nickname-change counts
- Search, assign, or remove titles
- Create and edit text titles
- Create or replace PNG image titles
- Delete titles
- Create title items
- Reload Tagvyn data and synchronize title images

## Title Items

An operator can select a registered title in the title manager and create a special **Name Tag** for it.

Players can right-click the item to equip the linked title. A successful use consumes one item outside Creative mode, and Tagvyn title Name Tags cannot be used to rename mobs.

## Commands

| Command | Purpose |
| --- | --- |
| `/tagvyn` | Opens the admin dashboard for OPs, or the nickname screen for normal players |
| `/tagvyn nick` | Opens the nickname screen directly |

Most administration is intentionally handled through the GUI instead of subcommands.

## Compatibility

| | Current support |
| --- | --- |
| Minecraft | **1.21.1** |
| Loader | **NeoForge 21.1+** |
| Java | **21** |
| Installation | **Client + Server** |

Fabric and Forge builds are not available yet.

## Documentation

This README is intentionally focused on installing and using Tagvyn.

Technical documentation such as the API, configuration reference, data format, image-title internals, and porting notes will be moved to a dedicated **Wiki / developer documentation** as the project grows.

## License

**All Rights Reserved.**
