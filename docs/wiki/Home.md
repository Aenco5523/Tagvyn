# Tagvyn Developer Wiki

This documentation is for mod developers, maintainers, and people porting or integrating with Tagvyn.

Tagvyn is a GUI-first nickname and title system for Minecraft. The public integration surface lives in `dev.aenco.tagvyn.api`; loader-specific implementation code is kept behind that API so integrations can avoid depending on internal NeoForge classes.

> **Current API version:** `2`
>
> **Important:** The exact Minecraft version and mod loader are build-specific. Do not assume one permanent target from this Wiki.

## Start here

- [Public API](API.md) — read identities, change nicknames/titles, register text or PNG titles, and format display names.
- [Architecture](Architecture.md) — package layout, persistence, display flow, GUI/network boundaries, and server authority.
- [Configuration](Configuration.md) — nickname limits, validation rules, and display options.
- [Titles and Images](Titles-and-Images.md) — title IDs, `titles.json`, uploaded PNGs, generated glyphs, synchronization, and title items.
- [Networking](Networking.md) — payload groups, protocol versioning, validation, and client/server responsibilities.
- [Localization](Localization.md) — translation keys, CI validation, placeholders, and multilingual UI guidance.
- [Build and Porting](Build-and-Porting.md) — development setup, output naming, CI expectations, and loader/version porting guidance.

## Integration principles

Use `TagvynAPI`/`TagvynApi` instead of calling internal services directly. The API is intentionally expressed using Java and Minecraft types rather than NeoForge-specific public types where possible.

Mutations that affect players must run on the logical server. Treat the server as authoritative for nickname limits, title existence, uploaded images, and administrative permissions. Client GUIs are only front ends; they do not grant permission by themselves.

Tagvyn changes **display identity**, not authentication identity. A Tagvyn nickname does not replace the Mojang/Microsoft authenticated account name, UUID, or `GameProfile`. Vanilla selectors and systems that resolve real usernames continue to use the real username.

## Source layout

The primary package is `dev.aenco.tagvyn`.

| Package | Responsibility |
| --- | --- |
| `api` | Stable integration surface for other mods |
| `client` | Screens, client payload handlers, generated client resource pack |
| `command` | Minimal `/tagvyn` entry commands |
| `config` | Common configuration |
| `data` | Persistent/synchronized player identity data |
| `display` | Builds nickname/title `Component`s |
| `event` | Login/server lifecycle and display integration events |
| `item` | Name Tag based title items |
| `network` | NeoForge payload registration and server/client actions |
| `service` | Nickname/title business logic and API implementation |
| `title` | Title registry and PNG storage |

## Stability

Only classes under `dev.aenco.tagvyn.api` should be treated as the public integration contract unless another package is explicitly documented as public later. Internal packages may change between Tagvyn releases without preserving source or binary compatibility.

Before relying on API behavior, check `TagvynAPI.API_VERSION`. If a future release changes the public contract incompatibly, that version is expected to change.

## License note

Tagvyn source availability does not grant permission to redistribute modified builds or derivative versions. See the repository `LICENSE` before publishing integrations, forks, ports, bundled copies, or modified distributions. An integration that merely calls the public API is conceptually separate from copying or redistributing Tagvyn itself; distribution questions are still governed by the license terms.
