package dglabmc.core.rule;

import dglabmc.core.config.AppConfig;
import dglabmc.core.config.ConfigRepository;
import dglabmc.core.device.DeviceChannel;
import dglabmc.core.device.DeviceTransport;
import dglabmc.core.device.DeviceChannelState;
import dglabmc.core.wave.WaveformDefinition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RuleEngine {
    private final ConfigRepository configRepository;
    private final DeviceTransport sessionManager;
    private final RuleFeedback feedback;
    private final Map<String, Long> cooldowns = new HashMap<String, Long>();
    private final ChannelRuntime stateA = new ChannelRuntime(DeviceChannel.A);
    private final ChannelRuntime stateB = new ChannelRuntime(DeviceChannel.B);
    private long lastTickAt = System.currentTimeMillis();

    public RuleEngine(ConfigRepository configRepository, DeviceTransport sessionManager, RuleFeedback feedback) {
        this.configRepository = configRepository;
        this.sessionManager = sessionManager;
        this.feedback = feedback == null ? RuleFeedback.NOOP : feedback;
    }

    public synchronized void fire(RuleEventContext context) {
        fireInternal(context, false);
    }

    public synchronized void fireSynthetic(RuleEventContext context) {
        fireInternal(context, true);
    }

    private void fireInternal(RuleEventContext context, boolean synthetic) {
        if (context == null || context.triggerId == null || !sessionManager.isBound()) {
            return;
        }

        try {
            AppConfig config = configRepository.getCurrent();
            long now = System.currentTimeMillis();
            List<RuleDefinition> currentGroup = new ArrayList<RuleDefinition>();
            int currentGroupId = Integer.MIN_VALUE;
            boolean matchedAnyGroup = false;
            for (RuleDefinition rule : config.rules) {
                int groupId = Math.max(0, rule.orderGroup);
                if (currentGroup.isEmpty()) {
                    currentGroupId = groupId;
                }
                if (groupId != currentGroupId) {
                    if (dispatchMatchingGroup(config, currentGroup, context, now, synthetic)) {
                        matchedAnyGroup = true;
                        return;
                    }
                    feedback.reportRuleRowContinue(currentGroupId + 1);
                    currentGroup.clear();
                    currentGroupId = groupId;
                }
                currentGroup.add(rule);
            }
            if (!currentGroup.isEmpty()) {
                if (dispatchMatchingGroup(config, currentGroup, context, now, synthetic)) {
                    matchedAnyGroup = true;
                } else {
                    feedback.reportRuleRowContinue(currentGroupId + 1);
                }
            }
            if (!matchedAnyGroup) {
                feedback.reportRuleNoMatch();
            }
        } catch (IOException ignored) {
        }
    }

    public synchronized void preview(RuleDefinition rule) {
        if (rule == null || !sessionManager.isBound()) {
            return;
        }
        try {
            AppConfig config = configRepository.getCurrent();
            WaveformDefinition waveform = findWaveform(config, rule.waveformId);
            if (waveform == null || waveform.frames == null || waveform.frames.isEmpty()) {
                return;
            }
            dispatch(config, rule, waveform, createPreviewContext(rule.trigger), true);
        } catch (IOException ignored) {
        }
    }

    public synchronized void tick(RuleEventContext liveContext) {
        long now = System.currentTimeMillis();
        long elapsedMs = Math.max(0L, now - this.lastTickAt);
        this.lastTickAt = now;

        if (!sessionManager.isBound()) {
            clearState(this.stateA, true);
            clearState(this.stateB, true);
            return;
        }

        try {
            AppConfig config = configRepository.getCurrent();
            tickChannel(this.stateA, config.strength.channelA, config.strength.maxStrengthA, liveContext, elapsedMs);
            tickChannel(this.stateB, config.strength.channelB, config.strength.maxStrengthB, liveContext, elapsedMs);
        } catch (IOException ignored) {
        }
    }

    public synchronized void reset() {
        this.cooldowns.clear();
        this.lastTickAt = System.currentTimeMillis();
        clearState(this.stateA, true);
        clearState(this.stateB, true);
    }

    public synchronized RuntimeSnapshot snapshot() {
        RuntimeSnapshot snapshot = new RuntimeSnapshot();
        snapshot.channelA = snapshotFor(this.stateA, null, 0, null);
        snapshot.channelB = snapshotFor(this.stateB, null, 0, null);
        try {
            AppConfig config = configRepository.getCurrent();
            snapshot.channelA = snapshotFor(this.stateA, config.strength.channelA, config.strength.maxStrengthA, null);
            snapshot.channelB = snapshotFor(this.stateB, config.strength.channelB, config.strength.maxStrengthB, null);
        } catch (IOException ignored) {
        }
        return snapshot;
    }

    private void dispatch(AppConfig config, RuleDefinition rule, WaveformDefinition waveform, RuleEventContext context, boolean allowImmediatePulse) {
        switch (rule.channel) {
            case BOTH:
                applyEvent(this.stateA, config.strength.channelA, config.strength.maxStrengthA, waveform, rule, context, allowImmediatePulse);
                applyEvent(this.stateB, config.strength.channelB, config.strength.maxStrengthB, waveform, rule, context, allowImmediatePulse);
                break;
            case B:
                applyEvent(this.stateB, config.strength.channelB, config.strength.maxStrengthB, waveform, rule, context, allowImmediatePulse);
                break;
            case A:
            default:
                applyEvent(this.stateA, config.strength.channelA, config.strength.maxStrengthA, waveform, rule, context, allowImmediatePulse);
                break;
        }
    }

    private void applyEvent(ChannelRuntime runtime, AppConfig.ChannelStrengthProfile profile, int configuredMax, WaveformDefinition waveform, RuleDefinition rule, RuleEventContext context, boolean allowImmediatePulse) {
        if (profile == null || rule == null) {
            return;
        }

        boolean hasWaveform = waveform != null && waveform.frames != null && !waveform.frames.isEmpty();
        if (rule.strengthAction != StrengthAction.DECREASE && !hasWaveform) {
            return;
        }

        int strengthDelta = resolveStrengthChange(profile, rule, context);
        if (strengthDelta == 0) {
            return;
        }
        int effectiveMax = resolveEffectiveMax(runtime.channel, configuredMax);
        if (effectiveMax <= 0) {
            runtime.currentStrength = 0;
            clearState(runtime, false);
            return;
        }

        runtime.currentStrength = clamp(runtime.currentStrength + strengthDelta, 0, effectiveMax);
        runtime.lastTrigger = context == null ? "" : safeString(context.triggerId);
        runtime.dynamicMinStrength = 0;
        if (strengthDelta > 0) {
            runtime.delayRemainingMs = resolveDelayMs(profile, context);
            runtime.decayAccumulatorMs = 0L;
        }
        if (hasWaveform) {
            runtime.waveformId = waveform.id;
            runtime.waveformName = waveform.name == null ? "" : waveform.name;
            runtime.waveformFrames = new ArrayList<String>(waveform.frames);
            runtime.waveformDurationMs = resolveWaveformDuration(waveform);
            runtime.waveformAccumulatorMs = runtime.waveformDurationMs;
            runtime.clearOnNextPulse = rule.clearBeforeSend || runtime.cleared || !runtime.waveformId.equals(runtime.lastDispatchedWaveformId);
        }

        applyStrength(runtime);
        if (runtime.currentStrength > 0) {
            if (hasWaveform && allowImmediatePulse) {
                sendPulse(runtime);
            } else if (!runtime.waveformFrames.isEmpty()) {
                runtime.outputActive = true;
            }
        } else {
            clearState(runtime, false);
        }
    }

    private void tickChannel(ChannelRuntime runtime, AppConfig.ChannelStrengthProfile profile, int configuredMax, RuleEventContext liveContext, long elapsedMs) {
        if (profile == null) {
            clearState(runtime, false);
            return;
        }

        int effectiveMax = resolveEffectiveMax(runtime.channel, configuredMax);
        runtime.currentStrength = clamp(runtime.currentStrength, 0, effectiveMax);
        runtime.dynamicMinStrength = 0;
        if (runtime.currentStrength <= 0 || runtime.waveformFrames.isEmpty() || effectiveMax <= 0) {
            runtime.currentStrength = 0;
            clearState(runtime, false);
            return;
        }

        runtime.delayRemainingMs = Math.max(0L, runtime.delayRemainingMs - elapsedMs);
        if (runtime.delayRemainingMs <= 0L && profile.decayValue > 0) {
            runtime.decayAccumulatorMs += elapsedMs;
            long interval = Math.max(50L, profile.decayIntervalMs);
            while (runtime.decayAccumulatorMs >= interval && runtime.currentStrength > 0) {
                runtime.decayAccumulatorMs -= interval;
                runtime.currentStrength = Math.max(0, runtime.currentStrength - profile.decayValue);
            }
        }

        applyStrength(runtime);
        if (runtime.currentStrength <= 0) {
            clearState(runtime, false);
            return;
        }

        runtime.waveformAccumulatorMs += elapsedMs;
        if (runtime.clearOnNextPulse || runtime.waveformAccumulatorMs >= runtime.waveformDurationMs) {
            sendPulse(runtime);
        } else {
            runtime.outputActive = true;
        }
    }

    private void sendPulse(ChannelRuntime runtime) {
        if (runtime.waveformFrames.isEmpty() || runtime.currentStrength <= 0) {
            return;
        }
        sessionManager.sendPulse(runtime.channel, runtime.waveformFrames, runtime.clearOnNextPulse);
        runtime.clearOnNextPulse = false;
        runtime.waveformAccumulatorMs = 0L;
        runtime.outputActive = true;
        runtime.cleared = false;
        runtime.lastDispatchedWaveformId = runtime.waveformId;
    }

    private void applyStrength(ChannelRuntime runtime) {
        int targetStrength = clamp(runtime.currentStrength, 0, 200);
        if (runtime.lastAppliedStrength == targetStrength) {
            return;
        }
        sessionManager.setStrength(runtime.channel, targetStrength);
        runtime.lastAppliedStrength = targetStrength;
    }

    private void clearState(ChannelRuntime runtime, boolean resetWaveform) {
        if (!runtime.cleared) {
            sessionManager.clearPulse(runtime.channel);
        }
        if (runtime.lastAppliedStrength != 0) {
            sessionManager.setStrength(runtime.channel, 0);
            runtime.lastAppliedStrength = 0;
        }
        runtime.currentStrength = 0;
        runtime.delayRemainingMs = 0L;
        runtime.decayAccumulatorMs = 0L;
        runtime.waveformAccumulatorMs = 0L;
        runtime.dynamicMinStrength = 0;
        runtime.outputActive = false;
        runtime.cleared = true;
        runtime.clearOnNextPulse = true;
        runtime.lastDispatchedWaveformId = "";
        if (resetWaveform) {
            runtime.waveformId = "";
            runtime.waveformName = "";
            runtime.lastTrigger = "";
            runtime.waveformFrames = Collections.emptyList();
            runtime.waveformDurationMs = 300L;
        }
    }

    private int resolveStrengthChange(AppConfig.ChannelStrengthProfile profile, RuleDefinition rule, RuleEventContext context) {
        int baseDelta = resolveStrengthDelta(profile, context);
        double scale = rule == null ? 1.0D : clamp(rule.strengthScale, 0.0D, 20.0D);
        int scaledDelta = (int) Math.round(baseDelta * scale);
        if (scaledDelta == 0 && baseDelta > 0 && scale > 0.0D) {
            scaledDelta = 1;
        }
        if (rule != null && rule.strengthAction == StrengthAction.DECREASE) {
            return -Math.max(0, scaledDelta);
        }
        return Math.max(0, scaledDelta);
    }

    private int resolveStrengthDelta(AppConfig.ChannelStrengthProfile profile, RuleEventContext context) {
        String triggerId = context == null ? "" : safeString(context.triggerId);
        double damage = context == null ? 0.0D : Math.max(0.0D, context.damage);
        double healAmount = context == null ? 0.0D : Math.max(0.0D, context.healAmount);
        if (isDamageScaledTrigger(triggerId)) {
            return Math.max(1, (int) Math.round(damage * profile.damageScale));
        }
        if (isDeathTrigger(triggerId)) {
            int damagePart = Math.max(0, (int) Math.round(damage * profile.damageScale));
            return Math.max(1, damagePart + profile.deathStrength);
        }
        if (TriggerRegistry.PLAYER_HEAL.equals(triggerId) && healAmount > 0.0D) {
            return Math.max(1, Math.max(profile.eventStrength, (int) Math.round(healAmount * Math.max(1.0D, profile.damageScale * 0.5D))));
        }
        return Math.max(0, profile.eventStrength);
    }

    private long resolveDelayMs(AppConfig.ChannelStrengthProfile profile, RuleEventContext context) {
        String triggerId = context == null ? "" : safeString(context.triggerId);
        if (isDeathTrigger(triggerId)) {
            return Math.max(0, profile.deathDelayMs);
        }
        return Math.max(0, profile.delayMs);
    }

    private RuntimeChannelSnapshot snapshotFor(ChannelRuntime runtime, AppConfig.ChannelStrengthProfile profile, int configuredMax, RuleEventContext liveContext) {
        RuntimeChannelSnapshot snapshot = new RuntimeChannelSnapshot();
        snapshot.currentStrength = runtime.currentStrength;
        snapshot.outputActive = runtime.outputActive && runtime.currentStrength > 0;
        snapshot.waveformName = runtime.waveformName;
        snapshot.triggerId = runtime.lastTrigger;
        snapshot.delayRemainingMs = runtime.delayRemainingMs;
        snapshot.dynamicMinStrength = runtime.dynamicMinStrength;
        snapshot.configuredMaxStrength = clamp(configuredMax, 0, 200);
        snapshot.effectiveMaxStrength = resolveEffectiveMax(runtime.channel, configuredMax);
        snapshot.deviceMaxStrength = resolveDeviceMax(runtime.channel);
        if (profile != null) {
            snapshot.eventStrength = profile.eventStrength;
            snapshot.damageScale = profile.damageScale;
            snapshot.delayMs = profile.delayMs;
            snapshot.decayIntervalMs = profile.decayIntervalMs;
            snapshot.decayValue = profile.decayValue;
            snapshot.deathStrength = profile.deathStrength;
            snapshot.deathDelayMs = profile.deathDelayMs;
            snapshot.minStrength = profile.minStrength;
            snapshot.dynamicMinStrength = 0;
        }
        return snapshot;
    }

    private boolean dispatchMatchingGroup(AppConfig config, List<RuleDefinition> rules, RuleEventContext context, long now, boolean synthetic) {
        boolean matched = false;
        int rowIndex = 1;
        List<String> matchedRuleNames = new ArrayList<String>();
        for (RuleDefinition rule : rules) {
            rowIndex = Math.max(rowIndex, Math.max(0, rule.orderGroup) + 1);
            ExecutableRule executable = findExecutableRule(config, rule, context, now);
            if (executable == null) {
                continue;
            }
            dispatch(config, rule, executable.waveform, context, !synthetic);
            if (synthetic) {
                forceDispatchSynthetic(config, rule, executable.waveform);
            }
            cooldowns.put(rule.id, Long.valueOf(now));
            matchedRuleNames.add(labelForDebug(rule));
            matched = true;
        }
        if (matched) {
            feedback.reportRuleRowMatched(rowIndex, matchedRuleNames);
            feedback.reportRuleStop(rowIndex);
        }
        return matched;
    }

    private void forceDispatchSynthetic(AppConfig config, RuleDefinition rule, WaveformDefinition waveform) {
        if (config == null || rule == null || waveform == null || waveform.frames == null || waveform.frames.isEmpty()) {
            return;
        }
        switch (rule.channel) {
            case BOTH:
                forceDispatchSynthetic(this.stateA, waveform);
                forceDispatchSynthetic(this.stateB, waveform);
                break;
            case B:
                forceDispatchSynthetic(this.stateB, waveform);
                break;
            case A:
            default:
                forceDispatchSynthetic(this.stateA, waveform);
                break;
        }
    }

    private void forceDispatchSynthetic(ChannelRuntime runtime, WaveformDefinition waveform) {
        if (runtime == null || waveform == null || waveform.frames == null || waveform.frames.isEmpty() || runtime.currentStrength <= 0) {
            return;
        }
        sessionManager.setStrength(runtime.channel, clamp(runtime.currentStrength, 0, 200));
        sessionManager.sendPulse(runtime.channel, waveform.frames, true);
        runtime.waveformId = waveform.id;
        runtime.waveformName = waveform.name == null ? "" : waveform.name;
        runtime.waveformFrames = new ArrayList<String>(waveform.frames);
        runtime.waveformDurationMs = resolveWaveformDuration(waveform);
        runtime.waveformAccumulatorMs = 0L;
        runtime.outputActive = true;
        runtime.clearOnNextPulse = false;
        runtime.cleared = false;
        runtime.lastDispatchedWaveformId = runtime.waveformId;
    }

    private ExecutableRule findExecutableRule(AppConfig config, RuleDefinition rule, RuleEventContext context, long now) {
        if (config == null || rule == null || !rule.enabled || !context.triggerId.equals(rule.trigger)) {
            return null;
        }
        if (!matchesConditions(rule, context)) {
            return null;
        }
        long lastRun = cooldowns.containsKey(rule.id) ? cooldowns.get(rule.id).longValue() : 0L;
        if ((now - lastRun) < Math.max(0L, rule.cooldownMs)) {
            return null;
        }
        WaveformDefinition waveform = findWaveform(config, rule.waveformId);
        boolean usableWaveform = waveform != null && waveform.frames != null && !waveform.frames.isEmpty();
        if (usableWaveform) {
            return new ExecutableRule(rule, waveform);
        }
        return rule.strengthAction == StrengthAction.DECREASE ? new ExecutableRule(rule, null) : null;
    }

    private boolean matchesConditions(RuleDefinition rule, RuleEventContext context) {
        if (rule.conditions == null || rule.conditions.isEmpty()) {
            return true;
        }
        if (TriggerRegistry.LOW_HEALTH.equals(rule.trigger)) {
            Double threshold = rule.conditions.get("threshold");
            return threshold == null || context.currentHealth <= threshold.doubleValue();
        }
        if (TriggerRegistry.LOW_FOOD.equals(rule.trigger)) {
            Double threshold = rule.conditions.get("threshold");
            return threshold == null || context.currentFood <= threshold.doubleValue();
        }
        if (TriggerRegistry.ARMOR_LOW.equals(rule.trigger)) {
            Double threshold = rule.conditions.get("ratio");
            return threshold == null || context.lowestArmorRatio <= threshold.doubleValue();
        }
        return true;
    }

    private WaveformDefinition findWaveform(AppConfig config, String waveformId) {
        if (config == null || waveformId == null) {
            return null;
        }
        for (WaveformDefinition waveform : config.waveforms) {
            if (waveformId.equals(waveform.id)) {
                return waveform;
            }
        }
        return null;
    }

    private RuleEventContext createPreviewContext(String triggerId) {
        RuleEventContext context = new RuleEventContext();
        context.triggerId = triggerId;
        context.currentHealth = 10.0F;
        context.maxHealth = 20.0F;
        context.currentFood = 12;
        context.maxFood = 20;
        context.lowestArmorRatio = 0.5D;
        context.damage = 2.0F;
        context.healAmount = 2.0F;
        if (TriggerRegistry.LOW_HEALTH.equals(triggerId)) {
            context.currentHealth = 4.0F;
        } else if (TriggerRegistry.LOW_FOOD.equals(triggerId)) {
            context.currentFood = 5;
        } else if (TriggerRegistry.ARMOR_LOW.equals(triggerId)) {
            context.lowestArmorRatio = 0.1D;
        }
        return context;
    }

    private long resolveWaveformDuration(WaveformDefinition waveform) {
        if (waveform == null) {
            return 300L;
        }
        if (waveform.estimatedDurationMs > 0L) {
            return Math.max(150L, waveform.estimatedDurationMs);
        }
        return Math.max(150L, waveform.frames == null ? 300L : waveform.frames.size() * 100L);
    }

    private int resolveEffectiveMax(DeviceChannel channel, int configuredMax) {
        return Math.min(clamp(configuredMax, 0, 200), resolveDeviceMax(channel));
    }

    private int resolveDeviceMax(DeviceChannel channel) {
        DeviceChannelState snapshot = sessionManager.snapshot(channel);
        int reportedMax = snapshot == null ? 0 : snapshot.maxStrength;
        return reportedMax > 0 ? clamp(reportedMax, 0, 200) : 200;
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private String labelForDebug(RuleDefinition rule) {
        if (rule == null) {
            return "未命名规则";
        }
        if (rule.name != null && !rule.name.trim().isEmpty()) {
            return rule.name;
        }
        return safeString(rule.id).isEmpty() ? "未命名规则" : rule.id;
    }

    private boolean isDamageScaledTrigger(String triggerId) {
        return TriggerRegistry.PLAYER_HURT.equals(triggerId)
            || TriggerRegistry.FALL_DAMAGE.equals(triggerId)
            || TriggerRegistry.SHIELD_BLOCK.equals(triggerId)
            || TriggerRegistry.ATTACK_DAMAGE_ALL.equals(triggerId)
            || TriggerRegistry.ATTACK_DAMAGE_PLAYER.equals(triggerId)
            || TriggerRegistry.ATTACK_DAMAGE_NON_PLAYER.equals(triggerId);
    }

    private boolean isDeathTrigger(String triggerId) {
        return TriggerRegistry.PLAYER_DEATH.equals(triggerId) || TriggerRegistry.PLAYER_KILLED_BY_OTHER.equals(triggerId);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class ChannelRuntime {
        final DeviceChannel channel;
        int currentStrength;
        int dynamicMinStrength;
        long delayRemainingMs;
        long decayAccumulatorMs;
        long waveformAccumulatorMs;
        long waveformDurationMs = 300L;
        int lastAppliedStrength = -1;
        boolean clearOnNextPulse = true;
        boolean cleared = true;
        boolean outputActive;
        String waveformId = "";
        String waveformName = "";
        String lastTrigger = "";
        String lastDispatchedWaveformId = "";
        List<String> waveformFrames = Collections.emptyList();

        private ChannelRuntime(DeviceChannel channel) {
            this.channel = channel;
        }
    }

    private static final class ExecutableRule {
        final RuleDefinition rule;
        final WaveformDefinition waveform;

        private ExecutableRule(RuleDefinition rule, WaveformDefinition waveform) {
            this.rule = rule;
            this.waveform = waveform;
        }
    }

    public static class RuntimeSnapshot {
        public RuntimeChannelSnapshot channelA = new RuntimeChannelSnapshot();
        public RuntimeChannelSnapshot channelB = new RuntimeChannelSnapshot();
    }

    public static class RuntimeChannelSnapshot {
        public int currentStrength;
        public int configuredMaxStrength;
        public int effectiveMaxStrength;
        public int deviceMaxStrength;
        public int eventStrength;
        public double damageScale;
        public int delayMs;
        public int decayIntervalMs;
        public int decayValue;
        public int deathStrength;
        public int deathDelayMs;
        public int minStrength;
        public int dynamicMinStrength;
        public long delayRemainingMs;
        public boolean outputActive;
        public String waveformName = "";
        public String triggerId = "";
    }
}
