# Tagvyn

> GUI-first nickname and title management for Minecraft servers.

Tagvyn gives players customizable nicknames and titles while giving server operators a GUI-first way to manage how players are presented in-game. It supports text titles, RGB colors, PNG image titles, and reusable server-defined title systems without requiring operators to memorize a large command tree.

## What Tagvyn adds

- Custom player nicknames
- Text titles with RGB colors
- PNG image titles synchronized by Tagvyn
- Nicknames and titles in TAB, chat/display names, and player name tags
- A first-join nickname setup prompt and GUI
- A GUI-first operator dashboard
- Searchable player and title management for larger servers
- Scrollable admin panels designed for different GUI scales and translated text lengths
- Name Tag based title items that players can right-click to equip
- Persistent nickname/title data across reconnects
- Multiple bundled client languages

## Why use Tagvyn?

Tagvyn is intended for servers that want ranks, roles, event titles, community titles, or custom player identities without turning routine administration into a command-heavy workflow.

Operators can search for a player, edit their nickname, assign or remove a title, create or update titles, upload PNG title images, and create title items from the in-game GUI. Players get a simple nickname setup flow and can use server-issued title items without needing admin commands.

## Before you download

Tagvyn is a **client + server mod**. Install the same compatible Tagvyn build on the server and on every connecting client.

Choose a JAR that matches both your **Minecraft version** and **mod loader**. Build filenames use this format:

```text
Tagvyn-<loader>-<minecraft-version>-<mod-version>.jar
```

The README is intentionally not tied to one permanent Minecraft version or loader. Check the release/build filename and release notes for the exact environment supported by a particular JAR.

Tagvyn changes a player's **displayed nickname/title**. It does **not** replace the authenticated Minecraft account name, UUID, or GameProfile. Vanilla selectors and commands that target the real username continue to use the real username.

## Installation

1. Download the Tagvyn JAR that matches your Minecraft version and mod loader.
2. Put it in the server's `mods` folder.
3. Put the same compatible JAR in every connecting client's `mods` folder.
4. Start the game/server normally.

## Using Tagvyn

### Players

If a player joins without a nickname, Tagvyn shows a localized clickable message that opens the nickname setup screen.

Players can also open the nickname screen with:

```text
/tagvyn nick
```

### Operators

Operators normally only need:

```text
/tagvyn
```

For operators, this opens the **Tagvyn Admin** dashboard. From the GUI an operator can search and select online players, manage nicknames and nickname-change counts, assign or remove titles, create or edit text titles, upload or replace PNG title images, delete titles, create title items, and reload/synchronize Tagvyn data.

Most administration is intentionally handled through the GUI instead of subcommands.

## Title Items

An operator can select a registered title in the title manager and create a special **Name Tag** linked to that title.

Players can right-click the item in the air to equip the linked title. A successful use consumes one item outside Creative mode. Tagvyn title Name Tags cannot be used to rename mobs.

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

## Compatibility

Tagvyn may be released for multiple Minecraft versions and mod loaders over time. Use the loader and Minecraft version encoded in the JAR filename and documented in that release.

## Documentation

This README is focused on what Tagvyn does, why you might want it, what you need before installing it, and how to start using it.

Technical material such as the public API, configuration reference, data formats, image-title internals, and porting notes belongs in the project **Wiki / developer documentation** rather than the main README.

## License

Tagvyn is distributed under a custom **All Rights Reserved** license. Official unmodified releases may be downloaded and used for normal Minecraft gameplay and server operation. Modification, redistribution, repackaging, or commercial use is not permitted unless the license or the copyright holder explicitly allows it.

See [`LICENSE`](LICENSE) for the full terms.
