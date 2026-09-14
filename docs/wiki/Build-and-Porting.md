# Build and Porting

This page is for Tagvyn maintainers and authorized port developers. Check the repository `LICENSE` before redistributing modified or ported builds.

## Build environment

The current project uses Gradle and Java 21.

Build from the repository root with:

```bash
./gradlew build
```

On Windows:

```powershell
gradlew.bat build
```

The build target is defined in `gradle.properties`, not in this Wiki. Relevant properties include:

```properties
minecraft_version=...
mod_loader=...
mod_version=...
```

Loader-specific dependency/version properties are also kept there.

## Artifact naming

Release JARs follow:

```text
Tagvyn-<loader>-<minecraft-version>-<mod-version>.jar
```

The Gradle base archive name is built from the mod name, loader, and Minecraft version; Gradle appends the mod version.

Do not hardcode a particular Minecraft version or loader into general documentation or integration logic when it can be derived from the build/release instead.

## CI expectations

The GitHub Actions build currently performs, at minimum:

1. repository checkout;
2. Java setup;
3. Gradle setup;
4. language-file validation;
5. build metadata extraction for artifact naming;
6. `gradle build --stacktrace`;
7. artifact upload.

Language validation compares bundled translation files against `en_us` for key parity and `%s` placeholder parity. When adding a translatable string, update every bundled language file or CI should fail.

## JAR license packaging

The build includes the repository `LICENSE` inside the generated JAR as:

```text
META-INF/LICENSE
```

Keep this behavior when adding loaders or changing the build system.

## Current architecture boundary for ports

The most important portability rule is to preserve the public package:

```text
dev.aenco.tagvyn.api
```

Loader-specific details should stay behind implementations/adapters.

A future multi-loader layout should ideally separate concepts like:

```text
common API + domain models
common business rules where practical
loader-specific persistence/network/event/display hooks
loader-specific client resource registration
```

The current source tree is still a single-loader project, so porting may initially require extracting code rather than simply adding another module.

## Areas that are mostly loader-independent

These concepts are good candidates for a common module or near-identical implementations:

- title ID validation/normalization rules;
- nickname normalization/validation semantics;
- public API records/interfaces;
- title registry JSON model;
- PNG validation rules;
- title item logical behavior;
- display formatting rules expressed as Minecraft `Component`s.

Be careful: even apparently simple classes may import loader-specific path/config/event APIs today, so extraction can require small abstractions.

## Areas that are loader-specific today

The current NeoForge implementation relies on loader APIs for areas such as:

- mod bootstrap/registration;
- player attachments and attachment synchronization;
- common config registration;
- event hooks;
- payload registration and distribution;
- physical-client checks;
- generated resource-pack registration;
- display-name/TAB integration hooks.

A Fabric or Forge port should implement equivalent behavior without exposing those loader-specific types through `TagvynApi`.

## Persistence porting contract

Whatever persistence system a loader uses, preserve these user-visible semantics:

- nickname/title identity survives reconnects;
- identity remains associated with the correct player UUID/account identity;
- equipped title metadata is available on the client for rendering;
- death does not unintentionally erase Tagvyn identity;
- title registry changes can refresh online player snapshots.

Do not implement nicknames by replacing `GameProfile` names. Tagvyn's contract is display identity, not authentication identity.

## Networking porting contract

A port does not need to reproduce NeoForge payload classes byte-for-byte, but it should preserve the behavior:

- server-authoritative nickname validation;
- server-authoritative OP checks;
- client GUI open/state payloads;
- player/title manager actions;
- uploaded PNG manifest/data synchronization;
- title-item behavior;
- clear client/server compatibility boundaries.

If different loaders use different wire protocols internally, keep that detail out of the public Java API.

## Image-title porting contract

Uploaded image titles depend on three pieces:

1. server-side validated PNG storage;
2. a stable title-ID-to-glyph mapping;
3. a client resource/font mechanism that lets ordinary text Components render the image.

A port may use a loader-appropriate resource-pack registration mechanism, but the visible behavior should remain equivalent.

Current validation limits are 512 KiB and at most 256×256 pixels. Changing those limits is a behavior change and should be documented.

## API compatibility

The current public API reports:

```java
TagvynAPI.API_VERSION
```

Before making a breaking public API change:

- decide whether compatibility can be preserved with default methods/overloads;
- update API documentation;
- increment the API version when the contract is incompatible;
- update integrations/examples;
- distinguish API-version changes from network-protocol changes.

## Version port workflow

For a new Minecraft version, review at least:

- mappings/name changes;
- Component/style/font APIs;
- player display/TAB APIs;
- persistence APIs;
- networking/codec APIs;
- resource-pack registration/reload APIs;
- item data-component APIs;
- server permission/player lookup APIs;
- client screen/input APIs.

Do not assume compilation success proves visual correctness. Tagvyn has UI layouts, player skin/display interactions, generated resources, and multiple GUI scales that require in-game testing.

## Loader port workflow

For a new loader, first implement a minimal bootstrap and the public `TagvynApi` adapter, then add persistence/display hooks, then networking and GUI/image synchronization. Keeping these layers separate makes it easier to compare behavior against an existing working build.

## Development checklist

Before merging a port or major version update, verify:

- project builds in CI;
- language validation passes;
- JAR name contains loader + Minecraft version + mod version;
- `META-INF/LICENSE` exists in the JAR;
- server and client can connect with matching Tagvyn builds;
- first-join nickname prompt opens the nickname GUI;
- nickname limits behave correctly;
- admin GUI permissions are server-enforced;
- text and PNG titles render in intended display surfaces;
- image synchronization survives reconnects;
- player skins are unaffected;
- title items redeem correctly;
- large player/title lists and high GUI scales remain usable.
