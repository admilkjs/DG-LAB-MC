# DG-LAB MC

Minecraft 客户端 DG-LAB 控制模组，采用单主干、纯 Java Core 和按版本编译的 adapter 架构。

## 当前结构

| 目录 | 职责 |
|---|---|
| `core/` | 配置、设备协议、规则、安全、波形和纯 Java 测试 |
| `adapters/forge-1.20.1/` | Forge 1.20.1 入口、事件、UI、命令、网络和资源 |
| `adapters/neoforge-1.20.2/` | NeoForge 1.20.2 入口、事件、UI、命令、网络和资源 |
| `gradle/targets.properties` | 已启用版本及构建工具链 |

Core 不依赖 Minecraft、Forge 或 NeoForge。新增版本时新增 adapter，不复制业务代码。

## 本地构建

```powershell
.\gradlew.bat :core:test --no-daemon
.\gradlew.bat buildTarget --project-prop target=forge-1.20.1 --no-daemon
.\gradlew.bat buildAll --no-daemon
```

单版本发布包位于对应 adapter 的 `dist/release/`。根工程的 `buildAll` 会按 `gradle/targets.properties` 顺序构建全部启用版本。

## 增加版本

1. 在 `adapters/` 下创建独立版本工程。
2. 固定该版本的 Minecraft、loader、Java 和 Gradle wrapper。
3. 通过 composite build 依赖 `core`。
4. 只在 adapter 内处理 Minecraft API 差异。
5. 在 `gradle/targets.properties` 增加 target。
6. 先通过 `:core:test`，再通过目标 adapter 的 `buildRelease`。

## 兼容原则

- Core 中禁止 Minecraft 和 loader import。
- Core 不使用版本号分支或运行时反射处理 API 差异。
- UI、事件、命令、网络和资源留在 adapter。
- 修改规则、配置、设备协议、安全或波形逻辑时只修改 Core。

## 发布

每个 Minecraft 版本发布独立 Jar，文件名包含目标版本。所有版本由 `main` 主干和 target 清单构建；旧版本分支在迁移完成后删除，最终版本由 Tag 保留。

详细实施步骤见：[多版本实施计划](superpowers/plans/2026-09-11-dglabmc-multiversion.md)。
