# 版本线与分支策略

## 总规则

- 能用 NeoForge 的版本优先 NeoForge
- 不能用 NeoForge 的版本使用 Forge
- 每条版本线单独维护、单独发布

## 推荐分支

- `main`
  主开发线
- `forge/1.12.2`
  Forge 低版本线
- `forge/1.16.5`
  Forge 中期稳定线
- `forge/1.18.2`
  Forge 1.18 稳定线
- `forge/1.19.2`
  Forge 1.19 稳定线
- `forge/1.20.1`
  Forge 1.20.1 线
- `neoforge/1.20.2`
  NeoForge 1.20.2 线
- `neoforge/1.20.4`
  NeoForge 1.20.4 线
- `neoforge/1.20.6`
  NeoForge 1.20.6 线

## 推荐 tag

- `forge-1.12.2-v*`
- `forge-1.16.5-v*`
- `forge-1.18.2-v*`
- `forge-1.19.2-v*`
- `forge-1.20.1-v*`
- `neoforge-1.20.2-v*`
- `neoforge-1.20.4-v*`
- `neoforge-1.20.6-v*`

## 为什么这样拆

- Forge `1.12.x`、`1.16.x`、`1.18+` API 差异很大
- NeoForge 从 `1.20.2+` 开始才是更合理的目标
- Java 版本要求也不同，不能共用一套构建脚本长期维护

## 官方依据

- Forge 1.12.x: https://docs.minecraftforge.net/en/1.12.x/
- Forge 1.16.x: https://docs.minecraftforge.net/en/1.16.x/
- Forge 1.18.x: https://docs.minecraftforge.net/en/1.18.x/
- Forge 1.19.x: https://docs.minecraftforge.net/en/1.19.x/
- Forge 1.20.x: https://docs.minecraftforge.net/en/1.20.x/
- NeoForge 用户文档: https://docs.neoforged.net/user/docs/
- NeoForge 1.20.6: https://docs.neoforged.net/docs/1.20.6/
