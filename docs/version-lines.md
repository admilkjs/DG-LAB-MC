# 版本线与分支策略

## 总原则

- 能用 NeoForge 的版本优先 NeoForge。
- 无法稳定使用 NeoForge 的版本使用 Forge。
- 每条版本线单独维护、单独构建、单独发布。
- `main` 只放公共文档、工作流和仓库级配置。

## 当前版本线

- `forge/1.12.2`
- `forge/1.13.2`
- `forge/1.14.4`
- `forge/1.15.2`
- `forge/1.16.5`
- `forge/1.18.2`
- `forge/1.19.2`
- `forge/1.20.1`
- `neoforge/1.20.2`
- `neoforge/1.20.4`
- `neoforge/1.20.6`
- `neoforge/1.21.4`

## Release 规则

- 主分支使用 `v*` tag 触发聚合发布
- 一个 Release 对应一个 `mod_version`
- 同一次发布会打包全部版本分支产物

## 官方依据

- Forge 元数据：`https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml`
- NeoForge 元数据：`https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml`
- NeoForge 1.21.4 文档：`https://docs.neoforged.net/docs/1.21.4/`
- 1.21.4 资源生成文档：`https://docs.neoforged.net/docs/1.21.4/resources/`

## 说明

- `1.13.2`、`1.14.4`、`1.15.2` 使用 Forge 老构建链回退适配。
- `1.20.2+` 版本线使用 NeoForge。
- 构建 JDK 按分支固定，交给 GitHub Actions 自动选择。
