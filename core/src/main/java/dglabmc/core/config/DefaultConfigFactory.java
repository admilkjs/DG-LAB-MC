package dglabmc.core.config;

import dglabmc.core.rule.ChannelTarget;
import dglabmc.core.rule.RuleDefinition;
import dglabmc.core.rule.TriggerRegistry;
import dglabmc.core.util.HexWaveformUtil;
import dglabmc.core.wave.WaveformDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DefaultConfigFactory {
    private DefaultConfigFactory() {
    }

    public static AppConfig create() {
        AppConfig config = new AppConfig();
        config.connection.deviceClientId = UUID.randomUUID().toString();
        config.strength.maxStrengthA = 60;
        config.strength.maxStrengthB = 60;
        config.strength.channelA.damageScale = 3.0D;
        config.strength.channelA.eventStrength = 18;
        config.strength.channelA.delayMs = 2500;
        config.strength.channelA.decayIntervalMs = 250;
        config.strength.channelA.decayValue = 2;
        config.strength.channelA.deathStrength = 50;
        config.strength.channelA.deathDelayMs = 2500;
        config.strength.channelA.minStrength = 0;
        config.strength.channelB.damageScale = 3.0D;
        config.strength.channelB.eventStrength = 18;
        config.strength.channelB.delayMs = 2500;
        config.strength.channelB.decayIntervalMs = 250;
        config.strength.channelB.decayValue = 2;
        config.strength.channelB.deathStrength = 50;
        config.strength.channelB.deathDelayMs = 2500;
        config.strength.channelB.minStrength = 0;
        config.waveforms = createWaveforms();
        config.rules = createRules(config.waveforms);
        return config;
    }

    private static List<WaveformDefinition> createWaveforms() {
        List<WaveformDefinition> waveforms = new ArrayList<WaveformDefinition>();
        waveforms.add(HexWaveformUtil.createWaveform(
            "受伤脉冲",
            "受伤时的快速上冲波形",
            "builtin",
            "",
            HexWaveformUtil.swellPulse(70, 10, 72, 16)
        ));
        waveforms.add(HexWaveformUtil.createWaveform(
            "治疗回升",
            "治疗时较平缓的回升波形",
            "builtin",
            "",
            HexWaveformUtil.swellPulse(95, 6, 38, 12)
        ));
        waveforms.add(HexWaveformUtil.createWaveform(
            "死亡警报",
            "死亡时使用的高强度警报波形",
            "builtin",
            "",
            HexWaveformUtil.warningPulse(15, 85, 4)
        ));
        waveforms.add(HexWaveformUtil.createWaveform(
            "低血量预警",
            "低血量时循环提醒波形",
            "builtin",
            "",
            HexWaveformUtil.warningPulse(12, 48, 3)
        ));
        return waveforms;
    }

    private static List<RuleDefinition> createRules(List<WaveformDefinition> waveforms) {
        List<RuleDefinition> rules = new ArrayList<RuleDefinition>();
        rules.add(rule("rule-damage", 0, "受伤反馈", TriggerRegistry.PLAYER_HURT, ChannelTarget.A, findId(waveforms, "受伤脉冲"), 650L));
        rules.add(rule("rule-heal", 1, "治疗反馈", TriggerRegistry.PLAYER_HEAL, ChannelTarget.A, findId(waveforms, "治疗回升"), 1200L));
        rules.add(rule("rule-death", 2, "死亡警报", TriggerRegistry.PLAYER_DEATH, ChannelTarget.BOTH, findId(waveforms, "死亡警报"), 2000L));

        RuleDefinition warning = rule("rule-low-health", 3, "低血量预警", TriggerRegistry.LOW_HEALTH, ChannelTarget.B, findId(waveforms, "低血量预警"), 2500L);
        warning.conditions.put("threshold", Double.valueOf(6.0D));
        rules.add(warning);
        return rules;
    }

    private static RuleDefinition rule(String id, int orderGroup, String name, String trigger, ChannelTarget channel, String waveformId, long cooldown) {
        RuleDefinition definition = new RuleDefinition();
        definition.id = id;
        definition.orderGroup = orderGroup;
        definition.name = name;
        definition.trigger = trigger;
        definition.channel = channel;
        definition.waveformId = waveformId;
        definition.cooldownMs = cooldown;
        return definition;
    }

    private static String findId(List<WaveformDefinition> waveforms, String name) {
        for (WaveformDefinition waveform : waveforms) {
            if (name.equals(waveform.name)) {
                return waveform.id;
            }
        }
        return "";
    }
}

