package cn.admilk.dglabweb.config;

import cn.admilk.dglabweb.rule.RuleDefinition;
import cn.admilk.dglabweb.rule.RuleProcessingMode;
import cn.admilk.dglabweb.wave.WaveformDefinition;

import java.util.ArrayList;
import java.util.List;

public class AppConfig {
    public static final int CURRENT_SCHEMA_VERSION = 9;

    public int schemaVersion = CURRENT_SCHEMA_VERSION;
    public String loaderFlavor = "forge-1.16.5";
    @Deprecated
    public RuleProcessingMode ruleProcessingMode = RuleProcessingMode.PARALLEL;
    public ConnectionPreferences connection = new ConnectionPreferences();
    public StrengthPreferences strength = new StrengthPreferences();
    public UiPreferences ui = new UiPreferences();
    public List<WaveformDefinition> waveforms = new ArrayList<WaveformDefinition>();
    public List<RuleDefinition> rules = new ArrayList<RuleDefinition>();

    public static class ConnectionPreferences {
        public String localBindAddress = "127.0.0.1";
        public String deviceAdvertisedAddress = "";
        public String deviceClientId = "";
        public String lastResolvedLanIp = "";
    }

    public static class UiPreferences {
        public String lastOpenedTab = "dashboard";
        public String accentPreset = "industrial";
    }

    public static class StrengthPreferences {
        @Deprecated
        public int baseStrength = 20;
        @Deprecated
        public int maxStrength = 60;
        public int maxStrengthA = 60;
        public int maxStrengthB = 60;
        public ChannelStrengthProfile channelA = new ChannelStrengthProfile();
        public ChannelStrengthProfile channelB = new ChannelStrengthProfile();
    }

    public static class ChannelStrengthProfile {
        public double damageScale = 3.0D;
        public int eventStrength = 18;
        public int delayMs = 2500;
        public int decayIntervalMs = 250;
        public int decayValue = 2;
        public int deathStrength = 50;
        public int deathDelayMs = 2500;
        public int minStrength = 0;
    }
}
