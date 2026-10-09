# Building from source

The build compiles **all mod classes** in `src/main/java` and packages the resources in `src/main/resources`. An existing AutoFish JAR is not required. Compiled code targets **Java 8**.

## Prerequisites

- Windows PowerShell, Python 3.10+ and JDK 17+.
- A Minecraft 1.12.2 client runtime JAR and Forge 14.23.5.2860 JAR, obtained through a Minecraft/Forge installation.
- ASM 9 and ASM Commons 9 JARs, obtained from their published Maven artifacts or a local client library directory.

Game and loader JARs are local build inputs. Do not add them to this repository.

```powershell
.\tools\build.ps1 `
  -MinecraftRuntimeJar 'C:\dependencies\minecraft-1.12.2.jar' `
  -ForgeJar 'C:\dependencies\forge-1.12.2-14.23.5.2860.jar' `
  -AsmLibraryDirectory 'C:\dependencies\asm' `
  -JdkHome 'C:\dependencies\jdk-17'
```

The script extracts Forge's mapping table, maps the runtime/API to SRG names, compiles the source, and runs policy, handler, localization, saved-binding and configuration-save tests. Minecraft fixtures are used for automated checks; they are not included in the output JAR.

Outputs in ignored `dist/`:

- `Better-AutoFish-1.12.2-1.0.7.jar`
- `Better-AutoFish-1.12.2-1.0.7-source.zip`
- `SHA256SUMS-1.0.7.txt`

The source ZIP includes the mod source, resources, build tooling, tests, documentation and license. Release tags should point at the same source snapshot used to build their JAR.

## Versions

Increase the final component of `VERSION` and the version in `src/main/resources/mcmod.info` together for each new build: `1.0.7`, `1.0.8`, and so on. The packager rejects an existing filename. Update the download filename in both READMEs when preparing a release.

This repository uses the PowerShell build above. All current mod source is kept in one place; it has no Gradle build that silently compiles a separate, older implementation.

## 中文说明

构建会编译 `src/main/java` 中的全部模组类，并打包 `src/main/resources` 中的资源，不需要原版 AutoFish JAR。输出为 Java 8 字节码。

准备 PowerShell、Python 3.10+、JDK 17+、Minecraft 1.12.2 客户端运行时 JAR、Forge 14.23.5.2860 JAR，以及 ASM 9/ASM Commons 9。用上方命令传入本地依赖路径。不要把游戏或加载器 JAR 提交到仓库。

脚本会映射运行时/API、编译完整源码并执行策略、处理器、本地化、旧按键迁移和配置保存测试；测试夹具不会打进模组。产物位于被忽略的 `dist/`，发布时将 JAR 与校验文件作为 Release 附件，源码可使用对应 tag 的归档或生成的源码 ZIP。

每次生成新版本，都把 `VERSION` 与 `mcmod.info` 的末位版本号加一，并更新两份 README 的下载文件名。打包程序拒绝覆盖已有版本。
