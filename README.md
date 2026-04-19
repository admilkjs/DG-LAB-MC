# DG-LAB MC

客户端 DG-LAB 控制模组仓库。

## 当前可用发布

- `forge/1.16.5`
- Release: `forge-1.16.5-v0.1.1`

## 当前命名

- 主代码包名：`dglabmc`
- 模组 ID：`dglabmc`

## 当前仓库规则

- 能用 NeoForge 的版本优先 NeoForge
- 不能用 NeoForge 的版本使用 Forge
- 不做“一个 jar 兼容全部版本”
- 按版本线拆分分支、构建、发布

## 版本矩阵

详见：

- [版本线与分支策略](docs/version-lines.md)
- [版本支持矩阵](docs/version-matrix.md)

## 本地构建

标准构建：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-11'
.\gradlew.bat shadowJar reobfShadowJar generatePvpPunishConfig --no-daemon
```

本地混淆发布构建：

```powershell
.\gradlew.bat buildRelease --no-daemon
```

## GitHub Actions

`Build` 工作流会上传：

- `build/libs/*.jar`
- `dist/*.zip`

`Release` 工作流会在 tag 推送后发布：

- `build/libs/*.jar`
- `dist/*.zip`

说明：

- GitHub Release 当前只发布 CI 中稳定可用的标准产物
- ProGuard 混淆发布包继续保留为本地手动构建

## 当前已实现

- 原生 Minecraft UI
- DG-LAB 设备配对与本地 WebSocket 桥接
- 规则系统
- 自定义波形导入
- 配置 ZIP 导入导出
- 聊天密文伪造事件

## 参考

以下仓库只作为功能方向、交互思路、协议行为和波形格式参考，没有直接照抄实现：

- `refs/Minecraft-DG-LAB`
- `refs/DG_LAB`
- `admilkjs/sse-dg-lab` 的波形导入思路
- Forge / NeoForge 官方文档
