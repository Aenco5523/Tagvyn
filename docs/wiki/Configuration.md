# Configuration

Tagvyn registers a NeoForge common config through `TagvynConfig.SPEC`. This page documents the current logical settings. Exact file naming/location is loader-specific and may change in future ports, so integrations should not hardcode a config path unless they target a specific loader implementation.

## Nickname settings

### `nickname.changeLimit`

Default: `3`

Range: `-1` to `100000`

Controls how many counted nickname changes a normal player may make.

- `-1` means unlimited.
- Server operators bypass this limit in Tagvyn's own admin/player flows.
- API callers can choose whether to bypass the limit through the `bypassLimit` argument.

A nickname change is counted when a non-bypassing mutation changes the stored nickname value. Clearing an existing nickname also counts for non-bypassing callers.

### `nickname.minLength`

Default: `1`

Range: `1` to `64`

Minimum nickname length measured in Unicode code points.

### `nickname.maxLength`

Default: `24`

Range: `1` to `64`

Maximum nickname length measured in Unicode code points.

The service uses the lower of configured min/max as the effective minimum and the higher as the effective maximum, so an accidentally reversed pair does not make all nicknames invalid.

### `nickname.allowSpaces`

Default: `true`

When `false`, any Unicode whitespace in the normalized nickname causes validation to fail.

## Display settings

### `display.showTitleInDisplayName`

Default: `true`

Controls title inclusion in the general player display name path. This affects places where Minecraft/NeoForge uses the player's display name, such as chat/name-tag/command-feedback integrations wired through that display-name path.

### `display.showTitleInTab`

Default: `true`

Controls whether titles are included in the TAB player-list name.

## Nickname normalization and validation

Before validation, `TagvynService` normalizes input by:

1. converting `null` to an empty string;
2. trimming leading/trailing whitespace;
3. collapsing repeated ASCII spaces (`" +"`) into one ASCII space.

A nickname is then rejected when any of the following is true:

- its Unicode code-point length is outside the configured min/max range;
- spaces/whitespace are disabled and the nickname contains whitespace;
- it contains the section sign `§`;
- it contains an ISO control character.

Other mods should treat the server-side Tagvyn result as authoritative. Client-side replication of these rules is useful only for immediate UX feedback.

## Remaining-change semantics

`TagvynApi.getRemainingNicknameChanges(player)` returns:

```text
-1  -> unlimited
0+  -> remaining counted changes
```

For finite limits, the value is calculated as:

```text
max(0, configuredLimit - nicknameChanges)
```

## Operator permissions

Tagvyn's own administrator GUI uses vanilla command permission level `2` as the operator check. Administrative branches are not intended for ordinary players.

The public API does not automatically infer your mod's authorization model. For example, calling:

```java
api.setNickname(player, value, true);
```

explicitly requests a limit bypass. Your integration is responsible for deciding who is allowed to do that.

## Configuration compatibility advice

If you are writing an integration, prefer behavior-based API calls over reading Tagvyn's config directly. For example, call `getRemainingNicknameChanges`, `setNickname`, and handle the returned result instead of reimplementing change-limit calculations.

Direct config access is an internal implementation detail and may differ between future Fabric/Forge/NeoForge ports even when the public Tagvyn API remains conceptually the same.
