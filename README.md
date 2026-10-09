# Better AutoFish 1.12.2

[English](README.md) | [简体中文](README.zh-CN.md)

A client-side automatic fishing mod for Minecraft 1.12.2, with features designed for multiplayer server use. Based on [NorthWestWind's AutoFish](https://github.com/North-West-Wind/AutoFish).

## Features

- Automatically casts when fishing is enabled and no hook is present.
- Resumes fishing after reconnecting or respawning, while keeping the enabled setting.
- Reels in fast consecutive bites using hook motion and optional bite-sound detection.
- Continues fishing with chat, inventory, containers or menus open, and while Minecraft is in the background.
- Recovers from stuck hooks and stops before a fishing rod reaches its last two durability points.
- Provides in-game timing and detection settings in English and Simplified Chinese.

## Requirements

- Minecraft **1.12.2** with Forge; compiled against **14.23.5.2860**. Cleanroom clients can use their Forge compatibility layer.
- Install on the **client**. The server does not need this mod.
- A fishing rod in either hand and a view aimed at the water.

## Installation and use

1. Download the mod `.jar` from [Releases](https://github.com/allenk416/better-autofish-1.12.2/releases).
2. Close Minecraft and remove the previous AutoFish JAR from the active `mods` folder.
3. Put `Better-AutoFish-1.12.2-1.0.6.jar` in that folder, then start the game.
4. Press **F7** to enable or disable fishing. Press **F10** to open settings. Both bindings can be changed in **Options → Controls**.
5. Adjust the values as needed for the server you play on, then select **Save & apply**. You can leave the settings screen open while fishing.

The mod follows Minecraft's language setting: **English (US)** or **简体中文**. Both languages are included in the same JAR; other languages fall back to English.

Configuration is saved in `config/autofish-recovery.properties` inside the active Minecraft instance. The existing filename is retained so previous settings can be reused.

After a server restart, fishing resumes once the client has reconnected and the player is holding a rod again. Automatic reconnection and login are supplied by the client or another mod. Background fishing requires the game world to keep running; a paused single-player world will not progress.

For parameter defaults, timeout behavior and sound detection, see the [settings guide](docs/SETTINGS.md). Use automatic fishing where permitted by the server.

## Credits and license

Original AutoFish by **NorthWestWind**; this derivative is maintained by **allenk416** under **GPL-3.0**. See [LICENSE](LICENSE) and [NOTICE.md](NOTICE.md) for the license, source attribution and development acknowledgements. Source build instructions are in [BUILDING.md](BUILDING.md).
