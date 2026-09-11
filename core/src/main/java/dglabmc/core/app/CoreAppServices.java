package dglabmc.core.app;

import dglabmc.core.config.AppConfig;
import dglabmc.core.config.ConfigArchiveService;
import dglabmc.core.config.ConfigRepository;
import dglabmc.core.config.DefaultConfigFactory;
import dglabmc.core.device.DeviceChannel;
import dglabmc.core.device.DeviceRuntime;
import dglabmc.core.device.DeviceRuntimeFactory;
import dglabmc.core.device.DeviceServer;
import dglabmc.core.device.DeviceSnapshot;
import dglabmc.core.rule.RuleDefinition;
import dglabmc.core.rule.RuleEngine;
import dglabmc.core.rule.RuleFeedback;
import dglabmc.core.rule.TriggerDefinition;
import dglabmc.core.rule.TriggerRegistry;
import dglabmc.core.util.NetworkUtil;
import dglabmc.core.wave.WaveformDefinition;
import dglabmc.core.wave.WaveformImportService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.IntSupplier;

/**
 * Platform-neutral application service facade.
 *
 * <p>This class contains configuration, rule, waveform, pairing and backup
 * use cases. Minecraft/Forge code supplies only the device runtime factory
 * and the default server port.</p>
 */
public final class CoreAppServices {
    private final Path rootDirectory;
    private final String loaderFlavor;
    private final String modVersion;
    private final DeviceRuntimeFactory runtimeFactory;
    private final IntSupplier defaultPortSupplier;
    private final RuleFeedback feedback;
    private final ConfigArchiveService archiveService = new ConfigArchiveService();
    private final WaveformImportService waveformImportService = new WaveformImportService();

    private ConfigRepository configRepository;
    private DeviceRuntime deviceRuntime;
    private RuleEngine ruleEngine;
    private boolean initialized;

    public CoreAppServices(Path rootDirectory, String loaderFlavor, String modVersion,
                           DeviceRuntimeFactory runtimeFactory) {
        this(rootDirectory, loaderFlavor, modVersion, runtimeFactory, null, null);
    }

    public CoreAppServices(Path rootDirectory, String loaderFlavor, String modVersion,
                           DeviceRuntimeFactory runtimeFactory, IntSupplier defaultPortSupplier) {
        this(rootDirectory, loaderFlavor, modVersion, runtimeFactory, defaultPortSupplier, null);
    }

    public CoreAppServices(Path rootDirectory, String loaderFlavor, String modVersion,
                           DeviceRuntimeFactory runtimeFactory, IntSupplier defaultPortSupplier,
                           RuleFeedback feedback) {
        if (rootDirectory == null) {
            throw new IllegalArgumentException("配置目录不能为空。");
        }
        if (runtimeFactory == null) {
            throw new IllegalArgumentException("设备运行时工厂不能为空。");
        }
        this.rootDirectory = rootDirectory;
        this.loaderFlavor = loaderFlavor == null ? "unknown" : loaderFlavor;
        this.modVersion = modVersion == null ? "unknown" : modVersion;
        this.runtimeFactory = runtimeFactory;
        this.defaultPortSupplier = defaultPortSupplier;
        this.feedback = feedback == null ? RuleFeedback.NOOP : feedback;
    }

    public synchronized void initialize() {
        if (initialized) {
            ensureSocketServerStarted();
            return;
        }
        try {
            this.configRepository = new ConfigRepository(rootDirectory, loaderFlavor);
            AppConfig config = configRepository.load();
            if (config.connection.deviceClientId == null || config.connection.deviceClientId.trim().isEmpty()) {
                config.connection.deviceClientId = UUID.randomUUID().toString();
                configRepository.save(config);
            }
            this.deviceRuntime = runtimeFactory.create(config.connection.deviceClientId);
            if (deviceRuntime == null) {
                throw new IllegalStateException("设备运行时工厂返回了空值。");
            }
            this.ruleEngine = new RuleEngine(configRepository, deviceRuntime, feedback);
            this.initialized = true;
            ensureSocketServerStarted();
        } catch (IOException exception) {
            throw new RuntimeException("初始化 DG-LAB 控制服务失败。", exception);
        } catch (RuntimeException exception) {
            this.initialized = false;
            if (deviceRuntime != null) {
                try {
                    deviceRuntime.shutdown();
                } catch (RuntimeException shutdownException) {
                    exception.addSuppressed(shutdownException);
                } finally {
                    deviceRuntime = null;
                    ruleEngine = null;
                }
            }
            throw exception;
        }
    }

    public synchronized void shutdown() {
        try {
            if (deviceRuntime != null) {
                deviceRuntime.shutdown();
            }
        } finally {
            deviceRuntime = null;
            ruleEngine = null;
            initialized = false;
        }
    }

    public synchronized AppConfig getConfig() {
        ensureInitialized();
        try {
            return configRepository.getCurrent();
        } catch (IOException exception) {
            throw new RuntimeException("读取配置失败。", exception);
        }
    }

    public synchronized void saveConfig(AppConfig config) {
        ensureInitialized();
        try {
            configRepository.save(config);
        } catch (IOException exception) {
            throw new RuntimeException("保存配置失败。", exception);
        }
    }

    public synchronized void saveRule(RuleDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("规则不能为空。");
        }
        AppConfig config = getConfig();
        for (int i = 0; i < config.rules.size(); i++) {
            if (definition.id != null && definition.id.equals(config.rules.get(i).id)) {
                config.rules.set(i, definition);
                saveConfig(config);
                return;
            }
        }
        config.rules.add(definition);
        saveConfig(config);
    }

    public synchronized void deleteRule(String id) {
        AppConfig config = getConfig();
        config.rules.removeIf(rule -> rule != null && id != null && id.equals(rule.id));
        saveConfig(config);
    }

    public synchronized void saveWaveform(WaveformDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("波形不能为空。");
        }
        AppConfig config = getConfig();
        for (int i = 0; i < config.waveforms.size(); i++) {
            if (definition.id != null && definition.id.equals(config.waveforms.get(i).id)) {
                config.waveforms.set(i, definition);
                saveConfig(config);
                return;
            }
        }
        config.waveforms.add(definition);
        saveConfig(config);
    }

    public synchronized void deleteWaveform(String id) {
        AppConfig config = getConfig();
        for (RuleDefinition rule : config.rules) {
            if (rule != null && id != null && id.equals(rule.waveformId)) {
                throw new IllegalStateException("波形仍被规则引用： " + rule.id);
            }
        }
        config.waveforms.removeIf(waveform -> waveform != null && id != null && id.equals(waveform.id));
        saveConfig(config);
    }

    public synchronized WaveformDefinition importPulseWaveform(String name, String description, String sourceText) {
        WaveformDefinition waveform = waveformImportService.importPulse(name, description, sourceText);
        saveWaveform(waveform);
        return waveform;
    }

    public synchronized WaveformDefinition importHexWaveform(String name, String description, String sourceText) {
        WaveformDefinition waveform = waveformImportService.importHex(name, description, sourceText);
        saveWaveform(waveform);
        return waveform;
    }

    public synchronized Path exportConfigArchive() {
        ensureInitialized();
        try {
            return archiveService.exportToDefaultLocation(configRepository.getRootDirectory(), getConfig(), modVersion);
        } catch (IOException exception) {
            throw new RuntimeException("导出配置失败。", exception);
        }
    }

    public synchronized void importConfigArchive(InputStream inputStream) {
        ensureInitialized();
        try {
            backupCurrentConfig("import-zip");
            configRepository.importConfig(archiveService.readArchive(inputStream));
        } catch (IOException exception) {
            throw new RuntimeException("导入配置失败。", exception);
        }
    }

    public synchronized DeviceSnapshot getDeviceSnapshot() {
        return deviceRuntime == null ? DeviceSnapshot.disconnected() : deviceRuntime.snapshot();
    }

    public synchronized boolean isDeviceBound() {
        return deviceRuntime != null && deviceRuntime.isBound();
    }

    public synchronized RuleEngine getRuleEngine() {
        ensureInitialized();
        return ruleEngine;
    }

    public synchronized RuleEngine.RuntimeSnapshot getRuleRuntimeSnapshot() {
        return getRuleEngine().snapshot();
    }

    public synchronized String getPairingLink() {
        ensureSocketServerStarted();
        return resolvePairingLink(false);
    }

    public synchronized String refreshPairingLink() {
        ensureSocketServerStarted();
        return resolvePairingLink(true);
    }

    public synchronized int getDevicePort() {
        ensureSocketServerStarted();
        return requireServer().getBoundPort();
    }

    public synchronized boolean isDeviceServerRunning() {
        return deviceRuntime != null && requireServer().isRunning();
    }

    public synchronized String getDeviceServerBindAddress() {
        return deviceRuntime == null ? "" : safeString(requireServer().getBoundHost());
    }

    public synchronized String getDeviceServerError() {
        return deviceRuntime == null ? "" : safeString(requireServer().getLastErrorMessage());
    }

    public synchronized List<TriggerDefinition> getTriggers() {
        return new ArrayList<TriggerDefinition>(TriggerRegistry.all());
    }

    public synchronized void testWaveform(String waveformId, DeviceChannel channel) {
        ensureDeviceBound();
        try {
            WaveformDefinition waveform = configRepository.findWaveform(waveformId);
            if (waveform == null) {
                throw new IllegalArgumentException("未找到波形： " + waveformId);
            }
            deviceRuntime.setStrength(channel, 45);
            deviceRuntime.sendPulse(channel, waveform.frames, true);
        } catch (IOException exception) {
            throw new RuntimeException("测试波形失败。", exception);
        }
    }

    public synchronized void testRule(RuleDefinition definition) {
        ensureDeviceBound();
        getRuleEngine().preview(definition);
    }

    public synchronized void restoreDefaultConfig() {
        AppConfig current = getConfig();
        backupCurrentConfig("restore-default");
        AppConfig defaults = DefaultConfigFactory.create();
        defaults.connection = current.connection == null ? defaults.connection : current.connection;
        if (current.ui != null && current.ui.accentPreset != null && !current.ui.accentPreset.trim().isEmpty()) {
            defaults.ui.accentPreset = current.ui.accentPreset;
        }
        defaults.ui.lastOpenedTab = "transfer";
        saveConfig(defaults);
    }

    private String resolvePairingLink(boolean refreshAddress) {
        DeviceServer server = requireServer();
        if (!server.isRunning() || server.getBoundPort() <= 0) {
            throw new IllegalStateException("设备服务尚未启动。");
        }
        AppConfig config = getConfig();
        String manualAddress = safeString(config.connection.deviceAdvertisedAddress).trim();
        String address = manualAddress.isEmpty() ? config.connection.lastResolvedLanIp : manualAddress;
        if (address == null || address.trim().isEmpty() || (refreshAddress && manualAddress.isEmpty())) {
            address = NetworkUtil.resolveBestLanAddress();
            if (!address.equals(config.connection.lastResolvedLanIp)) {
                config.connection.lastResolvedLanIp = address;
                saveConfig(config);
            }
        }
        return "https://www.dungeon-lab.com/app-download.php#DGLAB-SOCKET#ws://"
            + address + ":" + server.getBoundPort() + "/" + config.connection.deviceClientId;
    }

    private void ensureSocketServerStarted() {
        ensureInitialized();
        DeviceServer server = requireServer();
        if (server.isRunning()) {
            return;
        }
        if (defaultPortSupplier == null) {
            // The adapter may start its server explicitly when its loader
            // config is not available to Core. Core remains usable for
            // configuration and rule editing until then.
            return;
        }
        int port = defaultPortSupplier.getAsInt();
        if (port <= 0) {
            throw new IllegalStateException("设备服务端口无效： " + port);
        }
        AppConfig config = getConfig();
        String bindAddress = safeString(config.connection.localBindAddress).trim();
        if (bindAddress.isEmpty() || "127.0.0.1".equals(bindAddress) || "localhost".equalsIgnoreCase(bindAddress)) {
            bindAddress = "0.0.0.0";
        }
        server.start(bindAddress, port);
    }

    private DeviceServer requireServer() {
        if (deviceRuntime == null || deviceRuntime.server() == null) {
            throw new IllegalStateException("设备服务未初始化。");
        }
        return deviceRuntime.server();
    }

    private void ensureDeviceBound() {
        ensureInitialized();
        if (!isDeviceBound()) {
            throw new IllegalStateException("设备未绑定，无法发送测试。");
        }
    }

    private void ensureInitialized() {
        if (!initialized || configRepository == null || deviceRuntime == null || ruleEngine == null) {
            throw new IllegalStateException("DG-LAB 控制服务未初始化。");
        }
    }

    private void backupCurrentConfig(String reason) {
        try {
            archiveService.backupLastConfig(configRepository.getRootDirectory(), getConfig(), modVersion, reason);
        } catch (IOException exception) {
            throw new RuntimeException("备份当前配置失败。", exception);
        }
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }
}
