# DG-LAB 控制中心

Forge `1.16.5` 客户端模组。

## 功能

- 原生 Minecraft 界面，不使用 WebView。
- DG-LAB 设备桥接，提供配对链接与本地 WebSocket 通道。
- 规则系统，可把游戏事件映射到波形播放。
- 自定义波形导入。
- 支持 `Dungeonlab+pulse` 文本导入。
- 支持 `HEX` 帧导入。
- 支持配置打包导出为 `ZIP`。
- 支持从 `ZIP` 导入配置，便于迁移。

## 当前实现

- 目标运行环境是 Forge `1.16.5`。
- 包名为 `cn.admilk.dglabweb`。
- UI 和提示文本已统一为中文。
- 已生成构建产物：`build/libs/dglabweb-0.1.0.jar`

## 触发事件

- 受伤
- 治疗
- 死亡
- 击杀
- 被击杀
- 低血量
- 低饥饿
- 跳跃
- 摔落伤害
- 开始冲刺
- 开始潜行
- 盾牌格挡
- 暴击
- 拉弓释放
- 图腾触发
- 护甲低耐久

## 使用方式

1. 安装到 Forge `1.16.5` 客户端。
2. 进入游戏后按 `O` 打开控制中心。
3. 在“配对链接”页复制链接，交给 DG-LAB App。
4. 在“规则”页配置事件到波形的映射。
5. 在“波形”页导入 `Dungeonlab+pulse` 或 `HEX`。
6. 在“配置迁移”页导出或导入 `ZIP`。

## 构建

要求：

- Java `11`
- 建议开启代理，当前项目已在 `gradle.properties` 中预设 `127.0.0.1:7890`

命令：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-11'
.\gradlew.bat build --no-daemon
```

## 兼容性说明

- NeoForge 不支持 `1.16.x`，所以当前实际实现仍是 Forge `1.16.5`。
- 代码里已把路径与客户端桥接做了平台抽象，后续可继续拆分到其他加载器。
- `图腾触发` 在 Forge `1.16.5` 下没有直接可用的对应事件，目前使用客户端状态近似检测。

## 参考

以下仓库仅用于功能方向、交互思路、协议行为和波形格式参考，没有直接照抄实现：

- `refs/Minecraft-DG-LAB`
- `refs/DG_LAB`
- `admilkjs/sse-dg-lab` 的波形导入思路
- Forge `1.16.x` 官方文档
