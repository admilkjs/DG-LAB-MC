package dglabmc.tool;

import dglabmc.core.config.AppConfig;
import dglabmc.core.config.ConfigArchiveService;
import dglabmc.core.rule.ChannelTarget;
import dglabmc.core.rule.RuleDefinition;
import dglabmc.core.rule.StrengthAction;
import dglabmc.core.rule.TriggerRegistry;
import dglabmc.core.wave.WaveformDefinition;
import dglabmc.core.wave.WaveformImportService;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PvpPunishConfigGenerator {
    private PvpPunishConfigGenerator() {
    }

    public static void main(String[] args) throws Exception {
        Path projectDir = Paths.get("").toAbsolutePath().normalize();
        Path output = args != null && args.length > 0
            ? Paths.get(args[0]).toAbsolutePath().normalize()
            : projectDir.resolve("dist").resolve("dglabmc-pvp-punish.zip");

        AppConfig config = createConfig(projectDir);
        Files.createDirectories(output.getParent());
        ConfigArchiveService archiveService = new ConfigArchiveService();
        try (OutputStream outputStream = Files.newOutputStream(output)) {
            archiveService.writeArchive(outputStream, config, "1.0.1", "pvp-punish-pack");
        }
        System.out.println("PVP punish config created: " + output);
    }

    private static AppConfig createConfig(Path projectDir) throws IOException {
        AppConfig config = new AppConfig();
        config.connection.deviceClientId = UUID.randomUUID().toString();
        config.ui.lastOpenedTab = "transfer";
        config.ui.accentPreset = "industrial";
        config.strength.maxStrengthA = 200;
        config.strength.maxStrengthB = 200;
        applyPunishProfile(config.strength.channelA);
        applyPunishProfile(config.strength.channelB);

        WaveformImportService importService = new WaveformImportService();
        Map<String, WaveformDefinition> waveforms = new LinkedHashMap<String, WaveformDefinition>();
        waveforms.put("刺", importPulse(importService, projectDir.resolve("pulse-刺-2460726.pulse"), "惩罚-刺", "短促刺激，用于受伤加压"));
        waveforms.put("强弱强", importPulse(importService, projectDir.resolve("pulse-强弱强-2417239.pulse"), "惩罚-强弱强", "强弱交替，用于图腾和装备告警"));
        waveforms.put("疼痛刺激", importPulse(importService, projectDir.resolve("pulse-疼痛刺激-2402081.pulse"), "惩罚-疼痛刺激", "持续刺痛，用于低血量和低饥饿"));
        waveforms.put("突然", importPulse(importService, projectDir.resolve("pulse-突然-2440583.pulse"), "惩罚-突然", "突发强刺激，用于被击杀"));

        config.waveforms = new ArrayList<WaveformDefinition>(waveforms.values());
        config.rules = createRules(waveforms);
        return config;
    }

    private static void applyPunishProfile(AppConfig.ChannelStrengthProfile profile) {
        profile.eventStrength = 12;
        profile.damageScale = 6.0D;
        profile.delayMs = 3800;
        profile.decayIntervalMs = 250;
        profile.decayValue = 3;
        profile.deathStrength = 110;
        profile.deathDelayMs = 5200;
        profile.minStrength = 0;
    }

    private static List<RuleDefinition> createRules(Map<String, WaveformDefinition> waveforms) {
        List<RuleDefinition> rules = new ArrayList<RuleDefinition>();
        rules.add(increaseRule("rule-pvp-killed", 0, "被击杀加压", TriggerRegistry.PLAYER_KILLED_BY_OTHER, ChannelTarget.BOTH, waveforms.get("突然").id, 1.0D, 3600L));
        rules.add(increaseRule("rule-pvp-totem", 1, "图腾触发加压", TriggerRegistry.TOTEM_TRIGGER, ChannelTarget.BOTH, waveforms.get("强弱强").id, 2.4D, 2400L));

        RuleDefinition lowHealth = increaseRule("rule-pvp-low-health", 2, "低血量加压", TriggerRegistry.LOW_HEALTH, ChannelTarget.BOTH, waveforms.get("疼痛刺激").id, 1.5D, 3200L);
        lowHealth.conditions.put("threshold", Double.valueOf(10.0D));
        rules.add(lowHealth);

        RuleDefinition lowFood = increaseRule("rule-pvp-low-food", 3, "低饥饿加压", TriggerRegistry.LOW_FOOD, ChannelTarget.BOTH, waveforms.get("疼痛刺激").id, 0.9D, 2600L);
        lowFood.conditions.put("threshold", Double.valueOf(6.0D));
        rules.add(lowFood);

        RuleDefinition lowArmor = increaseRule("rule-pvp-low-armor", 4, "护甲低耐久加压", TriggerRegistry.ARMOR_LOW, ChannelTarget.BOTH, waveforms.get("强弱强").id, 1.2D, 2600L);
        lowArmor.conditions.put("ratio", Double.valueOf(0.18D));
        rules.add(lowArmor);

        rules.add(increaseRule("rule-pvp-hurt", 5, "受伤加压", TriggerRegistry.PLAYER_HURT, ChannelTarget.BOTH, waveforms.get("刺").id, 1.0D, 500L));
        rules.add(decreaseRule("rule-pvp-heal", 6, "治疗减压", TriggerRegistry.PLAYER_HEAL, ChannelTarget.BOTH, 0.7D, 500L));
        rules.add(decreaseRule("rule-pvp-block", 7, "格挡减压", TriggerRegistry.SHIELD_BLOCK, ChannelTarget.BOTH, 0.5D, 500L));
        rules.add(decreaseRule("rule-pvp-hit-player", 8, "命中玩家减压", TriggerRegistry.ATTACK_DAMAGE_PLAYER, ChannelTarget.BOTH, 0.6D, 150L));
        rules.add(decreaseRule("rule-pvp-crit-player", 9, "暴击玩家减压", TriggerRegistry.ATTACK_CRITICAL_PLAYER, ChannelTarget.BOTH, 1.0D, 250L));
        rules.add(decreaseRule("rule-pvp-kill-player", 10, "击杀玩家减压", TriggerRegistry.ATTACK_KILL_PLAYER, ChannelTarget.BOTH, 3.2D, 1200L));
        return rules;
    }

    private static RuleDefinition increaseRule(String id, int orderGroup, String name, String trigger, ChannelTarget channel, String waveformId, double scale, long cooldownMs) {
        RuleDefinition rule = baseRule(id, orderGroup, name, trigger, channel, cooldownMs);
        rule.waveformId = waveformId;
        rule.strengthAction = StrengthAction.INCREASE;
        rule.strengthScale = scale;
        rule.clearBeforeSend = true;
        return rule;
    }

    private static RuleDefinition decreaseRule(String id, int orderGroup, String name, String trigger, ChannelTarget channel, double scale, long cooldownMs) {
        RuleDefinition rule = baseRule(id, orderGroup, name, trigger, channel, cooldownMs);
        rule.waveformId = "";
        rule.strengthAction = StrengthAction.DECREASE;
        rule.strengthScale = scale;
        rule.clearBeforeSend = false;
        return rule;
    }

    private static RuleDefinition baseRule(String id, int orderGroup, String name, String trigger, ChannelTarget channel, long cooldownMs) {
        RuleDefinition rule = new RuleDefinition();
        rule.id = id;
        rule.name = name;
        rule.enabled = true;
        rule.orderGroup = orderGroup;
        rule.trigger = trigger;
        rule.channel = channel;
        rule.cooldownMs = cooldownMs;
        rule.conditions = new LinkedHashMap<String, Double>();
        return rule;
    }

    private static WaveformDefinition importPulse(WaveformImportService importService, Path path, String name, String description) throws IOException {
        String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).trim();
        if (!source.startsWith("Dungeonlab+pulse:")) {
            throw new IOException("Invalid pulse file: " + path.toAbsolutePath());
        }
        return importService.importPulse(name, description, source);
    }
}
