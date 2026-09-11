# 单主干与版本适配策略

## 总原则

- `main` 是唯一开发和发布入口。
- `core` 保存与 Minecraft API 无关的业务逻辑。
- 每个 Minecraft/Loader 版本由一个独立 adapter 编译。
- adapter 固定自己的 Gradle、Java、映射和 loader 版本。
- 不在 Core 中使用版本号判断或运行时反射兼容 API。

## 当前 target

| Target | Minecraft | Loader | Java | 状态 |
|---|---|---|---:|---|
| `forge-1.20.1` | 1.20.1 | Forge 47.4.18 | 17 | 已迁移并验证 |
| `neoforge-1.20.2` | 1.20.2 | NeoForge 20.2.93 | 17 | 已迁移并验证 |

目标配置位于 `gradle/targets.properties`。

## 构建

```powershell
.\gradlew.bat buildTarget --project-prop target=forge-1.20.1 --no-daemon
.\gradlew.bat buildAll --no-daemon
```

## 后续迁移

旧 `forge/*`、`neoforge/*` 分支只作为迁移来源。版本迁移完成并通过 CI 后，保留最终 Tag，删除分支，把版本信息加入 target 清单。
