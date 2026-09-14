# Tagvyn

> A GUI-first nickname and title mod for Minecraft.

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
- Multiple client languages

## Installation

1. Download a Tagvyn build that matches your **Minecraft version** and **mod loader**.
2. Put the Tagvyn JAR in the `mods` folder on the **server**.
3. Put the same matching Tagvyn JAR in the `mods` folder on every connecting **client**.
4. Start the game/server normally.

Build filenames tell you which loader and Minecraft version they target:

```text
Tagvyn-<loader>-<minecraft-version>-<mod-version>.jar
```

Always use the release/build whose loader and Minecraft version match your installation.

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

## Languages

Tagvyn follows the Minecraft client's selected language.

Currently bundled translations:

- English
- 한국어
- 日本語
- 简体中文
- 繁體中文
- Español
- Français
- Deutsch
- Português (Brasil)

Unsupported languages fall back to the default English strings provided by the mod/game resource system.

## Commands

| Command | Purpose |
| --- | --- |
| `/tagvyn` | Opens the admin dashboard for OPs, or the nickname screen for normal players |
| `/tagvyn nick` | Opens the nickname screen directly |

Most administration is intentionally handled through the GUI instead of subcommands.

## Compatibility

Tagvyn may be released for multiple Minecraft versions and mod loaders over time.

Do not treat this README as a fixed compatibility table. Check the **release/build filename** and release notes for the exact Minecraft version and loader supported by a particular JAR.

## Documentation

This README is intentionally focused on installing and using Tagvyn.

Technical documentation such as the API, configuration reference, data format, image-title internals, and porting notes belongs in the project **Wiki / developer documentation** rather than the main README.

## License

**All Rights Reserved.**
