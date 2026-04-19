# 版本线与分支策略

## 结论

这个项目要覆盖 `Forge 1.12.2` 到 `NeoForge` 版本线，必须拆成多分支维护，不能继续把所有目标版本塞进同一个 `1.16.5` 工程里。

## 推荐分支

- `main`
  当前稳定线，保留 Forge `1.16.5`
- `forge/1.16.5`
  当前实际发布线
- `forge/1.12.2`
  低版本兼容线
- `neoforge/1.20.1`
  NeoForge 最低支持线
- `neoforge/1.20.2+`
  NeoForge 后续主维护线

## 推荐 tag

- `forge-1.16.5-v0.1.0`
- `forge-1.12.2-v0.1.0`
- `neoforge-1.20.1-v0.1.0`
- `neoforge-1.20.2-v0.1.0`

只有对应版本线真正可构建时，才应该打 release tag。

## 为什么要拆

- Forge `1.12.x` 和 Forge `1.16.x` 的工程结构与注册方式不同
- NeoForge 从 `1.20.1+` 开始已经是另一条加载器路线
- Java 版本要求也分裂：
  Forge `1.12.x / 1.16.5` 仍以 Java 8 为基线
  NeoForge `1.20.1` 为 Java 17
  NeoForge `1.20.5+` 已切到 Java 21

## 当前仓库已完成的准备

- 统一主命名空间为 `dglabmc`
- 统一模组 ID 为 `dglabmc`
- 工作流改为分支推送即可触发
- README 和产物说明同步到新的命名空间

## 官方文档

- Forge 1.12.x: https://docs.minecraftforge.net/en/1.12.x/
- Forge 1.16.x: https://docs.minecraftforge.net/en/1.16.x/
- NeoForge 1.20.1: https://docs.neoforged.net/docs/1.20.1/
- NeoForge 1.20.2+: https://docs.neoforged.net/docs/1.20.2/

## 当前判断

按 2026-04-20 查到的 NeoForge 官方文档：

- 官方用户指南明确写了 NeoForge 虽然存在于 `1.20.1`，但更推荐在 `1.20.2+` 使用
- 当前公开文档覆盖到了 `1.21.x`
- 没查到 `1.22` 的官方 NeoForge 文档入口，所以当前不把 `1.22` 作为可发布目标
