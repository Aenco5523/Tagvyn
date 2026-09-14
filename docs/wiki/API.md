# Public API

Tagvyn exposes its supported integration surface through `dev.aenco.tagvyn.api`.

## Entry point

```java
import dev.aenco.tagvyn.api.TagvynAPI;
import dev.aenco.tagvyn.api.TagvynApi;

if (TagvynAPI.isReady()) {
    TagvynApi api = TagvynAPI.get();
}
```

`TagvynAPI.API_VERSION` is currently `"2"`.

`TagvynAPI.get()` throws `IllegalStateException` if Tagvyn has not initialized its API yet. Use `isReady()` when your code can run before normal mod initialization is complete.

## Read a player's identity

```java
IdentitySnapshot identity = TagvynAPI.get().getIdentity(player);

String nickname = identity.nickname();
String titleId = identity.titleId();
boolean hasNickname = identity.hasNickname();
boolean hasTitle = identity.hasTitle();
```

`IdentitySnapshot` is an immutable view containing:

| Field | Meaning |
| --- | --- |
| `nickname` | Stored Tagvyn nickname, or an empty string |
| `nicknameChanges` | Number of counted nickname changes |
| `titleId` | Equipped title ID, or an empty string |
| `titleText` | Snapshot of the equipped title text |
| `titleColor` | 24-bit RGB integer |
| `imageFont` | Font resource ID used by image titles, or empty |
| `imageGlyph` | Glyph used by image titles, or empty |

For simple name lookup, prefer:

```java
String shownName = TagvynAPI.get().getEffectiveName(player);
```

This returns the Tagvyn nickname when one exists, otherwise the real Minecraft profile name.

## Nickname mutations

Nickname writes require a `ServerPlayer` and should be executed on the logical server.

```java
NicknameUpdateResult result = TagvynAPI.get().setNickname(
        serverPlayer,
        "Aenco",
        false
);

if (!result.success()) {
    switch (result.failure()) {
        case INVALID -> {
            // Failed configured validation rules.
        }
        case LIMIT_REACHED -> {
            // Player has no normal nickname changes remaining.
        }
        case NONE -> {
        }
    }
}
```

The third argument, `bypassLimit`, controls whether the normal nickname-change counter/limit is bypassed. It should only be set to `true` when your integration has already made an appropriate authorization decision.

Clear a nickname with:

```java
NicknameUpdateResult result = TagvynAPI.get().clearNickname(serverPlayer, false);
```

Read remaining normal changes with:

```java
int remaining = TagvynAPI.get().getRemainingNicknameChanges(player);
```

A return value of `-1` means unlimited.

### Validation behavior

Tagvyn normalizes nickname input by trimming leading/trailing whitespace and collapsing repeated ASCII spaces. Validation then applies the configured minimum/maximum code-point length and optional space restriction. The section sign (`§`) and ISO control characters are rejected.

Do not duplicate these checks in another mod as the source of truth. You may pre-validate for UX, but always use the Tagvyn API on the server and handle its result.

## Equip and clear titles

```java
boolean equipped = TagvynAPI.get().setTitle(serverPlayer, "founder");
```

`setTitle` returns `false` when the title ID does not exist.

```java
TagvynAPI.get().clearTitle(serverPlayer);
```

When a title is equipped, Tagvyn stores a snapshot of its visible metadata on the player's identity attachment. Refreshing titles updates online players from the registry and clears equipped titles whose definitions no longer exist.

## Read registered titles

```java
Optional<TagvynTitle> founder = TagvynAPI.get().getTitle("founder");
Collection<TagvynTitle> all = TagvynAPI.get().getTitles();
```

A `TagvynTitle` contains:

```java
new TagvynTitle(
        "vip",      // id
        "VIP",      // visible text
        0xFFD700,   // RGB
        "",         // imageFont
        ""          // imageGlyph
);
```

Color values are masked to 24-bit RGB.

## Register a text title

```java
TagvynApi api = TagvynAPI.get();

boolean created = api.registerTitle(
        new TagvynTitle(
                "vip",
                "VIP",
                0xFFD700,
                "",
                ""
        ),
        false
);
```

The last argument is `overwrite`.

Title IDs are normalized to lower-case and must match:

```text
[a-z0-9_.-]{1,64}
```

A normal title must contain visible text unless it has a complete image font/glyph pair.

`registerTitle` persists the definition to Tagvyn's title registry. If you change registry data while players are online, call `refreshTitles(server)` after the mutation so equipped snapshots and uploaded-image assets are refreshed.

## Register a PNG image title

For ordinary integrations, this is the preferred image-title API:

```java
byte[] pngBytes = ...;

boolean created = TagvynAPI.get().registerImageTitle(
        "event_winner",
        "WINNER",
        0x55FFFF,
        pngBytes,
        false
);

if (created) {
    TagvynAPI.get().refreshTitles(server);
}
```

The PNG is validated and persisted by Tagvyn. Current limits are:

- PNG format with a valid PNG signature/IHDR header
- maximum file size: 512 KiB
- width: 1–256 px
- height: 1–256 px

Tagvyn assigns an internal private-use glyph and uses its generated client resource pack to render the uploaded image. Integrations should not allocate uploaded-image glyphs themselves.

Read the stored PNG with:

```java
Optional<byte[]> png = TagvynAPI.get().getTitleImage("event_winner");
```

The returned byte array is a defensive clone.

## Advanced font/glyph titles

`registerTitle(TagvynTitle, overwrite)` can also represent a pre-existing external bitmap-font/glyph title by supplying both `imageFont` and `imageGlyph`.

```java
TagvynTitle custom = new TagvynTitle(
        "partner",
        "PARTNER",
        0xFFFFFF,
        "example:icons",
        "\uE100"
);
```

This is an advanced path. Tagvyn does not magically provide an external mod's font resource. The referenced font/glyph must already exist on clients. For a normal PNG that Tagvyn should store and distribute, use `registerImageTitle` instead.

## Unregister a title

```java
boolean removed = TagvynAPI.get().unregisterTitle("vip");
if (removed) {
    TagvynAPI.get().refreshTitles(server);
}
```

Removing an uploaded image title also removes its server-side PNG. After `refreshTitles`, online players using the removed title are cleared.

## Refresh title state and image assets

```java
TagvynAPI.get().refreshTitles(server);
```

This does two things:

1. refreshes online player title snapshots against the current registry;
2. synchronizes uploaded image-title assets to connected clients.

Call it after a group of title registry changes rather than after every single change when batching operations.

## Format a display name

```java
Component formatted = TagvynAPI.get().formatDisplayName(
        player,
        player.getName(),
        true
);
```

When `includeTitle` is `true`, the result can contain the title image glyph, text title, and effective player name. When it is `false`, only the nickname/fallback name portion is produced.

Use this helper when another mod needs a Component consistent with Tagvyn's display style.

## Threading and authority

Player mutations and registry changes should be performed on the server thread. Do not trust a client to authorize `bypassLimit`, operator actions, title creation, or image upload. Tagvyn's own GUI packets re-check permissions and validation on the server; integrations should follow the same rule.

## Compatibility rule

Depend on `dev.aenco.tagvyn.api`, not classes in `service`, `title`, `network`, `data`, or `client`. Internal classes document how Tagvyn works but are not the stable API contract.
