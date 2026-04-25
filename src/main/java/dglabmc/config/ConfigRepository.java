package dglabmc.config;

import dglabmc.rule.RuleDefinition;
import dglabmc.rule.TriggerDefinition;
import dglabmc.rule.TriggerRegistry;
import dglabmc.rule.RuleProcessingMode;
import dglabmc.rule.StrengthAction;
import dglabmc.wave.WaveformDefinition;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;

public class ConfigRepository {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path rootDirectory;
    private final Path configFile;
    private AppConfig current;

    public ConfigRepository(Path rootDirectory) {
        this.rootDirectory = rootDirectory;
        this.configFile = rootDirectory.resolve("app-config.json");
    }

    public synchronized AppConfig load() throws IOException {
        Files.createDirectories(this.rootDirectory);
        if (!Files.exists(this.configFile)) {
            this.current = normalize(DefaultConfigFactory.create());
            save(this.current);
            return this.current;
        }

        try (Reader reader = Files.newBufferedReader(this.configFile, StandardCharsets.UTF_8)) {
            AppConfig loaded = gson.fromJson(reader, AppConfig.class);
            this.current = normalize(loaded);
            save(this.current);
            return this.current;
        }
    }

    public synchronized void save(AppConfig config) throws IOException {
        AppConfig normalized = normalize(config);
        Files.createDirectories(this.rootDirectory);
        try (Writer writer = Files.newBufferedWriter(this.configFile, StandardCharsets.UTF_8)) {
            gson.toJson(normalized, writer);
        }
        this.current = normalized;
    }

    public synchronized AppConfig getCurrent() throws IOException {
        if (this.current == null) {
            return load();
        }
        return this.current;
    }

    public synchronized Path getRootDirectory() {
        return this.rootDirectory;
    }

    public synchronized AppConfig importConfig(AppConfig imported) throws IOException {
        this.current = normalize(imported);
        save(this.current);
        return this.current;
    }

    public synchronized WaveformDefinition findWaveform(String waveformId) throws IOException {
        AppConfig config = getCurrent();
        for (WaveformDefinition waveform : config.waveforms) {
            if (waveformId.equals(waveform.id)) {
                return waveform;
            }
        }
        return null;
    }

    private AppConfig normalize(AppConfig config) {
        AppConfig resolved = config == null ? DefaultConfigFactory.create() : config;
        int loadedSchemaVersion = resolved.schemaVersion <= 0 ? AppConfig.CURRENT_SCHEMA_VERSION : resolved.schemaVersion;
        if (resolved.loaderFlavor == null || resolved.loaderFlavor.trim().isEmpty()) {
            resolved.loaderFlavor = "neoforge-1.20.2";
        }
        if (resolved.ruleProcessingMode == null) {
            resolved.ruleProcessingMode = RuleProcessingMode.PARALLEL;
        }
        if (resolved.connection == null) {
            resolved.connection = new AppConfig.ConnectionPreferences();
        }
        if (resolved.strength == null) {
            resolved.strength = new AppConfig.StrengthPreferences();
        }
        normalizeStrengthPreferences(resolved.strength, loadedSchemaVersion);
        if (resolved.ui == null) {
            resolved.ui = new AppConfig.UiPreferences();
        }
        if (resolved.ui.lastOpenedTab == null || resolved.ui.lastOpenedTab.trim().isEmpty()) {
            resolved.ui.lastOpenedTab = "dashboard";
        }
        if (loadedSchemaVersion < 12) {
            resolved.ui.showPlayerStatus = true;
        }
        normalizeHudOverlayPreferences(resolved.ui, loadedSchemaVersion);
        if (resolved.waveforms == null) {
            resolved.waveforms = DefaultConfigFactory.create().waveforms;
        }
        if (resolved.rules == null) {
            resolved.rules = DefaultConfigFactory.create().rules;
        }
        if (resolved.connection.deviceClientId == null || resolved.connection.deviceClientId.trim().isEmpty()) {
            resolved.connection.deviceClientId = UUID.randomUUID().toString();
        }

        RuleProcessingMode legacyRuleProcessingMode = resolved.ruleProcessingMode == null ? RuleProcessingMode.PARALLEL : resolved.ruleProcessingMode;
        for (WaveformDefinition waveform : resolved.waveforms) {
            if (waveform.id == null || waveform.id.trim().isEmpty()) {
                waveform.id = UUID.randomUUID().toString();
            }
            if (waveform.frames == null) {
                waveform.frames = new ArrayList<String>();
            }
            normalizeBuiltinWaveform(waveform);
            waveform.estimatedDurationMs = waveform.frames.size() * 100L;
        }

        for (RuleDefinition rule : resolved.rules) {
            if (rule.id == null || rule.id.trim().isEmpty()) {
                rule.id = "rule-" + UUID.randomUUID().toString();
            }
            rule.trigger = normalizeTrigger(rule.trigger);
            if (rule.strengthAction == null) {
                rule.strengthAction = StrengthAction.INCREASE;
            }
            if (rule.strengthScale <= 0.0D) {
                rule.strengthScale = 1.0D;
            }
            if (rule.name == null || rule.name.trim().isEmpty()) {
                rule.name = defaultRuleName(rule.trigger);
            }
            if (rule.conditions == null) {
                rule.conditions = new LinkedHashMap<String, Double>();
            }
            rule.baseStrength = clamp(rule.baseStrength, 0, 200);
            rule.maxStrength = clamp(rule.maxStrength, rule.baseStrength, 200);
        }
        normalizeRuleOrderGroups(resolved.rules, loadedSchemaVersion, legacyRuleProcessingMode);
        resolved.schemaVersion = AppConfig.CURRENT_SCHEMA_VERSION;
        return resolved;
    }

    private String normalizeTrigger(String triggerId) {
        if (TriggerRegistry.PLAYER_KILL.equals(triggerId)) {
            return TriggerRegistry.ATTACK_KILL_ALL;
        }
        if (TriggerRegistry.CRITICAL_ATTACK.equals(triggerId)) {
            return TriggerRegistry.ATTACK_CRITICAL_ALL;
        }
        return triggerId;
    }

    private void normalizeStrengthPreferences(AppConfig.StrengthPreferences strength, int loadedSchemaVersion) {
        strength.baseStrength = clamp(strength.baseStrength, 0, 200);
        strength.maxStrength = clamp(strength.maxStrength, Math.max(0, strength.baseStrength), 200);
        if (strength.channelA == null) {
            strength.channelA = new AppConfig.ChannelStrengthProfile();
        }
        if (strength.channelB == null) {
            strength.channelB = new AppConfig.ChannelStrengthProfile();
        }

        int fallbackMax = strength.maxStrength > 0 ? strength.maxStrength : 60;
        if (strength.maxStrengthA <= 0) {
            strength.maxStrengthA = fallbackMax;
        }
        if (strength.maxStrengthB <= 0) {
            strength.maxStrengthB = fallbackMax;
        }
        strength.maxStrengthA = clamp(strength.maxStrengthA, 0, 200);
        strength.maxStrengthB = clamp(strength.maxStrengthB, 0, 200);

        boolean migrateLegacyStrength = loadedSchemaVersion < 5;
        normalizeChannelProfile(strength.channelA, strength.baseStrength, migrateLegacyStrength);
        normalizeChannelProfile(strength.channelB, strength.baseStrength, migrateLegacyStrength);
    }

    private void normalizeChannelProfile(AppConfig.ChannelStrengthProfile profile, int legacyBaseStrength, boolean migrateLegacyStrength) {
        if (migrateLegacyStrength && profile.eventStrength <= 0 && legacyBaseStrength > 0) {
            profile.eventStrength = legacyBaseStrength;
        }
        profile.damageScale = clamp(profile.damageScale, 0.0D, 20.0D);
        profile.eventStrength = clamp(profile.eventStrength, 0, 200);
        profile.delayMs = clamp(profile.delayMs, 0, 600000);
        profile.decayIntervalMs = clamp(profile.decayIntervalMs, 50, 600000);
        profile.decayValue = clamp(profile.decayValue, 0, 200);
        profile.deathStrength = clamp(profile.deathStrength, 0, 200);
        if (migrateLegacyStrength && profile.deathDelayMs <= 0) {
            profile.deathDelayMs = profile.delayMs;
        }
        profile.deathDelayMs = clamp(profile.deathDelayMs, 0, 600000);
        profile.minStrength = clamp(profile.minStrength, 0, 200);
    }

    private void normalizeHudOverlayPreferences(AppConfig.UiPreferences ui, int loadedSchemaVersion) {
        if (ui.hudOverlay == null) {
            ui.hudOverlay = new AppConfig.HudOverlayPreferences();
        }
        if (loadedSchemaVersion < 12) {
            ui.hudOverlay.enabled = true;
        }
        if (ui.hudOverlay.anchor == null || ui.hudOverlay.anchor.trim().isEmpty()) {
            ui.hudOverlay.anchor = "top_right";
        }
        ui.hudOverlay.anchor = ui.hudOverlay.anchor.trim().toLowerCase();
        if (!"top_left".equals(ui.hudOverlay.anchor)
            && !"top_right".equals(ui.hudOverlay.anchor)
            && !"bottom_left".equals(ui.hudOverlay.anchor)
            && !"bottom_right".equals(ui.hudOverlay.anchor)) {
            ui.hudOverlay.anchor = "top_right";
        }
        ui.hudOverlay.offsetX = clamp(ui.hudOverlay.offsetX, -4000, 4000);
        ui.hudOverlay.offsetY = clamp(ui.hudOverlay.offsetY, -4000, 4000);
        if (loadedSchemaVersion < 11 || ui.hudOverlay.relativeX == null || ui.hudOverlay.relativeY == null) {
            ui.hudOverlay.relativeX = Double.valueOf(migrateHudRelativeX(ui.hudOverlay.anchor, ui.hudOverlay.offsetX));
            ui.hudOverlay.relativeY = Double.valueOf(migrateHudRelativeY(ui.hudOverlay.anchor, ui.hudOverlay.offsetY));
        }
        ui.hudOverlay.relativeX = Double.valueOf(clamp(safeDouble(ui.hudOverlay.relativeX, 1.0D), 0.0D, 1.0D));
        ui.hudOverlay.relativeY = Double.valueOf(clamp(safeDouble(ui.hudOverlay.relativeY, 0.0D), 0.0D, 1.0D));
        ui.hudOverlay.scale = clamp(ui.hudOverlay.scale, 0.5D, 4.0D);
        ui.hudOverlay.panelOpacity = clamp(ui.hudOverlay.panelOpacity, 0.15D, 1.0D);
        ui.hudOverlay.filterOpacity = clamp(ui.hudOverlay.filterOpacity, 0.0D, 0.8D);
    }

    private double migrateHudRelativeX(String anchor, int offsetX) {
        int margin = clamp(Math.abs(offsetX), 0, 4000);
        double normalizedMargin = clamp(margin / 1600.0D, 0.0D, 0.95D);
        if ("top_left".equals(anchor) || "bottom_left".equals(anchor)) {
            return normalizedMargin;
        }
        return 1.0D - normalizedMargin;
    }

    private double migrateHudRelativeY(String anchor, int offsetY) {
        int margin = clamp(Math.abs(offsetY), 0, 4000);
        double normalizedMargin = clamp(margin / 900.0D, 0.0D, 0.95D);
        if ("bottom_left".equals(anchor) || "bottom_right".equals(anchor)) {
            return 1.0D - normalizedMargin;
        }
        return normalizedMargin;
    }

    private double safeDouble(Double value, double fallback) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return fallback;
        }
        return value.doubleValue();
    }

    private void normalizeBuiltinWaveform(WaveformDefinition waveform) {
        if (!"builtin".equals(waveform.sourceType)) {
            return;
        }

        if ("Damage Spike".equals(waveform.name) || "builtin-受击".equals(waveform.name)) {
            waveform.name = "受击脉冲";
        } else if ("Recovery Rise".equals(waveform.name) || "builtin-治疗".equals(waveform.name)) {
            waveform.name = "治疗回升";
        } else if ("Critical Alarm".equals(waveform.name) || "builtin-死亡".equals(waveform.name)) {
            waveform.name = "死亡警报";
        } else if ("Low Health Warning".equals(waveform.name) || "builtin-预警".equals(waveform.name)) {
            waveform.name = "低血量预警";
        }

        if ("Damage Spike".equals(waveform.description) || "受击脉冲".equals(waveform.description)) {
            waveform.description = "受击时的快速上冲波形";
        } else if ("Recovery Rise".equals(waveform.description) || "治疗回升".equals(waveform.description)) {
            waveform.description = "治疗时较平缓的回升波形";
        } else if ("Critical Alarm".equals(waveform.description) || "死亡警报".equals(waveform.description)) {
            waveform.description = "死亡时使用的高强度警报波形";
        } else if ("Low Health Warning".equals(waveform.description) || "低血量预警".equals(waveform.description)) {
            waveform.description = "低血量时循环提醒波形";
        }
    }

    private void normalizeRuleOrderGroups(java.util.List<RuleDefinition> rules, int loadedSchemaVersion, RuleProcessingMode legacyRuleProcessingMode) {
        if (rules == null || rules.isEmpty()) {
            return;
        }
        if (loadedSchemaVersion < 8) {
            for (int i = 0; i < rules.size(); i++) {
                rules.get(i).orderGroup = legacyRuleProcessingMode == RuleProcessingMode.SEQUENTIAL_FIRST_MATCH ? i : 0;
            }
        }
        int normalizedGroup = -1;
        int previousRawGroup = Integer.MIN_VALUE;
        for (RuleDefinition rule : rules) {
            int rawGroup = Math.max(0, rule.orderGroup);
            if (normalizedGroup < 0 || rawGroup != previousRawGroup) {
                normalizedGroup++;
                previousRawGroup = rawGroup;
            }
            rule.orderGroup = normalizedGroup;
        }
    }

    private String defaultRuleName(String triggerId) {
        for (TriggerDefinition definition : TriggerRegistry.all()) {
            if (definition.id.equals(triggerId)) {
                return definition.label + "规则";
            }
        }
        return "新规则";
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
