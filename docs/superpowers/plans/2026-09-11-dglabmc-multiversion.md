# DG-LAB MC 单主干多版本实施计划

## 目标

将当前按 Minecraft 版本分支复制维护的仓库，重构为一个 `main` 主干：

- `core` 保存与 Minecraft API 无关的业务逻辑；
- `adapters/forge-1.20.1` 保存 Forge 1.20.1 的入口、事件、UI、命令、网络和资源；
- 根工程提供 `buildAll` 与 `buildTarget`；
- 首个交付目标为 Forge 1.20.1；
- 后续版本通过新增 adapter 接入，最终删除版本分支。

## 阶段 0：建立基线

1. 从 `forge/1.20.1` 当前提交建立迁移工作分支。
2. 记录 Forge 1.20.1 的构建参数：Minecraft 1.20.1、Forge 47.4.18、Java 17、mod id `dglabmc`。
3. 在迁移前执行现有测试和 `buildRelease`，保存 Jar、配置 ZIP 和日志作为回归基线。
4. 保留 `main` 上的未跟踪 `.claude/`、`.serena/` 状态，不把它们纳入迁移提交。

完成标准：基线构建成功，迁移前后可以比较产物和运行行为。

## 阶段 1：创建独立 Core

新增 `core` 普通 Java 子工程，先迁移以下包：

- `config`
- `device`
- `rule`
- `security`
- `wave`
- `util`
- 多人状态中的纯数据对象

包名统一为 `dglabmc.core.*`。Core 不依赖 Minecraft、Forge 或 NeoForge，只保留 Java 标准库、Netty、ZXing 等必要库。

新增最小端口接口：

```text
CoreHost
  configDirectory()
  modVersion()
  showMessage()
  openControlCenter()
  copyToClipboard()
  readClipboard()
  openFileManager()
```

将 `AppServices` 改造成可注入的 `DgLabApplication`。配置目录、平台提示和 UI 打开动作由 adapter 提供。Core 内不再引用 `DgLabMcMod` 或 Forge 配置类。

同步迁移并扩大纯 Java 测试：规则引擎、信号解析、WebSocket、配置归档和旧配置迁移。

完成标准：

- `core:test` 不需要 Minecraft 开发环境；
- Core 源码中不存在 `net.minecraft`、`net.minecraftforge` 或 `com.mojang` import；
- 原有核心测试全部通过。

## 阶段 2：建立 Forge 1.20.1 Adapter

新增 `adapters/forge-1.20.1` 独立 Gradle 工程，使用原分支的 ForgeGradle、Shadow、Reobf 和 Java 17 配置。

迁移并重命名以下职责：

```text
ForgeEntrypoint       @Mod 入口与生命周期
ForgeEvents           客户端、玩家、伤害、聊天、Tick 事件
ForgeCommands         Forge Brigadier 命令注册
ForgePlatformBridge   路径、剪贴板、提示、文件管理器
ForgeNetwork          SimpleChannel 和数据包
ui/*                  Minecraft Screen 和 Widget
```

Forge 事件先转换成 Core DTO，再交给 Core 的事件处理器。Forge 网络包只在 adapter 内使用 `FriendlyByteBuf`、`ServerPlayer` 和 `SimpleChannel`。

Core UI 只提供模型、校验和操作结果；Minecraft Screen 继续在 adapter 内实现，不创建跨版本 UI 框架。

完成标准：Forge 1.20.1 可启动、加载 Mod、打开控制中心、执行命令、连接设备、触发规则，并正确显示多人状态。

## 阶段 3：根工程构建调度

根 `main` 不应用 Minecraft Gradle 插件，只负责：

- 管理启用的 target 清单；
- 执行 `core:test`；
- 调度单目标和全量构建；
- 汇总发布产物和日志。

目标清单放入 `gradle/targets.properties`，记录 loader、Minecraft、loader 版本、Java 版本、adapter 路径和启用状态。

提供任务：

```powershell
.\gradlew.bat :core:test
.\gradlew.bat buildTarget --project-prop target=forge-1.20.1
.\gradlew.bat buildAll
```

每个 adapter 可以固定自己的 Gradle wrapper 和插件版本。根工程调用 adapter 的 `buildRelease`，避免 ForgeGradle 3、ForgeGradle 6 和 NeoForge Gradle 7 互相污染。

完成标准：

- `buildTarget --project-prop target=forge-1.20.1` 输出可发布 Jar；
- `buildAll` 能调用全部启用 target；
- Shadow 将 Core 合入最终 Mod Jar，Forge Reobf 在合并后执行；
- CI 使用与本地相同的根任务。

## 阶段 4：兼容与数据迁移

1. 保持 mod id `dglabmc`、资源命名空间和配置格式稳定。
2. 首次启动时检测旧 `dglabweb` 配置目录，并迁移到 `dglabmc`。
3. 保留旧字段的读取兼容，写回时统一为当前格式。
4. 为配置迁移增加备份和失败回滚。
5. 将 `mods.toml`、语言文件和 `pack.mcmeta` 作为 adapter 资源，统一由目标属性过滤。

完成标准：旧配置可以无损读取，新版本配置可以导入导出，迁移失败不会覆盖原文件。

## 阶段 5：从版本分支提取后续 Adapter

Forge 1.20.1 验证后，再按以下顺序迁移：

1. Forge 1.19.2、Forge 1.18.2；
2. NeoForge 1.20.2、1.20.4、1.20.6、1.21.4；
3. Forge 1.16.5、1.15.2、1.14.4、1.13.2、1.12.2。

每个版本只允许新增或修改对应 adapter。若发现两套 adapter 出现稳定重复，再抽取 `forge-common`；在重复出现前不提前建设额外抽象。

每次迁移必须验证：

- 入口和资源元数据；
- 事件映射；
- UI 交互；
- 命令行为；
- 网络同步；
- Java 与 Gradle 工具链；
- 发布 Jar 可加载。

## 阶段 6：分支退役

所有 target 在 CI 和发布流程通过后：

1. 为每条旧版本线保留最终版本 Tag。
2. 将迁移记录写入版本矩阵和变更日志。
3. 将旧分支尖端保存为归档 Tag。
4. 删除 `forge/*`、`neoforge/*` 和旧 refactor 分支。
5. 更新 README，改为介绍 `main`、Core 和 adapter 目录。
6. 发布流程只从 `main` 的 target 清单读取版本。

## 提交拆分

建议按以下提交保持可回滚：

1. `docs: add multiversion architecture plan`
2. `refactor: create standalone core module`
3. `refactor: add forge 1.20.1 adapter`
4. `build: add target dispatcher and aggregate tasks`
5. `test: add core and forge 1.20.1 regression coverage`
6. `docs: retire version branch workflow`

## 风险与处理

| 风险 | 处理方式 |
|---|---|
| ForgeGradle 版本相互冲突 | adapter 使用独立构建和独立 wrapper |
| UI API 差异过大 | Core 只保存 UI 模型，渲染代码留在 adapter |
| Core 误依赖 Minecraft | CI 增加 import 扫描和独立编译 |
| 配置目录变化导致丢配置 | 首次启动备份、迁移、失败回滚 |
| 多人网络协议不兼容 | Core 只保存状态 DTO，包格式由 adapter 管理 |
| 全量构建占用过多内存 | 根任务默认串行，可显式开启 Gradle parallel |

## 最终验收

- 功能代码在 Core 只实现一次。
- 新增版本只需新增 adapter、版本配置和资源模板。
- `core:test` 可脱离 Minecraft 执行。
- `buildTarget` 和 `buildAll` 都可用。
- Forge 1.20.1 发布包行为与迁移前一致。
- 删除旧版本分支后，主干仍可重建所有已启用版本。
