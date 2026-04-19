# 版本支持矩阵

## 当前矩阵

| Minecraft | Loader | Java | 分支 | 构建验证 | 发布状态 |
|-----------|--------|------|------|----------|----------|
| 1.12.2 | Forge | 8 | `forge/1.12.2` | `clean build generatePvpPunishConfig buildRelease` 通过 | 待发布 |
| 1.16.5 | Forge | 8 | `forge/1.16.5` | 已验证 | 待发布 |
| 1.18.2 | Forge | 17 | `forge/1.18.2` | 已验证 | 待发布 |
| 1.19.2 | Forge | 17 | `forge/1.19.2` | 已验证 | 待发布 |
| 1.20.1 | Forge | 17 | `forge/1.20.1` | 已验证 | 待发布 |
| 1.20.2 | NeoForge | 17 | `neoforge/1.20.2` | 已验证 | 待发布 |
| 1.20.4 | NeoForge | 17 | `neoforge/1.20.4` | 已验证 | 待发布 |
| 1.20.6 | NeoForge | 21 | `neoforge/1.20.6` | 已验证 | 待发布 |

## 构建命令

所有版本线统一使用：

```powershell
.\gradlew.bat clean build generatePvpPunishConfig buildRelease --no-daemon
```

区别只在 JDK：

- Java 8：`forge/1.12.2`、`forge/1.16.5`
- Java 17：`forge/1.18.2`、`forge/1.19.2`、`forge/1.20.1`、`neoforge/1.20.2`、`neoforge/1.20.4`
- Java 21：`neoforge/1.20.6`

## 说明

- 这里按稳定维护线发布，不按每一个小版本都单独出包。
- GitHub Actions 会根据分支或 tag 自动选择对应 JDK。
