# 版本支持矩阵

版本支持以 `main` 中的 adapter 为准，旧版本分支不再作为维护入口。

| Target | Minecraft | Loader | Java | Core 测试 | Adapter 构建 | 发布状态 |
|---|---|---|---:|---|---|---|
| `forge-1.12.2` | 1.12.2 | Forge | 8 | 已通过 | 已通过 | 已迁移 |
| `forge-1.13.2` | 1.13.2 | Forge | 11 | 已通过 | 已通过 | 已迁移 |
| `forge-1.14.4` | 1.14.4 | Forge | 11 | 已通过 | 已通过 | 已迁移 |
| `forge-1.15.2` | 1.15.2 | Forge | 11 | 已通过 | 已通过 | 已迁移 |
| `forge-1.16.5` | 1.16.5 | Forge | 8 | 已通过 | 已通过 | 已迁移 |
| `forge-1.18.2` | 1.18.2 | Forge | 17 | 已通过 | 已通过 | 已迁移 |
| `forge-1.19.2` | 1.19.2 | Forge | 17 | 已通过 | 已通过 | 已迁移 |
| `forge-1.20.1` | 1.20.1 | Forge 47.4.18 | 17 | 已通过 | 已通过 | 已迁移 |
| `neoforge-1.20.2` | 1.20.2 | NeoForge 20.2.93 | 17 | 已通过 | 已通过 | 已迁移 |
| `neoforge-1.20.4` | 1.20.4 | NeoForge 20.4.251 | 17 | 已通过 | 已通过 | 已迁移 |
| `neoforge-1.20.6` | 1.20.6 | NeoForge 20.6.139 | 21 | 已通过 | 已通过 | 已迁移 |
| `neoforge-1.21.4` | 1.21.4 | NeoForge 21.4.157 | 21 | 已通过 | 已通过 | 已迁移 |

## 发布产物

每个 adapter 生成独立 Jar，文件名包含 mod 版本、loader 和 Minecraft 版本。根工程的 `buildAll` 只构建 target 清单中 `enabled=true` 的版本。
