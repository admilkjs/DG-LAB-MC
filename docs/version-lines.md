# 版本线与分支策略

## 结论

这个项目要覆盖 `Forge 1.12.2` 到 `NeoForge` 版本线，必须拆成多分支维护，不能继续把所有目标版本塞进同一个 `1.16.5` 工程里。

## 推荐分支

- `main`
  当前稳定线，保留 Forge `1.16.5`
- `forge/1.12.2`
  低版本兼容线
- `forge/1.16.5`
  Forge 中期稳定线
- `neoforge/1.20.1`
  NeoForge 最低支持线
- `neoforge/1.20.2+`
  NeoForge 后续主维护线

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
