# DG-LAB MC

Forge `1.16.5` 客户端 DG-LAB 控制模组。

## 当前状态

- 当前可构建目标：Forge `1.16.5`
- 当前主代码包名：`dglabmc`
- 当前模组 ID：`dglabmc`
- 当前默认产物：`build/libs/dglabmc-0.1.0.jar`
- 当前配置包产物：`dist/dglabmc-pvp-punish.zip`

## 已实现内容

- 原生 Minecraft 界面，不内置 WebView
- DG-LAB 设备配对、本地 WebSocket 桥接
- 规则系统、波形导入、配置导入导出
- 聊天密文伪造事件
- 单独的 PVP 惩罚配置生成

## 构建

本地：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-11'
.\gradlew.bat shadowJar reobfShadowJar generatePvpPunishConfig --no-daemon
```

本地混淆发布构建：

```powershell
.\gradlew.bat buildRelease --no-daemon
```

## 工作流产物

GitHub Actions `Build` 工作流会上传：

- `build/libs/*.jar`
- `dist/*.zip`

本地运行 `buildRelease` 还会生成：

- `dist/release/dglabmc-<version>-release-obf.jar`
- `dist/release/dglabmc-<version>-release-obf.mapping.txt`
- `dist/release/dglabmc-<version>-release-obf.seeds.txt`

GitHub `Release` 工作流会在版本 tag 推送后发布：

- `build/libs/*.jar`
- `dist/*.zip`

说明：

- GitHub Release 当前发布标准可用产物：重映射后的主 jar 和配置 zip
- ProGuard 混淆发布包暂时保留为本地手动构建，避免 CI 环境差异导致发布失败

## 版本线

这个仓库不会用一个 jar 硬兼容 `1.12.2` 到 NeoForge 全线版本。

原因很简单：

- `1.12.2`、`1.16.5`、`NeoForge 1.20.1+` 的加载器、事件总线、注册方式、资源结构、Java 版本要求都不是一套
- 真要长期维护，只能按版本线拆分

当前仓库已经先把命名空间改成了统一的 `dglabmc`，并准备按分支推进多版本。

详细说明见：

- [版本线与分支策略](docs/version-lines.md)

## 参考

以下仓库只作为功能方向、交互思路、协议行为和波形格式参考，没有直接照抄实现：

- `refs/Minecraft-DG-LAB`
- `refs/DG_LAB`
- `admilkjs/sse-dg-lab` 的波形导入思路
- Forge / NeoForge 官方文档
