# Settings

[English](SETTINGS.md) | [简体中文](SETTINGS.zh-CN.md)

Press **F10**, edit the values, then select **Save & apply**. Changes take effect immediately and are saved to `config/autofish-recovery.properties`. **Reset values** fills in the defaults; select **Save & apply** to apply them. It preserves the current fishing toggle. **Back** or **Esc** closes the screen; **Tab** moves between fields.

## Timing

| Setting | Default | Range | When it expires |
|---|---:|---:|---|
| Recast delay | 500 ms | 100–10000 ms | Allows another cast after reeling, once the previous hook has disappeared. |
| Cast retry | 5000 ms | 2000–60000 ms | Retries a cast if no hook has appeared. It does not postpone bites on an existing hook. |
| Startup delay | 5000 ms | 1000–60000 ms | Allows the first cast after enabling fishing or regaining a valid player, world and rod. |
| No-bite timeout | 10000 ms | 6000–600000 ms | Reels an in-water hook if its age reaches the limit without a detected bite. |
| Dry-hook timeout | 20000 ms | 10000–60000 ms | Reels a hook outside water if its age reaches this separate limit. |
| Old-hook cleanup | 3000 ms | 1000–10000 ms | Clears a stale client reference to a hook already reeled in, if it has not disappeared normally. |

A detected bite is reeled immediately on the next client tick; the recast and startup timers do not add a delay to that bite. After a reel, the mod waits for the old hook to disappear before recasting. The cleanup timeout is independent of the recast delay.

Adjust timing for the server you play on. A short no-bite timeout can reel before a fish arrives. Short recast delays can cause ignored casts on laggy servers or servers that limit item-use frequency.

## Detection and protection

| Setting | Default | Range or behavior |
|---|---:|---|
| Motion threshold | 0.05 | 0.01–1.0; downward hook motion used as a bite signal. |
| Sound radius | 1 block | 0.1–5 blocks; accepts bite sounds near the player's own hook. |
| Sound detection | On | Uses `entity.bobber.splash` in addition to hook motion. |
| Rod protection | On | Stops when a damageable rod has 2 or fewer durability points left. |
| Fishing | Off on first use | The chosen state is saved and reused after restarting the client. |

Lower motion thresholds can react to ordinary hook movement. A larger sound radius can include nearby players' bite sounds. The closest-hook check reduces that risk; overlapping hooks can still be ambiguous.

Sound detection listens to a game event, not microphone or speaker audio. Muting sound through the operating system does not affect that event. In-game zero volume can bypass parts of Minecraft's sound handling; keep the relevant sliders above zero when relying on sound detection. Hook-motion detection remains available when no bite sound event is received.

## GUI and background use

Chat, inventory, container menus and the settings screen can remain open while fishing on a running multiplayer world. The mod does not activate the Minecraft window or recapture the mouse.

For background use, configure Minecraft to keep running when unfocused. A paused single-player world cannot produce fish. Disconnecting, dying or entering spectator mode temporarily prevents rod use; normal fishing resumes after a valid player and world are available and the rod is equipped again.
