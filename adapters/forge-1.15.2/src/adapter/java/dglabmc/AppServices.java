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

/** Forge 1.15.2 facade. All business logic remains in Core. */
public final class AppServices {
    private static final AppServices INSTANCE = new AppServices();
    private CoreAppServices delegate;
    private DeviceSessionManager sessionManager;
    private AppServices() {}
    public static AppServices get() { return INSTANCE; }
    public synchronized void initialize() {
        if (delegate == null) {
            Path root = PlatformServices.paths().resolveConfigDirectory(DgLabMcMod.MODID);
            RuleFeedback feedback = new RuleFeedback() {
                @Override public void reportRuleRowContinue(int rowIndex) {
                    dglabmc.client.ClientCommandRouter.reportRuleRowContinue(rowIndex);
                }
                @Override public void reportRuleRowMatched(int rowIndex, List<String> ruleNames) {
                    dglabmc.client.ClientCommandRouter.reportRuleRowMatched(rowIndex, ruleNames);
                }
                @Override public void reportRuleStop(int rowIndex) {
                    dglabmc.client.ClientCommandRouter.reportRuleStop(rowIndex);
                }
                @Override public void reportRuleNoMatch() {
                    dglabmc.client.ClientCommandRouter.reportRuleNoMatch();
                }
            };
            delegate = new CoreAppServices(root, "forge-1.15.2", DgLabMcMod.VERSION, clientId -> {
                sessionManager = new DeviceSessionManager();
                return new ForgeDeviceRuntime(sessionManager, new DeviceWebSocketServer(sessionManager, clientId));
            }, dglabmc.config.StartupConfig::resolveDevicePort, feedback);
        }
        delegate.initialize();
    }
    public synchronized void shutdown() { if (delegate != null) delegate.shutdown(); delegate = null; sessionManager = null; }
    public synchronized AppConfig getConfig() { return delegate().getConfig(); }
    public synchronized void saveConfig(AppConfig c) { delegate().saveConfig(c); }
    public synchronized void saveRule(RuleDefinition r) { delegate().saveRule(r); }
    public synchronized void deleteRule(String id) { delegate().deleteRule(id); }
    public synchronized void saveWaveform(WaveformDefinition w) { delegate().saveWaveform(w); }
    public synchronized void deleteWaveform(String id) { delegate().deleteWaveform(id); }
    public synchronized WaveformDefinition importPulseWaveform(String n,String d,String s){return delegate().importPulseWaveform(n,d,s);}
    public synchronized WaveformDefinition importHexWaveform(String n,String d,String s){return delegate().importHexWaveform(n,d,s);}
    public synchronized Path exportConfigArchive(){return delegate().exportConfigArchive();}
    public synchronized void importConfigArchive(InputStream i){delegate().importConfigArchive(i);}
    public synchronized DeviceSessionManager.DeviceSnapshot getDeviceSnapshot(){
        dglabmc.core.device.DeviceSnapshot source = delegate().getDeviceSnapshot();
        DeviceSessionManager.DeviceSnapshot target = new DeviceSessionManager.DeviceSnapshot();
        target.connected = source.connected;
        target.bound = source.bound;
        target.clientId = source.clientId;
        target.targetId = source.targetId;
        target.currentStrengthA = source.currentStrengthA;
        target.currentStrengthB = source.currentStrengthB;
        target.maxStrengthA = source.maxStrengthA;
        target.maxStrengthB = source.maxStrengthB;
        target.recentMessages = source.recentMessages;
        return target;
    }
    public synchronized boolean isDeviceBound(){return delegate().isDeviceBound();}
    public synchronized RuleEngine getRuleEngine(){return delegate().getRuleEngine();}
    public synchronized RuleEngine.RuntimeSnapshot getRuleRuntimeSnapshot(){return delegate().getRuleRuntimeSnapshot();}
    public synchronized String getPairingLink(){return delegate().getPairingLink();}
    public synchronized String refreshPairingLink(){return delegate().refreshPairingLink();}
    public synchronized int getDevicePort(){return delegate().getDevicePort();}
    public synchronized boolean isDeviceServerRunning(){return delegate().isDeviceServerRunning();}
    public synchronized String getDeviceServerBindAddress(){return delegate().getDeviceServerBindAddress();}
    public synchronized String getDeviceServerError(){return delegate().getDeviceServerError();}
    public synchronized List<TriggerDefinition> getTriggers(){return delegate().getTriggers();}
    public synchronized void testWaveform(String id, DeviceChannel c){delegate().testWaveform(id,c);}
    public synchronized void testRule(RuleDefinition r){delegate().testRule(r);}
    public synchronized void restoreDefaultConfig(){delegate().restoreDefaultConfig();}
    private CoreAppServices delegate(){ if(delegate == null) initialize(); return delegate; }
}
