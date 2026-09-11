package dglabmc.core.rule;

import dglabmc.core.config.AppConfig;
import dglabmc.core.config.ConfigRepository;
import dglabmc.core.device.DeviceChannel;
import dglabmc.core.device.DeviceChannelState;
import dglabmc.core.device.DeviceTransport;
import dglabmc.core.wave.WaveformDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RuleEngineSyntheticTest {
    @TempDir
    Path tempDir;

    @Test
    void syntheticTriggerStillSendsPulse() throws Exception {
        ConfigRepository repository = new ConfigRepository(tempDir);
        repository.save(buildConfig());

        RecordingTransport transport = new RecordingTransport();
        RuleEngine engine = new RuleEngine(repository, transport, RuleFeedback.NOOP);

        RuleEventContext context = new RuleEventContext();
        context.triggerId = TriggerRegistry.PLAYER_HURT;
        context.damage = 3.0F;
        context.currentHealth = 16.0F;
        context.maxHealth = 20.0F;

        engine.fireSynthetic(context);

        assertFalse(transport.strengthChanges.isEmpty(), "伪造事件应该先下发强度");
        assertEquals(1, transport.pulseCalls.size(), "伪造事件应该单独下发一次波形");
        PulseCall pulseCall = transport.pulseCalls.get(0);
        assertEquals(DeviceChannel.A, pulseCall.channel, "测试规则只应命中 A 通道");
        assertEquals(Collections.singletonList("0A0A0A0A64646464"), pulseCall.frames, "下发的波形帧应与规则绑定的波形一致");
        assertEquals(true, pulseCall.clearBeforeSend, "伪造事件下发波形前应先清空旧波形");
    }

    private AppConfig buildConfig() {
        AppConfig config = new AppConfig();
        config.waveforms = new ArrayList<WaveformDefinition>();
        config.rules = new ArrayList<RuleDefinition>();
        config.strength.maxStrengthA = 200;
        config.strength.maxStrengthB = 200;
        config.strength.channelA.eventStrength = 12;
        config.strength.channelA.damageScale = 3.0D;
        config.strength.channelB.eventStrength = 12;
        config.strength.channelB.damageScale = 3.0D;

        WaveformDefinition waveform = new WaveformDefinition();
        waveform.id = "wave-hurt";
        waveform.name = "test-wave";
        waveform.frames = new ArrayList<String>(Collections.singletonList("0A0A0A0A64646464"));
        waveform.estimatedDurationMs = 100L;
        config.waveforms.add(waveform);

        RuleDefinition rule = new RuleDefinition();
        rule.id = "rule-hurt";
        rule.name = "受伤";
        rule.trigger = TriggerRegistry.PLAYER_HURT;
        rule.channel = ChannelTarget.A;
        rule.waveformId = waveform.id;
        rule.cooldownMs = 0L;
        config.rules.add(rule);
        return config;
    }

    private static final class RecordingTransport implements DeviceTransport {
        final List<Integer> strengthChanges = new ArrayList<Integer>();
        final List<PulseCall> pulseCalls = new ArrayList<PulseCall>();

        @Override
        public synchronized boolean isBound() {
            return true;
        }

        @Override
        public synchronized void setStrength(DeviceChannel channel, int value) {
            strengthChanges.add(Integer.valueOf(value));
        }

        @Override
        public synchronized void sendPulse(DeviceChannel channel, List<String> frames, boolean clearBeforeSend) {
            pulseCalls.add(new PulseCall(channel, new ArrayList<String>(frames), clearBeforeSend));
        }

        @Override
        public synchronized void clearPulse(DeviceChannel channel) {
        }

        @Override
        public synchronized DeviceChannelState snapshot(DeviceChannel channel) {
            return new DeviceChannelState(channel, 0, 200, false);
        }
    }

    private static final class PulseCall {
        final DeviceChannel channel;
        final List<String> frames;
        final boolean clearBeforeSend;

        private PulseCall(DeviceChannel channel, List<String> frames, boolean clearBeforeSend) {
            this.channel = channel;
            this.frames = frames;
            this.clearBeforeSend = clearBeforeSend;
        }
    }
}
