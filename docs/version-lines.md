# 版本线与分支策略

## 总规则

- 能用 NeoForge 的版本优先 NeoForge。
- 不能稳定使用 NeoForge 的版本使用 Forge。
- 每条版本线单独维护、单独构建、单独发布。
- `main` 只放共用文档、工作流和仓库级配置。

## 版本分支

- `forge/1.12.2`
- `forge/1.16.5`
- `forge/1.18.2`
- `forge/1.19.2`
- `forge/1.20.1`
- `neoforge/1.20.2`
- `neoforge/1.20.4`
- `neoforge/1.20.6`

## Tag 规则

- `forge-1.12.2-v*`
- `forge-1.16.5-v*`
- `forge-1.18.2-v*`
- `forge-1.19.2-v*`
- `forge-1.20.1-v*`
- `neoforge-1.20.2-v*`
- `neoforge-1.20.4-v*`
- `neoforge-1.20.6-v*`

## 为什么拆分

- Forge `1.12.2`、`1.16.5`、`1.18+` 的 API 差异很大。
- NeoForge 从 `1.20.2+` 才是合理目标。
- Java 版本要求不同，长期维护不适合共用一套构建脚本。

## 官方依据

- Forge 1.12.x: https://docs.minecraftforge.net/en/1.12.x/
- Forge 1.16.x: https://docs.minecraftforge.net/en/1.16.x/
- Forge 1.18.x: https://docs.minecraftforge.net/en/1.18.x/
- Forge 1.19.x: https://docs.minecraftforge.net/en/1.19.x/
- Forge 1.20.x: https://docs.minecraftforge.net/en/1.20.x/
- NeoForge 用户文档: https://docs.neoforged.net/user/docs/
- NeoForge 1.20.6: https://docs.neoforged.net/docs/1.20.6/
