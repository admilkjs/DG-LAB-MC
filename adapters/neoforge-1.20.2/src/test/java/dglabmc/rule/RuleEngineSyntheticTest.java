package dglabmc.rule;

import dglabmc.config.AppConfig;
import dglabmc.config.ConfigRepository;
import dglabmc.device.DeviceChannel;
import dglabmc.device.DeviceSessionManager;
import dglabmc.wave.WaveformDefinition;
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

        RecordingSessionManager sessionManager = new RecordingSessionManager();
        try {
            RuleEngine engine = new RuleEngine(repository, sessionManager);

            RuleEventContext context = new RuleEventContext();
            context.triggerId = TriggerRegistry.PLAYER_HURT;
            context.damage = 3.0F;
            context.currentHealth = 16.0F;
            context.maxHealth = 20.0F;

            engine.fireSynthetic(context);

            assertFalse(sessionManager.strengthChanges.isEmpty(), "伪造事件应该先下发强度");
            assertEquals(1, sessionManager.pulseCalls.size(), "伪造事件应该单独下发一次波形");
            PulseCall pulseCall = sessionManager.pulseCalls.get(0);
            assertEquals(DeviceChannel.A, pulseCall.channel, "测试规则只应命中 A 通道");
            assertEquals(Collections.singletonList("0A0A0A0A64646464"), pulseCall.frames, "下发的波形帧应与规则绑定的波形一致");
            assertEquals(true, pulseCall.clearBeforeSend, "伪造事件下发波形前应先清空旧波形");
        } finally {
            sessionManager.shutdown();
        }
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

    private static final class RecordingSessionManager extends DeviceSessionManager {
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
        public synchronized DeviceSnapshot snapshot() {
            DeviceSnapshot snapshot = new DeviceSnapshot();
            snapshot.connected = true;
            snapshot.bound = true;
            snapshot.maxStrengthA = 200;
            snapshot.maxStrengthB = 200;
            return snapshot;
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
