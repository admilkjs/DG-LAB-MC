package dglabmc;

import dglabmc.core.app.CoreAppServices;
import dglabmc.core.config.AppConfig;
import dglabmc.core.device.DeviceChannel;
import dglabmc.core.device.DeviceSnapshot;
import dglabmc.core.rule.RuleDefinition;
import dglabmc.core.rule.RuleEngine;
import dglabmc.core.rule.RuleFeedback;
import dglabmc.core.rule.TriggerDefinition;
import dglabmc.core.wave.WaveformDefinition;
import dglabmc.device.DeviceSessionManager;
import dglabmc.device.DeviceWebSocketServer;
import dglabmc.device.ForgeDeviceRuntime;
import dglabmc.platform.PlatformServices;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

/** Forge 1.14.4 facade around the platform-neutral Core application service. */
public final class AppServices {
    private static final AppServices INSTANCE = new AppServices();
    private CoreAppServices delegate;
    private DeviceSessionManager sessionManager;

    private AppServices() { }

    public static AppServices get() { return INSTANCE; }

    public synchronized void initialize() {
        if (delegate == null) {
            Path root = PlatformServices.paths().resolveConfigDirectory(DgLabMcMod.MODID);
            delegate = new CoreAppServices(root, "forge-1.14.4", DgLabMcMod.VERSION,
                fallbackClientId -> {
                    sessionManager = new DeviceSessionManager();
                    DeviceWebSocketServer server = new DeviceWebSocketServer(sessionManager, fallbackClientId);
                    return new ForgeDeviceRuntime(sessionManager, server);
                }, dglabmc.config.StartupConfig::resolveDevicePort, RuleFeedback.NOOP);
        }
        delegate.initialize();
    }

    public synchronized void shutdown() {
        if (delegate != null) delegate.shutdown();
        delegate = null;
        sessionManager = null;
    }

    public synchronized AppConfig getConfig() { return service().getConfig(); }
    public synchronized void saveConfig(AppConfig value) { service().saveConfig(value); }
    public synchronized void saveRule(RuleDefinition value) { service().saveRule(value); }
    public synchronized void deleteRule(String id) { service().deleteRule(id); }
    public synchronized void saveWaveform(WaveformDefinition value) { service().saveWaveform(value); }
    public synchronized void deleteWaveform(String id) { service().deleteWaveform(id); }
    public synchronized WaveformDefinition importPulseWaveform(String name, String description, String source) { return service().importPulseWaveform(name, description, source); }
    public synchronized WaveformDefinition importHexWaveform(String name, String description, String source) { return service().importHexWaveform(name, description, source); }
    public synchronized Path exportConfigArchive() { return service().exportConfigArchive(); }
    public synchronized void importConfigArchive(InputStream input) { service().importConfigArchive(input); }
    public synchronized DeviceSessionManager.DeviceSnapshot getDeviceSnapshot() { return sessionManager == null ? new DeviceSessionManager.DeviceSnapshot() : sessionManager.snapshot(); }
    public synchronized boolean isDeviceBound() { return service().isDeviceBound(); }
    public synchronized RuleEngine getRuleEngine() { return service().getRuleEngine(); }
    public synchronized RuleEngine.RuntimeSnapshot getRuleRuntimeSnapshot() { return service().getRuleRuntimeSnapshot(); }
    public synchronized String getPairingLink() { return service().getPairingLink(); }
    public synchronized String refreshPairingLink() { return service().refreshPairingLink(); }
    public synchronized int getDevicePort() { return service().getDevicePort(); }
    public synchronized boolean isDeviceServerRunning() { return service().isDeviceServerRunning(); }
    public synchronized String getDeviceServerBindAddress() { return service().getDeviceServerBindAddress(); }
    public synchronized String getDeviceServerError() { return service().getDeviceServerError(); }
    public synchronized List<TriggerDefinition> getTriggers() { return service().getTriggers(); }
    public synchronized void testWaveform(String id, DeviceChannel channel) { service().testWaveform(id, channel); }
    public synchronized void testRule(RuleDefinition value) { service().testRule(value); }
    public synchronized void restoreDefaultConfig() { service().restoreDefaultConfig(); }

    private CoreAppServices service() {
        if (delegate == null) initialize();
        return delegate;
    }
}
