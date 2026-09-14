# Localization

Tagvyn uses Minecraft language JSON files for player-facing UI, messages, and title-item text.

## Bundled languages

The repository currently bundles translations for:

- English (`en_us`)
- Korean (`ko_kr`)
- Japanese (`ja_jp`)
- Simplified Chinese (`zh_cn`)
- Traditional Chinese (`zh_tw`)
- Spanish (`es_es`)
- French (`fr_fr`)
- German (`de_de`)
- Brazilian Portuguese (`pt_br`)

The client language setting selects the visible translation. Translatable Components sent by the server are intentionally resolved on the client where appropriate so each player can see their own language.

## Source of truth

Treat `en_us.json` as the key-set reference. CI validates that every bundled language has the same translation keys and matching `%s` placeholder counts.

When adding a new key:

1. add it to `en_us.json`;
2. add the same key to every bundled language file;
3. preserve placeholder count/order semantics;
4. run the normal build/CI validation.

## Use translatable Components

Prefer:

```java
Component.translatable("tagvyn.message.example", value)
```

over building a user-facing English string in Java.

For GUI labels:

```java
Component.translatable("tagvyn.gui.example.label")
```

For user-facing item names/lore, continue using translatable Components so the same server-issued item can be displayed according to each client's language resources.

## Do not translate stable identifiers

Never localize values that act as identifiers or protocol/storage keys, such as:

- title IDs (`founder`, `vip`, etc.);
- action IDs sent in internal packets;
- NBT/data-component marker keys;
- API method names;
- config keys;
- resource locations.

Translate only user-facing text.

## GUI design and translation length

Do not assume English or Korean button text is representative of every language. German/French/Spanish labels can be substantially longer.

Tagvyn's current admin screens use scrolling and width-aware clipping to avoid forcing every translated control into a single viewport. New screens should follow the same rule:

- prefer a scrollable management panel for long forms;
- keep essential navigation such as Back fixed where practical;
- avoid overly narrow side-by-side buttons for long actions;
- clip non-critical preview/list text with ellipsis rather than drawing outside a panel;
- test with multiple GUI scales and at least one language with longer strings.

## Placeholder safety

If English contains:

```json
"tagvyn.message.example": "Updated %s for %s"
```

other languages must preserve the two substitution placeholders expected by code.

Do not delete a placeholder because a literal translation sounds natural without it. Either rewrite the sentence to use all required values or update code and all translations together as one API/UI change.

## Adding a new language

Create the appropriate Minecraft locale file under:

```text
src/main/resources/assets/tagvyn/lang/<locale>.json
```

Copy the full current key set from `en_us` and translate every player-facing value. Then ensure the CI validation script includes/discovers the new locale as intended.

## First-join prompt

The first-join nickname prompt is a translatable Component. Keep it translatable rather than selecting a language server-side; the server normally does not know or need to enforce the client's UI language for this message.
