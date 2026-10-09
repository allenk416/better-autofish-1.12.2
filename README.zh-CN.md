# Better AutoFish 1.12.2

[English](README.md) | [简体中文](README.zh-CN.md)

面向 Minecraft 1.12.2 的客户端自动钓鱼模组，为多人服务器使用增加了自动起竿、重连恢复和游戏内设置等功能。基于 [NorthWestWind 的 AutoFish](https://github.com/North-West-Wind/AutoFish) 开发。

## 功能

- 开启自动钓鱼后，没有鱼钩时主动抛竿。
- 重新连接或复活后恢复钓鱼，并保留开关状态。
- 通过鱼钩下沉和可选的咬钩声音检测，处理连续快速上钩。
- 打开聊天、背包、容器或菜单时，以及游戏处于后台时继续钓鱼。
- 恢复卡住的鱼钩，并在鱼竿仅剩最后 2 点耐久时停止使用。
- 在游戏内调整时序和检测参数，支持英文与简体中文。

## 使用环境

- Minecraft **1.12.2** + Forge；编译使用 **14.23.5.2860**。Cleanroom 客户端可使用其 Forge 兼容层。
- 安装在**客户端**，服务器无需安装。
- 主手或副手持有鱼竿，视角朝向水面。

## 安装与使用

1. 从 [Releases](https://github.com/allenk416/better-autofish-1.12.2/releases) 下载模组 `.jar`。
2. 退出 Minecraft，把旧 AutoFish JAR 移出当前实例的 `mods` 文件夹。
3. 将 `Better-AutoFish-1.12.2-1.0.6.jar` 放入该文件夹，再启动游戏。
4. 按 **F7** 开关自动钓鱼，按 **F10** 打开设置。按键可在**选项 → 控制**中修改。
5. 根据实际游玩的服务器调整参数，再点击**保存并生效**。可以保持设置界面打开继续挂机。

模组跟随 Minecraft 的语言设置显示：选择 **English (US)** 显示英文，选择**简体中文**显示中文。同一个 JAR 包含两种语言，其他语言使用英文作为回退。

配置保存在当前 Minecraft 实例的 `config/autofish-recovery.properties` 中。保留已有文件名，以便继续使用之前的参数。

服务器重启后，客户端完成重连、玩家再次手持鱼竿时会恢复钓鱼。自动重连与登录由客户端或其他模组提供。后台钓鱼需要游戏世界继续运行；单人世界被暂停时，钓鱼也不会推进。

参数默认值、超时行为和声音检测说明见[参数指南](docs/SETTINGS.zh-CN.md)。请在允许自动钓鱼的服务器使用。

## 来源与许可

原 AutoFish 作者为 **NorthWestWind**；本衍生项目由 **allenk416** 维护，以 **GPL-3.0** 发布。许可、来源和开发致谢见 [LICENSE](LICENSE) 与 [NOTICE.md](NOTICE.md)。源码构建方法见 [BUILDING.md](BUILDING.md)。
