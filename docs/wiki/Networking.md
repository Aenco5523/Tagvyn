# Networking

Tagvyn uses NeoForge play payloads for GUI actions, nickname changes, administrator operations, and uploaded title-image synchronization.

This page documents the current implementation so maintainers and port authors understand the trust boundaries. Other mods should normally use the public `TagvynApi` instead of sending Tagvyn's internal payloads directly.

## Protocol version

The current main payload registrar uses:

```text
PROTOCOL_VERSION = "4"
```

This is an internal client/server network compatibility version. It is separate from:

```text
TagvynAPI.API_VERSION = "2"
```

The network protocol can change because of payload shape/UI changes without necessarily changing the public Java API, and vice versa.

## Server authority

Every client-to-server management action is treated as a request, not as trusted state.

Handlers enqueue work onto the appropriate game thread and validate the acting player on the server. Operator management handlers require the server-side operator check before they change player/title state.

Never move permission checks exclusively into a GUI. A malicious or modified client can construct packets without using the intended screen.

## Client-bound payload groups

The main networking layer currently sends these categories to clients:

| Payload | Purpose |
| --- | --- |
| `OpenNicknameScreenPayload` | Opens nickname editor with current nickname and server validation limits |
| `OpenAdminDashboardPayload` | Opens operator dashboard |
| `OpenPlayerManagerPayload` | Sends online-player summaries and title IDs for player management |
| `OpenTitleManagerPayload` | Sends title summaries for the title editor |
| `TitleImageManifestPayload` | Declares uploaded image titles and their assigned glyphs |
| `TitleImageDataPayload` | Sends PNG bytes for one uploaded image title |
| `TitleImageSyncCompletePayload` | Signals end of the current image synchronization pass |

Client screen creation is handled through `TagvynClientNetworking`.

On a dedicated server, client-bound payload types are still registered with no-op handlers so protocol registration stays valid without loading client UI classes as runtime handlers.

## Server-bound payload groups

Current server requests include:

| Payload | Purpose |
| --- | --- |
| `SubmitNicknamePayload` | Submit the current player's nickname |
| `AdminDashboardActionPayload` | Navigate admin pages / request reload |
| `AdminPlayerActionPayload` | Set/clear nickname, reset count, set/clear title for an online player |
| `CreateTextTitlePayload` | Create a text title |
| `UpdateTitlePayload` | Update existing title text/color while preserving image metadata |
| `UploadImageTitlePayload` | Upload/replace a title PNG and metadata |
| `DeleteTitlePayload` | Delete a title |

Title-item networking is registered separately in `TitleItemNetwork`.

## Nickname submission

The client receives server-defined constraints in `OpenNicknameScreenPayload`, including minimum length, maximum length, whether spaces are allowed, and remaining changes.

Those values are for UI feedback only. When `SubmitNicknamePayload` arrives, the server calls `TagvynService.setNickname` and validates the nickname again.

Operators submitting their own nickname through Tagvyn's flow bypass the normal change limit; ordinary players do not.

## Admin authorization

The main operator check uses vanilla command permission level `2`.

Examples of privileged requests that require this check server-side:

- opening player/title management screens;
- editing another player's nickname;
- resetting nickname-change counts;
- assigning/clearing another player's title;
- creating/updating/deleting titles;
- uploading PNG titles;
- reloading title data.

Do not expose an integration that sends these internal packets as a substitute for implementing authorization in your own server-side code.

## Player manager data

When opening the player manager, the server currently builds a sorted snapshot of online players containing:

```text
real Minecraft username
Tagvyn nickname
equipped title ID
nickname change count
remaining nickname changes
```

The manager searches by real username and Tagvyn nickname on the client, but server actions target the selected player by authenticated `GameProfile` name among currently online players.

This is another reason not to confuse a Tagvyn nickname with a Minecraft account identifier.

## Uploaded image synchronization

For one player, the current high-level sync is:

```text
TitleImageManifestPayload
TitleImageDataPayload (one per uploaded title with a valid stored PNG)
...
TitleImageSyncCompletePayload
```

The manifest contains title ID + assigned image glyph metadata. PNG data comes from `TitleImageStore`.

The client collects the synchronization state, updates Tagvyn's generated resources, then finalizes/reloads only when generated resource contents actually changed.

`TagvynApi.refreshTitles(server)` synchronizes uploaded images to all connected players after refreshing online title snapshots.

## Payload size and PNG safety

`UploadImageTitlePayload` is only a transport container. The server still calls `TitleImageStore.validate` before accepting the PNG.

Current image rules are documented in [Titles and Images](Titles-and-Images.md). Do not trust file extension, GUI-side checks, or packet length alone.

## Versioning guidance for maintainers

Bump the internal network protocol when an incompatible payload registration/codec change means old Tagvyn clients and new Tagvyn servers should no longer negotiate as compatible.

Examples include:

- adding/removing/reordering codec fields without backward compatibility;
- changing the meaning of an existing payload field;
- replacing a required client/server synchronization sequence;
- changing management payload formats in an incompatible way.

A new UI layout that uses the same payload shapes does not inherently require a protocol bump.

## Guidance for other mods

Do not depend on Tagvyn payload class names or protocol internals unless you are deliberately writing a version-specific compatibility bridge.

For normal integration:

```java
TagvynApi api = TagvynAPI.get();
```

and call the public server-side methods directly. This avoids coupling your mod to Tagvyn's current GUI/network implementation.
