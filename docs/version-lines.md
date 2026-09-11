# 单主干与版本适配策略

## 总原则

- `main` 是唯一开发和发布入口。
- `core` 保存与 Minecraft API 无关的业务逻辑。
- 每个 Minecraft/Loader 版本由一个独立 adapter 编译。
- adapter 固定自己的 Gradle、Java、映射和 loader 版本。
- 不在 Core 中使用版本号判断或运行时反射处理 API 差异。

## 当前 target

| Target | Minecraft | Loader | Java | 状态 |
|---|---|---|---:|---|
| `forge-1.12.2` | 1.12.2 | Forge | 8 | 已迁移并验证 |
| `forge-1.13.2` | 1.13.2 | Forge | 11 | 已迁移并验证 |
| `forge-1.14.4` | 1.14.4 | Forge | 11 | 已迁移并验证 |
| `forge-1.15.2` | 1.15.2 | Forge | 11 | 已迁移并验证 |
| `forge-1.16.5` | 1.16.5 | Forge | 8 | 已迁移并验证 |
| `forge-1.18.2` | 1.18.2 | Forge | 17 | 已迁移并验证 |
| `forge-1.19.2` | 1.19.2 | Forge | 17 | 已迁移并验证 |
| `forge-1.20.1` | 1.20.1 | Forge 47.4.18 | 17 | 已迁移并验证 |
| `neoforge-1.20.2` | 1.20.2 | NeoForge 20.2.93 | 17 | 已迁移并验证 |
| `neoforge-1.20.4` | 1.20.4 | NeoForge 20.4.251 | 17 | 已迁移并验证 |
| `neoforge-1.20.6` | 1.20.6 | NeoForge 20.6.139 | 21 | 已迁移并验证 |
| `neoforge-1.21.4` | 1.21.4 | NeoForge 21.4.157 | 21 | 已迁移并验证 |

目标配置位于 `gradle/targets.properties`。

## 构建

```powershell
.\gradlew.bat printTargets --no-daemon
.\gradlew.bat buildTarget --project-prop target=forge-1.20.1 --no-daemon
.\gradlew.bat buildAll --no-daemon
```

`buildAll` 会按 target 的 Java 属性选择本机 JDK，并顺序调用每个 adapter 的独立 Gradle wrapper。

## 分支退役

旧 `forge/*`、`neoforge/*` 分支只作为迁移来源。所有 target 在主干构建和发布流程稳定后，保留最终 Tag，删除旧分支。
