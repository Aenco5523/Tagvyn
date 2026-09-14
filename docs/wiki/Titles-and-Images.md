# Titles and Images

This page documents Tagvyn's title model, persistent title registry, uploaded PNG handling, generated glyphs, and title items.

## Title IDs

Every title has a stable ID. IDs are normalized to lower-case and must match:

```text
[a-z0-9_.-]{1,64}
```

Use IDs as stable references in integrations, stored permissions, rewards, or external databases. Visible text/color/image metadata may change while the ID remains the same.

Examples:

```text
founder
vip
staff.mod
event-winner-2026
```

## Title definition

The public `TagvynTitle` model contains:

```java
String id;
String text;
int color;
String imageFont;
String imageGlyph;
```

A title can be:

- text-only;
- image-only through a complete font/glyph pair;
- image + text.

A definition is invalid if only one half of the image font/glyph pair is provided. A title must have either non-blank text or a complete image pair.

Colors are 24-bit RGB values (`0xRRGGBB`).

## Persistent registry

The current NeoForge implementation stores title definitions at:

```text
config/tagvyn/titles.json
```

A typical text title looks like:

```json
{
  "id": "founder",
  "text": "FOUNDER",
  "color": "#FFB347"
}
```

An uploaded PNG title is represented internally with an uploaded-image marker and an allocated glyph:

```json
{
  "id": "winner",
  "text": "WINNER",
  "color": "#55FFFF",
  "image": {
    "uploaded": true,
    "glyph": "<private-use-character>"
  }
}
```

Do not rely on the exact JSON shape as a cross-loader public API. For integrations, use `TagvynApi` methods instead. The file format is primarily an implementation/storage format.

## Registering text titles through the API

```java
TagvynApi api = TagvynAPI.get();

boolean ok = api.registerTitle(
        new TagvynTitle(
                "moderator",
                "MOD",
                0x55FF55,
                "",
                ""
        ),
        false
);

if (ok) {
    api.refreshTitles(server);
}
```

The `overwrite` flag controls whether an existing title with the same normalized ID can be replaced.

## Uploaded PNG titles

The preferred image API is:

```java
boolean ok = api.registerImageTitle(
        "champion",
        "CHAMPION",
        0xFFD700,
        pngBytes,
        false
);
```

Tagvyn owns the storage, glyph allocation, synchronization, and client resource generation for this path.

### Validation limits

Current server-side PNG validation requires:

| Rule | Limit |
| --- | --- |
| Format | PNG |
| Maximum bytes | 512 KiB |
| Width | 1–256 px |
| Height | 1–256 px |
| Header | valid PNG signature + IHDR |

Validation is intentionally repeated on the server even when the admin GUI has already checked the file.

### Storage

Uploaded PNGs are stored server-side at:

```text
config/tagvyn/images/<title-id>.png
```

Writes use a temporary file and attempt an atomic replace first, with a normal replace fallback.

If API registration of the title definition fails after the PNG was saved, Tagvyn attempts to restore the previous PNG (or removes the new one when there was no previous image).

## Internal uploaded-image font

Uploaded images currently use:

```text
tagvyn:uploaded
```

Tagvyn allocates one code point from the Basic Multilingual Plane private-use range:

```text
U+E000 .. U+F8FF
```

Existing uploaded-image titles keep their assigned glyph when overwritten. New uploaded-image titles receive the first unused glyph in the range.

This allocation scheme is internal. External mods should not manually reserve Tagvyn's private glyphs.

## Why an image becomes a glyph

Minecraft's TAB entries, display names, name tags, and similar surfaces are text `Component` based. They do not provide a generic API for a server to attach an arbitrary PNG next to a name.

Tagvyn therefore turns each uploaded PNG into a bitmap-font resource on the client. The visible title component contains the assigned glyph styled with the generated font. This preserves compatibility with text-based display surfaces while still showing an actual uploaded image.

## Client synchronization flow

At a high level:

```text
server title registry + PNG files
          |
          v
image manifest -> client
          |
          v
required PNG data -> client
          |
          v
generated Tagvyn resource pack
          |
          v
resource reload only if generated contents changed
          |
          v
glyph can render in Components
```

The server remains authoritative for which images/titles exist. Clients only render synchronized assets.

## External font/glyph titles

The public `registerTitle` method also allows an advanced title with an externally provided font and glyph:

```java
new TagvynTitle(
        "example_badge",
        "BADGE",
        0xFFFFFF,
        "examplemod:badges",
        "\uE101"
)
```

Tagvyn will format this glyph, but it does not distribute the external font resource. All clients must already have that font through the supplying mod/resource pack.

For ordinary server-uploaded PNGs, use `registerImageTitle`.

## Equipping titles

```java
boolean ok = api.setTitle(serverPlayer, "founder");
```

The title must exist in the registry. On success, the current visible title data is copied into the player's synced identity attachment.

If title metadata later changes, call:

```java
api.refreshTitles(server);
```

to update online player snapshots and image assets.

## Removing titles

```java
boolean removed = api.unregisterTitle("founder");
if (removed) {
    api.refreshTitles(server);
}
```

For uploaded-image titles, removal also deletes the stored PNG. Refreshing online snapshots clears the title from players whose equipped ID no longer exists.

## Title items

Tagvyn can represent a title as a special vanilla Name Tag.

The item stores Tagvyn custom data containing:

```text
tagvyn_title_item = true
tagvyn_title_id = <stable title id>
```

The visible item name/lore are localized and decorative; the stable ID is the authoritative link.

On redemption:

1. verify it is a Tagvyn title item;
2. resolve the current title definition by ID;
3. refuse if the title was deleted;
4. refuse without consumption when already equipped;
5. equip the title;
6. consume one item unless the player has Creative instabuild.

Tagvyn title Name Tags are blocked from normal mob-renaming behavior.

## Recommended integration strategy

If another mod awards titles, store **Tagvyn title IDs**, not copies of visible text/color/glyph data. Resolve the current title through `getTitle` when needed and call `setTitle` to equip it.

If another mod creates dynamic image titles, use `registerImageTitle` and batch `refreshTitles(server)` after the changes. Avoid touching `titles.json`, the image directory, or private-use glyph allocation directly.
