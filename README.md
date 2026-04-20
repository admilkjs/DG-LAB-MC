# DG-LAB MC

Minecraft 客户端 DG-LAB 控制模组，多版本分支仓库。

## 版本分支

| Minecraft | Loader | 构建 JDK | 分支 | 状态 |
|-----------|--------|----------|------|------|
| 1.12.2 | Forge | 8 | `forge/1.12.2` | 已验证 |
| 1.13.2 | Forge | 11 | `forge/1.13.2` | 已验证 |
| 1.14.4 | Forge | 11 | `forge/1.14.4` | 已验证 |
| 1.15.2 | Forge | 11 | `forge/1.15.2` | 已验证 |
| 1.16.5 | Forge | 8 | `forge/1.16.5` | 已验证 |
| 1.18.2 | Forge | 17 | `forge/1.18.2` | 已验证 |
| 1.19.2 | Forge | 17 | `forge/1.19.2` | 已验证 |
| 1.20.1 | Forge | 17 | `forge/1.20.1` | 已验证 |
| 1.20.2 | NeoForge | 17 | `neoforge/1.20.2` | 已验证 |
| 1.20.4 | NeoForge | 17 | `neoforge/1.20.4` | 已验证 |
| 1.20.6 | NeoForge | 21 | `neoforge/1.20.6` | 已验证 |
| 1.21.4 | NeoForge | 21 | `neoforge/1.21.4` | 已验证 |

详细说明见：

- [版本线与分支策略](docs/version-lines.md)
- [版本支持矩阵](docs/version-matrix.md)

## 本地构建

统一命令：

```powershell
.\gradlew.bat clean build generatePvpPunishConfig buildRelease --no-daemon
```

主要产物：

- 模组发布包：`dist/release/`
- 规则配置 ZIP：`dist/dglabmc-pvp-punish.zip`

## 仓库约定

- 包名：`dglabmc`
- Mod ID：`dglabmc`
- 能用 NeoForge 的版本优先 NeoForge
- 无法稳定使用 NeoForge 的版本使用 Forge
- 不做单个 Jar 兼容全部版本，按版本线分别维护和发布

## 参考

以下仓库只用于功能方向、交互思路、协议行为和波形格式参考，没有直接照抄实现：

- `refs/Minecraft-DG-LAB`
- `refs/DG_LAB`
- `admilkjs/sse-dg-lab` 的波形导入思路
- Forge / NeoForge 官方文档与官方 Maven 元数据
