# Architecture

Tagvyn is organized so player-facing UI and loader-specific mechanics stay behind a small public API. The current implementation is NeoForge-based, but the package boundaries are intended to make future loader/version ports less invasive.

## High-level flow

A player's Tagvyn identity is stored as synchronized persistent player data. Server-side services mutate that identity, then refresh Minecraft's display/tab names and sync the attachment. Display hooks read the synced identity and construct the visible name/title component.

For uploaded image titles, the server stores the PNG and title definition. Clients receive an image manifest/data sync and rebuild Tagvyn's generated client resource pack. Display formatting then uses the generated bitmap font/glyph as part of a normal Minecraft `Component`.

## Bootstrap

`dev.aenco.tagvyn.Tagvyn` is the loader entry point. During construction it currently:

1. registers the player identity attachment;
2. registers Tagvyn GUI/image networking;
3. registers title-item networking;
4. registers the generated client resource pack on physical clients;
5. bootstraps `TagvynAPI` with the NeoForge implementation;
6. registers the common config.

The public API is initialized with `NeoForgeTagvynApi.INSTANCE`, while consumers access it only through `TagvynAPI`/`TagvynApi`.

## Player identity data

`data/TagvynAttachments` registers one `AttachmentType<IdentityData>` named `tagvyn:identity`.

The attachment is:

- created with an empty identity by default;
- serialized persistently with `IdentityData.CODEC`;
- copied on death;
- synchronized with `IdentityData.STREAM_CODEC`.

This makes nickname/title state available on both server and client without replacing the player's `GameProfile`.

### Why title metadata is copied into identity data

The equipped identity stores not only `titleId`, but also a snapshot of visible title text/color/font/glyph data. This lets display formatting remain cheap and operate directly from player-attached synced state.

When the registry changes, `TagvynService.refreshAllTitleSnapshots`/`refreshTitleSnapshot` updates that snapshot. If the equipped title ID no longer exists, the player's title is cleared.

## Service layer

`service/TagvynService` owns the core nickname/title business rules:

- nickname normalization and validation;
- change-limit accounting;
- nickname clear/set operations;
- title equip/clear operations;
- online title snapshot refresh;
- display/tab refresh and attachment synchronization.

`service/NeoForgeTagvynApi` adapts those internal services to the public API.

Keep authorization decisions outside low-level formatting code. GUI/network handlers should validate permissions before calling privileged mutations.

## Display layer

`display/TagvynDisplay` produces the visible `Component` from synced `IdentityData`.

The order is:

```text
[image glyph if present] [text title if present] nickname-or-vanilla-name
```

Text titles are rendered in brackets, for example:

```text
[FOUNDER] Aenco
```

If an image title is present, the glyph is rendered with its configured font and color before the text title.

Tagvyn deliberately does not alter account authentication identity. Anything that resolves players through the real username/UUID/GameProfile should continue to do so.

## Title registry

`title/TitleRegistry` is the server-side registry for title definitions.

Persistent definitions are stored in:

```text
config/tagvyn/titles.json
```

The registry supports:

- load from disk;
- lookup/list;
- persistent registration/update;
- persistent deletion;
- uploaded-image title registration;
- automatic private-use glyph allocation.

The registry writes immediately when `register` or `remove` succeeds. It rolls in-memory changes back when persistence fails.

## Uploaded PNG storage

`title/TitleImageStore` stores PNG files under:

```text
config/tagvyn/images/<title-id>.png
```

The current server validation enforces a valid PNG signature/IHDR, maximum size of 512 KiB, and maximum dimensions of 256×256.

Uploaded image titles use Tagvyn's internal font ID:

```text
tagvyn:uploaded
```

and a private-use Unicode glyph allocated from the BMP private-use range. External integrations should use the public `registerImageTitle` API rather than manually allocating these glyphs.

## Client-generated resource pack

`client/TagvynClientResourcePack` owns the client-side generated resources for uploaded title images.

The server sends metadata and PNG bytes; the client writes/updates the generated title resources and reloads resources only when the generated contents actually change. This avoids unnecessary full resource reloads on ordinary reconnects.

The generated resource pack exists because most Minecraft display surfaces accept text Components, not arbitrary server-provided image objects. Converting uploaded PNGs into bitmap-font glyph resources lets the same title image participate in TAB/display-name/name-tag rendering.

## Admin GUI architecture

The admin workflow is intentionally GUI-first.

The root `/tagvyn` command opens one of two paths:

- operator: admin dashboard;
- normal player: own nickname screen.

Admin screens send compact action payloads to the server. The server re-checks operator permissions and validates all input before mutating state. A client opening or forging a management payload is not sufficient authorization.

Current major client screens are:

| Screen | Purpose |
| --- | --- |
| `AdminDashboardScreen` | Entry point for operator management |
| `PlayerManagerScreen` | Search/select online players and manage nickname/title state |
| `TitleManagerScreen` | Search/edit/create/delete titles, upload PNGs, create title items |
| `NicknameScreen` | Player nickname editing |

Player/title manager UIs use scrolling rather than assuming all translated controls fit into one viewport.

## Networking

`network/TagvynNetwork` handles nickname/admin/title/image payloads. `network/TitleItemNetwork` handles title-item related network registration.

The current main network protocol string is `4`. It is an internal compatibility boundary between Tagvyn clients and servers and should not be treated as the same thing as `TagvynAPI.API_VERSION`.

See [Networking](Networking.md) for payload groups and versioning guidance.

## Title items

`item/TitleItemService` represents a title token as a vanilla Name Tag carrying Tagvyn-specific `CUSTOM_DATA`.

The custom data includes a marker and stable title ID. Visible item name/lore can change by locale, but redemption resolves the current title definition by ID.

A successful survival-mode redemption equips the title and consumes one item. Creative-mode redemption does not consume it. Missing/deleted titles and already-equipped titles do not consume the item.

## Server authority

The server is authoritative for:

- nickname validation and change limits;
- operator-only admin operations;
- title registry contents;
- PNG validation/storage;
- title item redemption;
- equipped identity state.

Client-side checks exist for UX, not security.

## What is public vs internal

The intended stable boundary is:

```text
dev.aenco.tagvyn.api.*
```

Everything else should currently be considered implementation detail. If another mod needs a capability that is only available through an internal class, prefer extending the public API rather than making the integration depend on internals.
