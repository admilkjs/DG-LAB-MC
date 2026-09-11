# dglabmc core

平台无关的 DG-LAB 业务层。此工程只依赖 Java 标准库与 Gson，不引用 Minecraft、Forge 或 NeoForge。

适配器通过 `dglabmc.core.device.DeviceTransport`、配置目录和 UI/日志端口接入核心服务。

当前迁移范围包含配置、规则模型、触发器注册表、波形解析、安全模块、网络工具、设备 DTO/会话状态和多人状态 DTO。WebSocket 服务、Netty 连接管理、Minecraft 事件映射及 `RuleEngine` 的平台反馈回调仍由适配器承载；后续应通过 `DeviceTransport`、`RuleFeedback` 和时钟接口逐步抽取。

版本标识由适配器注入：构造 `ConfigRepository` 时传入目标 loader（例如 `forge-1.20.1`），或在保存前设置 `AppConfig.loaderFlavor`。
