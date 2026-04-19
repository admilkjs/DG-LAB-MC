package cn.admilk.dglabweb;

import cn.admilk.dglabweb.config.AppConfig;
import cn.admilk.dglabweb.config.ConfigArchiveService;
import cn.admilk.dglabweb.config.ConfigRepository;
import cn.admilk.dglabweb.device.DeviceSessionManager;
import cn.admilk.dglabweb.device.DeviceWebSocketServer;
import cn.admilk.dglabweb.device.DeviceChannel;
import cn.admilk.dglabweb.rule.RuleDefinition;
import cn.admilk.dglabweb.rule.RuleEngine;
import cn.admilk.dglabweb.rule.TriggerDefinition;
import cn.admilk.dglabweb.rule.TriggerRegistry;
import cn.admilk.dglabweb.platform.PlatformServices;
import cn.admilk.dglabweb.util.NetworkUtil;
import cn.admilk.dglabweb.wave.WaveformDefinition;
import cn.admilk.dglabweb.wave.WaveformImportService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

public final class AppServices {
    private static final AppServices INSTANCE = new AppServices();

    private ConfigRepository configRepository;
    private ConfigArchiveService archiveService;
    private WaveformImportService waveformImportService;
    private DeviceSessionManager deviceSessionManager;
    private DeviceWebSocketServer deviceWebSocketServer;
    private RuleEngine ruleEngine;
    private boolean initialized;

    private AppServices() {
    }

    public static AppServices get() {
        return INSTANCE;
    }

    public synchronized void initialize() {
        if (initialized) {
            return;
        }
        try {
            Path rootDirectory = PlatformServices.paths().resolveConfigDirectory(DgLabWebMod.MODID);
            this.configRepository = new ConfigRepository(rootDirectory);
            this.archiveService = new ConfigArchiveService();
            this.waveformImportService = new WaveformImportService();
            this.deviceSessionManager = new DeviceSessionManager();
            AppConfig config = this.configRepository.load();
            if (config.connection.deviceClientId == null || config.connection.deviceClientId.trim().isEmpty()) {
                config.connection.deviceClientId = UUID.randomUUID().toString();
                this.configRepository.save(config);
            }
            this.deviceWebSocketServer = new DeviceWebSocketServer(this.deviceSessionManager, config.connection.deviceClientId);
            this.deviceWebSocketServer.start(cn.admilk.dglabweb.config.StartupConfig.resolveDevicePort());
            this.ruleEngine = new RuleEngine(this.configRepository, this.deviceSessionManager);
            this.initialized = true;
        } catch (IOException exception) {
            throw new RuntimeException("初始化 DG-LAB 控制服务失败。", exception);
        }
    }

    public synchronized void shutdown() {
        if (!initialized) {
            return;
        }
        if (deviceWebSocketServer != null) {
            deviceWebSocketServer.stop();
        }
        if (deviceSessionManager != null) {
            deviceSessionManager.shutdown();
        }
        initialized = false;
    }

    public synchronized AppConfig getConfig() {
        try {
            return configRepository.getCurrent();
        } catch (IOException exception) {
            throw new RuntimeException("读取配置失败。", exception);
        }
    }

    public synchronized void saveConfig(AppConfig config) {
        try {
            configRepository.save(config);
        } catch (IOException exception) {
            throw new RuntimeException("保存配置失败。", exception);
        }
    }

    public synchronized void saveRule(RuleDefinition definition) {
        AppConfig config = getConfig();
        for (int i = 0; i < config.rules.size(); i++) {
            if (config.rules.get(i).id.equals(definition.id)) {
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
        config.rules.removeIf(rule -> rule.id.equals(id));
        saveConfig(config);
    }

    public synchronized void saveWaveform(WaveformDefinition definition) {
        AppConfig config = getConfig();
        for (int i = 0; i < config.waveforms.size(); i++) {
            if (config.waveforms.get(i).id.equals(definition.id)) {
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
            if (id.equals(rule.waveformId)) {
                throw new IllegalStateException("波形仍被规则引用： " + rule.id);
            }
        }
        config.waveforms.removeIf(waveform -> waveform.id.equals(id));
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
        try {
            return archiveService.exportToDefaultLocation(configRepository.getRootDirectory(), getConfig(), getModVersion());
        } catch (IOException exception) {
            throw new RuntimeException("导出配置失败。", exception);
        }
    }

    public synchronized void importConfigArchive(InputStream inputStream) {
        try {
            backupCurrentConfig("import-zip");
            AppConfig imported = archiveService.readArchive(inputStream);
            configRepository.importConfig(imported);
        } catch (IOException exception) {
            throw new RuntimeException("导入配置失败。", exception);
        }
    }

    public synchronized DeviceSessionManager.DeviceSnapshot getDeviceSnapshot() {
        return deviceSessionManager.snapshot();
    }

    public synchronized boolean isDeviceBound() {
        return deviceSessionManager != null && deviceSessionManager.isBound();
    }

    public synchronized RuleEngine getRuleEngine() {
        return ruleEngine;
    }

    public synchronized RuleEngine.RuntimeSnapshot getRuleRuntimeSnapshot() {
        return ruleEngine.snapshot();
    }

    public synchronized String getPairingLink() {
        return resolvePairingLink(false);
    }

    public synchronized String refreshPairingLink() {
        return resolvePairingLink(true);
    }

    private String resolvePairingLink(boolean refreshAddress) {
        AppConfig config = getConfig();
        String manualAddress = config.connection.deviceAdvertisedAddress == null ? "" : config.connection.deviceAdvertisedAddress.trim();
        String address = manualAddress;
        if (address.isEmpty()) {
            address = config.connection.lastResolvedLanIp;
        }
        if (address == null || address.trim().isEmpty() || (refreshAddress && manualAddress.isEmpty())) {
            address = NetworkUtil.resolveBestLanAddress();
            if (!address.equals(config.connection.lastResolvedLanIp)) {
                config.connection.lastResolvedLanIp = address;
                saveConfig(config);
            }
        }
        return "https://www.dungeon-lab.com/app-download.php#DGLAB-SOCKET#ws://"
            + address
            + ":"
            + deviceWebSocketServer.getBoundPort()
            + "/"
            + config.connection.deviceClientId;
    }

    public synchronized int getDevicePort() {
        return deviceWebSocketServer.getBoundPort();
    }

    public synchronized List<TriggerDefinition> getTriggers() {
        return new ArrayList<TriggerDefinition>(TriggerRegistry.all());
    }

    public synchronized void testWaveform(String waveformId, DeviceChannel channel) {
        try {
            ensureDeviceBound();
            WaveformDefinition waveform = configRepository.findWaveform(waveformId);
            if (waveform == null) {
                throw new IllegalArgumentException("未找到波形： " + waveformId);
            }
            deviceSessionManager.setStrength(channel, 45);
            deviceSessionManager.sendPulse(channel, waveform.frames, true);
        } catch (IOException exception) {
            throw new RuntimeException("测试波形失败。", exception);
        }
    }

    public synchronized void testRule(RuleDefinition definition) {
        ensureDeviceBound();
        this.ruleEngine.preview(definition);
    }

    public synchronized void restoreDefaultConfig() {
        AppConfig current = getConfig();
        backupCurrentConfig("restore-default");
        AppConfig defaults = cn.admilk.dglabweb.config.DefaultConfigFactory.create();
        defaults.connection = current.connection == null ? defaults.connection : current.connection;
        if (current.ui != null && current.ui.accentPreset != null && !current.ui.accentPreset.trim().isEmpty()) {
            defaults.ui.accentPreset = current.ui.accentPreset;
        }
        defaults.ui.lastOpenedTab = "transfer";
        saveConfig(defaults);
    }

    private void ensureDeviceBound() {
        if (!isDeviceBound()) {
            throw new IllegalStateException("设备未绑定，无法发送测试。");
        }
    }

    private void backupCurrentConfig(String reason) {
        try {
            archiveService.backupLastConfig(configRepository.getRootDirectory(), getConfig(), getModVersion(), reason);
        } catch (IOException exception) {
            throw new RuntimeException("备份当前配置失败。", exception);
        }
    }

    private String getModVersion() {
        return DgLabWebMod.VERSION;
    }
}
