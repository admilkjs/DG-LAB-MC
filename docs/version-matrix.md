# 版本支持矩阵

## 当前矩阵

| Minecraft | Loader | 构建 JDK | 分支 | 本地构建 | 发布状态 |
|-----------|--------|----------|------|----------|----------|
| 1.12.2 | Forge | 8 | `forge/1.12.2` | 已验证 | 待发布 |
| 1.13.2 | Forge | 11 | `forge/1.13.2` | 已验证 | 待发布 |
| 1.14.4 | Forge | 11 | `forge/1.14.4` | 已验证 | 待发布 |
| 1.15.2 | Forge | 11 | `forge/1.15.2` | 已验证 | 待发布 |
| 1.16.5 | Forge | 8 | `forge/1.16.5` | 已验证 | 待发布 |
| 1.18.2 | Forge | 17 | `forge/1.18.2` | 已验证 | 待发布 |
| 1.19.2 | Forge | 17 | `forge/1.19.2` | 已验证 | 待发布 |
| 1.20.1 | Forge | 17 | `forge/1.20.1` | 已验证 | 待发布 |
| 1.20.2 | NeoForge | 17 | `neoforge/1.20.2` | 已验证 | 待发布 |
| 1.20.4 | NeoForge | 17 | `neoforge/1.20.4` | 已验证 | 待发布 |
| 1.20.6 | NeoForge | 21 | `neoforge/1.20.6` | 已验证 | 待发布 |
| 1.21.4 | NeoForge | 21 | `neoforge/1.21.4` | 已验证 | 待发布 |

## 统一构建命令

```powershell
.\gradlew.bat clean build generatePvpPunishConfig buildRelease --no-daemon
```

## 构建 JDK

- Java 8：`forge/1.12.2`、`forge/1.16.5`
- Java 11：`forge/1.13.2`、`forge/1.14.4`、`forge/1.15.2`
- Java 17：`forge/1.18.2`、`forge/1.19.2`、`forge/1.20.1`、`neoforge/1.20.2`、`neoforge/1.20.4`
- Java 21：`neoforge/1.20.6`、`neoforge/1.21.4`

## 产物

- `dist/release/*`：发布用 Jar、混淆 Jar、mapping、seeds
- `dist/dglabmc-pvp-punish.zip`：示例规则配置
