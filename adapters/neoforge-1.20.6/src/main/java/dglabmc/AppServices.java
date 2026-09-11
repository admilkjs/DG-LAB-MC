package dglabmc;

import dglabmc.core.app.CoreAppServices;
import dglabmc.core.config.AppConfig;
import dglabmc.core.device.DeviceChannel;
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

/** NeoForge facade preserving old call sites while delegating use cases to Core. */
public final class AppServices {
    private static final AppServices INSTANCE = new AppServices();

    private CoreAppServices delegate;
    private DeviceSessionManager sessionManager;

    private AppServices() {
    }

    public static AppServices get() {
        return INSTANCE;
    }

    public synchronized void initialize() {
        if (delegate == null) {
            Path rootDirectory = PlatformServices.paths().resolveConfigDirectory(DgLabMcMod.MODID);
            RuleFeedback feedback = new RuleFeedback() {
                @Override
                public void reportRuleRowContinue(int rowIndex) {
                    dglabmc.client.ClientCommandRouter.reportRuleRowContinue(rowIndex);
                }

                @Override
                public void reportRuleRowMatched(int rowIndex, List<String> ruleNames) {
                    dglabmc.client.ClientCommandRouter.reportRuleRowMatched(rowIndex, ruleNames);
                }

                @Override
                public void reportRuleStop(int rowIndex) {
                    dglabmc.client.ClientCommandRouter.reportRuleStop(rowIndex);
                }

                @Override
                public void reportRuleNoMatch() {
                    dglabmc.client.ClientCommandRouter.reportRuleNoMatch();
                }
            };
            delegate = new CoreAppServices(
                rootDirectory,
                "neoforge-1.20.6",
                DgLabMcMod.VERSION,
                fallbackClientId -> {
                    sessionManager = new DeviceSessionManager();
                    DeviceWebSocketServer server = new DeviceWebSocketServer(sessionManager, fallbackClientId);
                    return new ForgeDeviceRuntime(sessionManager, server);
                },
                dglabmc.config.StartupConfig::resolveDevicePort,
                feedback
            );
        }
        delegate.initialize();
    }

    public synchronized void shutdown() {
        if (delegate != null) {
            delegate.shutdown();
        }
        delegate = null;
        sessionManager = null;
    }

    public synchronized AppConfig getConfig() { return delegate().getConfig(); }
    public synchronized void saveConfig(AppConfig config) { delegate().saveConfig(config); }
    public synchronized void saveRule(RuleDefinition definition) { delegate().saveRule(definition); }
    public synchronized void deleteRule(String id) { delegate().deleteRule(id); }
    public synchronized void saveWaveform(WaveformDefinition definition) { delegate().saveWaveform(definition); }
    public synchronized void deleteWaveform(String id) { delegate().deleteWaveform(id); }
    public synchronized WaveformDefinition importPulseWaveform(String name, String description, String sourceText) { return delegate().importPulseWaveform(name, description, sourceText); }
    public synchronized WaveformDefinition importHexWaveform(String name, String description, String sourceText) { return delegate().importHexWaveform(name, description, sourceText); }
    public synchronized Path exportConfigArchive() { return delegate().exportConfigArchive(); }
    public synchronized void importConfigArchive(InputStream inputStream) { delegate().importConfigArchive(inputStream); }
    public synchronized DeviceSessionManager.DeviceSnapshot getDeviceSnapshot() { return sessionManager == null ? new DeviceSessionManager.DeviceSnapshot() : sessionManager.snapshot(); }
    public synchronized boolean isDeviceBound() { return delegate().isDeviceBound(); }
    public synchronized RuleEngine getRuleEngine() { return delegate().getRuleEngine(); }
    public synchronized RuleEngine.RuntimeSnapshot getRuleRuntimeSnapshot() { return delegate().getRuleRuntimeSnapshot(); }
    public synchronized String getPairingLink() { return delegate().getPairingLink(); }
    public synchronized String refreshPairingLink() { return delegate().refreshPairingLink(); }
    public synchronized int getDevicePort() { return delegate().getDevicePort(); }
    public synchronized boolean isDeviceServerRunning() { return delegate().isDeviceServerRunning(); }
    public synchronized String getDeviceServerBindAddress() { return delegate().getDeviceServerBindAddress(); }
    public synchronized String getDeviceServerError() { return delegate().getDeviceServerError(); }
    public synchronized List<TriggerDefinition> getTriggers() { return delegate().getTriggers(); }
    public synchronized void testWaveform(String waveformId, DeviceChannel channel) { delegate().testWaveform(waveformId, channel); }
    public synchronized void testRule(RuleDefinition definition) { delegate().testRule(definition); }
    public synchronized void restoreDefaultConfig() { delegate().restoreDefaultConfig(); }

    private CoreAppServices delegate() {
        if (delegate == null) {
            initialize();
        }
        return delegate;
    }
}
