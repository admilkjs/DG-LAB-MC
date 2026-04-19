package dglabmc.client;

import dglabmc.AppServices;
import dglabmc.config.AppConfig;
import dglabmc.device.DeviceChannel;
import dglabmc.device.DeviceSessionManager;
import dglabmc.platform.PlatformServices;
import dglabmc.rule.ChannelTarget;
import dglabmc.rule.RuleDefinition;
import dglabmc.rule.TriggerDefinition;
import dglabmc.rule.RuleEngine;
import dglabmc.security.DailyPasswordLock;
import dglabmc.wave.WaveformDefinition;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ClientCommandRouter {
    private static boolean debugMode;
    private static boolean pendingControlCenterOpen;

    private ClientCommandRouter() {
    }

    public static boolean tryHandle(String message) {
        if (message == null) {
            return false;
        }
        String trimmed = message.trim();
        DailyPasswordLock.clearExpiredLock();
        if ("dgDebug".equals(trimmed)) {
            if (!DailyPasswordLock.isUnlocked()) {
                feedback("请先输入 /dglab password <今日密码>");
                return true;
            }
            debugMode = !debugMode;
            feedback(debugMode ? "[DG-Debug] 已开启" : "[DG-Debug] 已关闭");
            return true;
        }
        if (!trimmed.startsWith("/dglab")) {
            return false;
        }

        try {
            String body = trimmed.length() == 6 ? "" : trimmed.substring(6).trim();
            if (body.isEmpty()) {
                if (!DailyPasswordLock.isUnlocked()) {
                    feedback("请先输入 /dglab password <今日密码>");
                    return true;
                }
                showHelp();
                return true;
            }

            String[] parts = tokenize(body);
            if (parts.length == 0) {
                if (!DailyPasswordLock.isUnlocked()) {
                    feedback("请先输入 /dglab password <今日密码>");
                    return true;
                }
                showHelp();
                return true;
            }
            String root = lower(parts[0]);
            if ("password".equals(root)) {
                handlePassword(parts);
                return true;
            }
            if (!DailyPasswordLock.isUnlocked()) {
                if ("ui".equals(root) || "open".equals(root)) {
                    pendingControlCenterOpen = true;
                    return true;
                }
                feedback("请先输入 /dglab password <今日密码>");
                return true;
            }
            if ("ui".equals(root) || "open".equals(root)) {
                pendingControlCenterOpen = true;
                return true;
            }
            if ("status".equals(root)) {
                showStatus();
                return true;
            }
            if ("pair".equals(root) || "link".equals(root)) {
                handlePair(parts);
                return true;
            }
            if ("export".equals(root)) {
                exportConfig();
                return true;
            }
            if ("global".equals(root)) {
                handleGlobal(parts);
                return true;
            }
            if ("rule".equals(root)) {
                handleRule(parts);
                return true;
            }
            if ("waveform".equals(root) || "wave".equals(root)) {
                handleWaveform(parts);
                return true;
            }

            showHelp();
            return true;
        } catch (RuntimeException exception) {
            feedback(exception.getMessage() == null ? "命令执行失败" : exception.getMessage());
            return true;
        }
    }

    private static void handlePassword(String[] parts) {
        if (parts.length < 2) {
            feedback("用法: /dglab password <今日密码>");
            return;
        }
        if (DailyPasswordLock.unlock(parts[1])) {
            feedback("今日密码已通过");
            return;
        }
        feedback("密码不对");
    }

    public static void reportTrigger(String triggerId) {
        if (!debugMode) {
            return;
        }
        feedback("[DG-Debug] 触发事件: " + triggerLabel(triggerId));
    }

    public static void reportRuleRowContinue(int rowIndex) {
        if (!debugMode) {
            return;
        }
        feedback("[DG-Debug] 第" + rowIndex + "行未命中，继续向下");
    }

    public static void reportRuleRowMatched(int rowIndex, java.util.List<String> ruleNames) {
        if (!debugMode) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (String ruleName : ruleNames) {
            if (ruleName == null || ruleName.trim().isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("，");
            }
            builder.append(ruleName.trim());
        }
        feedback("[DG-Debug] 第" + rowIndex + "行命中: " + (builder.length() == 0 ? "未命名规则" : builder.toString()));
    }

    public static void reportRuleStop(int rowIndex) {
        if (!debugMode) {
            return;
        }
        feedback("[DG-Debug] 第" + rowIndex + "行已执行，停止向下");
    }

    public static void reportRuleNoMatch() {
        if (!debugMode) {
            return;
        }
        feedback("[DG-Debug] 本次事件没有规则命中");
    }

    public static boolean consumePendingControlCenterOpen() {
        if (!pendingControlCenterOpen) {
            return false;
        }
        pendingControlCenterOpen = false;
        return true;
    }

    private static void handlePair(String[] parts) {
        if (parts.length > 1 && "refresh".equals(lower(parts[1]))) {
            String link = AppServices.get().refreshPairingLink();
            PlatformServices.client().copyToClipboard(link);
            feedback("配对链接已刷新并复制");
            return;
        }
        String link = AppServices.get().getPairingLink();
        PlatformServices.client().copyToClipboard(link);
        feedback("配对链接已复制");
    }

    private static void handleGlobal(String[] parts) {
        if (parts.length < 2) {
            showGlobalStatus();
            return;
        }

        if ("strength".equals(lower(parts[1]))) {
            handleLegacyGlobalStrength(parts);
            return;
        }

        ChannelTarget channel = parseSingleChannel(parts[1]);
        if (channel == null) {
            feedback("用法: /dglab global <a|b> [max|event|damage|delay|decayInterval|decayValue|death|deathDelay|min] [值]");
            return;
        }

        if (parts.length == 2) {
            showChannelStatus(channel);
            return;
        }

        String key = lower(parts[2]);
        AppConfig config = AppServices.get().getConfig();
        AppConfig.ChannelStrengthProfile profile = profile(config, channel);
        if (parts.length == 3) {
            showChannelStatus(channel);
            return;
        }

        if ("max".equals(key)) {
            setChannelMax(config, channel, clamp(parseInt(parts[3], "上限"), 0, 200));
        } else if ("event".equals(key)) {
            profile.eventStrength = clamp(parseInt(parts[3], "普通事件强度"), 0, 200);
        } else if ("damage".equals(key)) {
            profile.damageScale = clamp(parseDouble(parts[3], "每伤害强度"), 0.0D, 20.0D);
        } else if ("delay".equals(key)) {
            profile.delayMs = clamp(parseInt(parts[3], "下降等待"), 0, 600000);
        } else if ("decayinterval".equals(key)) {
            profile.decayIntervalMs = clamp(parseInt(parts[3], "下降间隔"), 50, 600000);
        } else if ("decayvalue".equals(key)) {
            profile.decayValue = clamp(parseInt(parts[3], "下降数值"), 0, 200);
        } else if ("death".equals(key)) {
            profile.deathStrength = clamp(parseInt(parts[3], "死亡增加"), 0, 200);
        } else if ("deathdelay".equals(key)) {
            profile.deathDelayMs = clamp(parseInt(parts[3], "死亡等待"), 0, 600000);
        } else if ("min".equals(key)) {
            profile.minStrength = clamp(parseInt(parts[3], "最低强度"), 0, 200);
        } else {
            feedback("未知字段: " + parts[2]);
            return;
        }

        AppServices.get().saveConfig(config);
        showChannelStatus(channel);
    }

    private static void handleLegacyGlobalStrength(String[] parts) {
        if (parts.length < 3) {
            feedback("旧用法已兼容为 A/B 普通事件强度和上限");
            showGlobalStatus();
            return;
        }
        int base = clamp(parseInt(parts[2], "普通事件强度"), 0, 200);
        int max = parts.length >= 4 ? clamp(parseInt(parts[3], "上限"), 0, 200) : base;
        AppConfig config = AppServices.get().getConfig();
        config.strength.channelA.eventStrength = base;
        config.strength.channelB.eventStrength = base;
        config.strength.maxStrengthA = max;
        config.strength.maxStrengthB = max;
        AppServices.get().saveConfig(config);
        feedback("已同步到 A/B 通道");
        showGlobalStatus();
    }

    private static void handleRule(String[] parts) {
        if (parts.length < 2) {
            feedback("用法: /dglab rule list|enable|disable|test|rename|move|row");
            return;
        }
        String sub = lower(parts[1]);
        if ("list".equals(sub)) {
            listRules();
            return;
        }
        if ("move".equals(sub)) {
            handleRuleMove(parts);
            return;
        }
        if ("row".equals(sub)) {
            handleRuleRow(parts);
            return;
        }
        if (parts.length < 3) {
            feedback("缺少规则名或规则 ID");
            return;
        }
        RuleDefinition rule = findRule(parts[2]);
        if (rule == null) {
            feedback("未找到规则: " + parts[2]);
            return;
        }
        if ("enable".equals(sub)) {
            rule.enabled = true;
            saveRule(rule);
            feedback("已启用规则: " + rule.name);
            return;
        }
        if ("disable".equals(sub)) {
            rule.enabled = false;
            saveRule(rule);
            feedback("已禁用规则: " + rule.name);
            return;
        }
        if ("test".equals(sub)) {
            AppServices.get().testRule(rule);
            feedback("已测试规则: " + rule.name);
            return;
        }
        if ("rename".equals(sub)) {
            String nextName = joinTail(parts, 3);
            if (nextName.isEmpty()) {
                feedback("用法: /dglab rule rename <规则> <新名称>");
                return;
            }
            rule.name = nextName;
            saveRule(rule);
            feedback("规则已重命名为: " + rule.name);
            return;
        }
        feedback("用法: /dglab rule list|enable|disable|test|rename|move|row");
    }

    private static void handleRuleMove(String[] parts) {
        if (parts.length < 4) {
            feedback("用法: /dglab rule move <规则> <up|down|top|bottom>");
            return;
        }
        RuleDefinition rule = findRule(parts[2]);
        if (rule == null) {
            feedback("未找到规则: " + parts[2]);
            return;
        }
        String action = lower(parts[3]);
        AppConfig config = AppServices.get().getConfig();
        int index = findRuleIndex(config, rule.id);
        if (index < 0) {
            feedback("未找到规则: " + parts[2]);
            return;
        }
        RuleDefinition moving = config.rules.remove(index);
        int targetIndex;
        if ("up".equals(action)) {
            targetIndex = Math.max(0, index - 1);
        } else if ("down".equals(action)) {
            targetIndex = Math.min(config.rules.size(), index + 1);
        } else if ("top".equals(action)) {
            targetIndex = 0;
        } else if ("bottom".equals(action)) {
            targetIndex = config.rules.size();
        } else {
            config.rules.add(index, moving);
            feedback("用法: /dglab rule move <规则> <up|down|top|bottom>");
            return;
        }
        config.rules.add(targetIndex, moving);
        normalizeRuleGroups(config.rules);
        AppServices.get().saveConfig(config);
        feedback("已调整规则顺序: " + displayRuleName(moving));
    }

    private static void handleRuleRow(String[] parts) {
        if (parts.length < 4) {
            feedback("用法: /dglab rule row <规则> <mergeUp|splitNext>");
            return;
        }
        RuleDefinition rule = findRule(parts[2]);
        if (rule == null) {
            feedback("未找到规则: " + parts[2]);
            return;
        }
        String action = lower(parts[3]);
        AppConfig config = AppServices.get().getConfig();
        int index = findRuleIndex(config, rule.id);
        if (index < 0) {
            feedback("未找到规则: " + parts[2]);
            return;
        }
        List<List<RuleDefinition>> currentRows = buildRuleRows(config.rules, null);
        int[] position = findRuleRowPosition(currentRows, rule.id);
        if (position == null) {
            feedback("未找到规则: " + parts[2]);
            return;
        }
        if ("mergeup".equals(action)) {
            if (position[0] <= 0) {
                feedback("已经在第一行");
                return;
            }
            List<List<RuleDefinition>> reorderedRows = buildRuleRows(config.rules, rule.id);
            reorderedRows.get(position[0] - 1).add(rule);
            saveRuleRows(config, reorderedRows);
            feedback("已并到上一行: " + displayRuleName(rule));
            return;
        }
        if ("splitnext".equals(action)) {
            if (currentRows.get(position[0]).size() <= 1) {
                feedback("当前已是单独一行");
                return;
            }
            List<List<RuleDefinition>> reorderedRows = buildRuleRows(config.rules, rule.id);
            List<RuleDefinition> newRow = new ArrayList<RuleDefinition>();
            newRow.add(rule);
            reorderedRows.add(Math.min(position[0] + 1, reorderedRows.size()), newRow);
            saveRuleRows(config, reorderedRows);
            feedback("已拆成单独一行: " + displayRuleName(rule));
            return;
        }
        feedback("用法: /dglab rule row <规则> <mergeUp|splitNext>");
    }

    private static void handleWaveform(String[] parts) {
        if (parts.length < 2) {
            feedback("用法: /dglab waveform list|test");
            return;
        }
        String sub = lower(parts[1]);
        if ("list".equals(sub)) {
            listWaveforms();
            return;
        }
        if ("test".equals(sub)) {
            if (parts.length < 4) {
                feedback("用法: /dglab waveform test <波形> <A|B>");
                return;
            }
            WaveformDefinition waveform = findWaveform(parts[2]);
            if (waveform == null) {
                feedback("未找到波形: " + parts[2]);
                return;
            }
            DeviceChannel channel = "b".equals(lower(parts[3])) ? DeviceChannel.B : DeviceChannel.A;
            AppServices.get().testWaveform(waveform.id, channel);
            feedback("已测试波形: " + waveform.name + " -> " + channel.name());
            return;
        }
        feedback("用法: /dglab waveform list|test");
    }

    private static void exportConfig() {
        Path exportPath = AppServices.get().exportConfigArchive();
        PlatformServices.client().copyToClipboard(exportPath.toString());
        PlatformServices.client().openInFileManager(exportPath);
        feedback("配置已导出并打开文件夹");
    }

    private static void showStatus() {
        DeviceSessionManager.DeviceSnapshot snapshot = AppServices.get().getDeviceSnapshot();
        feedback("连接状态: " + (snapshot.bound ? "已绑定" : snapshot.connected ? "待绑定" : "未连接"));
        feedback("端口: " + AppServices.get().getDevicePort());
        RuleEngine.RuntimeSnapshot runtime = AppServices.get().getRuleRuntimeSnapshot();
        feedback("A: " + runtime.channelA.currentStrength + " / " + runtime.channelA.effectiveMaxStrength + " / " + (runtime.channelA.outputActive ? "输出中" : "未输出"));
        feedback("B: " + runtime.channelB.currentStrength + " / " + runtime.channelB.effectiveMaxStrength + " / " + (runtime.channelB.outputActive ? "输出中" : "未输出"));
    }

    private static void showGlobalStatus() {
        showChannelStatus(ChannelTarget.A);
        showChannelStatus(ChannelTarget.B);
    }

    private static void showChannelStatus(ChannelTarget channel) {
        AppConfig config = AppServices.get().getConfig();
        AppConfig.ChannelStrengthProfile profile = profile(config, channel);
        int configuredMax = channel == ChannelTarget.B ? config.strength.maxStrengthB : config.strength.maxStrengthA;
        int deviceMax = channel == ChannelTarget.B ? AppServices.get().getDeviceSnapshot().maxStrengthB : AppServices.get().getDeviceSnapshot().maxStrengthA;
        int effectiveMax = Math.min(configuredMax, deviceMax > 0 ? deviceMax : 200);
        feedback(channelLabel(channel) + " 上限: " + configuredMax + " / 本次 " + effectiveMax);
        feedback(channelLabel(channel) + " 普通 " + profile.eventStrength + " 伤害 " + trimDouble(profile.damageScale) + " 等待 " + profile.delayMs + " 下降 " + profile.decayIntervalMs + "/" + profile.decayValue + " 死亡 +" + profile.deathStrength + " 最低 " + profile.minStrength);
    }

    private static void listRules() {
        List<RuleDefinition> rules = AppServices.get().getConfig().rules;
        if (rules.isEmpty()) {
            feedback("当前没有规则");
            return;
        }
        for (RuleDefinition rule : rules) {
            feedback("规则 | 第" + (rule.orderGroup + 1) + "行 | " + displayRuleName(rule) + " | " + rule.id + " | " + (rule.enabled ? "开" : "关"));
        }
    }

    private static void listWaveforms() {
        List<WaveformDefinition> waveforms = AppServices.get().getConfig().waveforms;
        if (waveforms.isEmpty()) {
            feedback("当前没有波形");
            return;
        }
        for (WaveformDefinition waveform : waveforms) {
            feedback("波形 | " + waveform.name + " | " + waveform.id);
        }
    }

    private static void saveRule(RuleDefinition updated) {
        AppConfig config = AppServices.get().getConfig();
        for (int i = 0; i < config.rules.size(); i++) {
            if (config.rules.get(i).id.equals(updated.id)) {
                config.rules.set(i, updated);
                normalizeRuleGroups(config.rules);
                AppServices.get().saveConfig(config);
                return;
            }
        }
    }

    private static int findRuleIndex(AppConfig config, String ruleId) {
        if (config == null || ruleId == null) {
            return -1;
        }
        for (int i = 0; i < config.rules.size(); i++) {
            if (ruleId.equals(config.rules.get(i).id)) {
                return i;
            }
        }
        return -1;
    }

    private static void normalizeRuleGroups(List<RuleDefinition> rules) {
        if (rules == null || rules.isEmpty()) {
            return;
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

    private static List<List<RuleDefinition>> buildRuleRows(List<RuleDefinition> rules, String excludedRuleId) {
        List<List<RuleDefinition>> rows = new ArrayList<List<RuleDefinition>>();
        List<RuleDefinition> currentRow = null;
        int previousGroup = Integer.MIN_VALUE;
        for (RuleDefinition rule : rules) {
            if (excludedRuleId != null && excludedRuleId.equals(rule.id)) {
                continue;
            }
            int currentGroup = Math.max(0, rule.orderGroup);
            if (currentRow == null || currentGroup != previousGroup) {
                currentRow = new ArrayList<RuleDefinition>();
                rows.add(currentRow);
                previousGroup = currentGroup;
            }
            currentRow.add(rule);
        }
        return rows;
    }

    private static int[] findRuleRowPosition(List<List<RuleDefinition>> rows, String ruleId) {
        if (rows == null || ruleId == null) {
            return null;
        }
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<RuleDefinition> row = rows.get(rowIndex);
            for (int slotIndex = 0; slotIndex < row.size(); slotIndex++) {
                if (ruleId.equals(row.get(slotIndex).id)) {
                    return new int[] { rowIndex, slotIndex };
                }
            }
        }
        return null;
    }

    private static void saveRuleRows(AppConfig config, List<List<RuleDefinition>> rows) {
        config.rules.clear();
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            List<RuleDefinition> row = rows.get(rowIndex);
            for (RuleDefinition rule : row) {
                rule.orderGroup = rowIndex;
                config.rules.add(rule);
            }
        }
        AppServices.get().saveConfig(config);
    }

    private static String displayRuleName(RuleDefinition rule) {
        if (rule == null) {
            return "未命名规则";
        }
        if (rule.name != null && !rule.name.trim().isEmpty()) {
            return rule.name;
        }
        return rule.id == null || rule.id.trim().isEmpty() ? "未命名规则" : rule.id;
    }

    private static RuleDefinition findRule(String token) {
        RuleDefinition exact = null;
        RuleDefinition fuzzy = null;
        for (RuleDefinition rule : AppServices.get().getConfig().rules) {
            if (rule.id.equals(token)) {
                return rule;
            }
            if (token.equalsIgnoreCase(rule.name)) {
                exact = rule;
            } else if (rule.name != null && rule.name.toLowerCase(Locale.ROOT).startsWith(lower(token)) && fuzzy == null) {
                fuzzy = rule;
            }
        }
        return exact != null ? exact : fuzzy;
    }

    private static WaveformDefinition findWaveform(String token) {
        WaveformDefinition exact = null;
        WaveformDefinition fuzzy = null;
        for (WaveformDefinition waveform : AppServices.get().getConfig().waveforms) {
            if (waveform.id.equals(token)) {
                return waveform;
            }
            if (token.equalsIgnoreCase(waveform.name)) {
                exact = waveform;
            } else if (waveform.name != null && waveform.name.toLowerCase(Locale.ROOT).startsWith(lower(token)) && fuzzy == null) {
                fuzzy = waveform;
            }
        }
        return exact != null ? exact : fuzzy;
    }

    private static String triggerLabel(String triggerId) {
        for (TriggerDefinition definition : AppServices.get().getTriggers()) {
            if (definition.id.equals(triggerId)) {
                return definition.label;
            }
        }
        return triggerId;
    }

    private static void showHelp() {
        feedback("/dglab password <今日密码>");
        feedback("/dglab ui  打开界面");
        feedback("/dglab status  查看状态");
        feedback("/dglab pair [refresh]  复制或刷新配对链接");
        feedback("/dglab export  导出配置并打开文件夹");
        feedback("/dglab global <a|b> <项> [值]");
        feedback("/dglab rule list|enable|disable|test|rename|move|row");
        feedback("/dglab waveform list|test");
    }

    private static void feedback(String message) {
        PlatformServices.client().showPlayerMessage(message);
    }

    private static String joinTail(String[] parts, int startIndex) {
        if (startIndex >= parts.length) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < parts.length; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(parts[i]);
        }
        return builder.toString().trim();
    }

    private static String[] tokenize(String input) {
        List<String> tokens = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean escaping = false;
        boolean tokenStarted = false;
        for (int i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);
            if (quoted) {
                if (escaping) {
                    current.append(currentChar);
                    escaping = false;
                } else if (currentChar == '\\') {
                    escaping = true;
                } else if (currentChar == '"') {
                    quoted = false;
                    tokenStarted = true;
                } else {
                    current.append(currentChar);
                }
                continue;
            }
            if (Character.isWhitespace(currentChar)) {
                if (tokenStarted) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    tokenStarted = false;
                }
                continue;
            }
            if (currentChar == '"') {
                quoted = true;
                tokenStarted = true;
                continue;
            }
            current.append(currentChar);
            tokenStarted = true;
        }
        if (escaping) {
            current.append('\\');
        }
        if (tokenStarted) {
            tokens.add(current.toString());
        }
        return tokens.toArray(new String[0]);
    }

    private static int parseInt(String value, String label) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + "必须是数字");
        }
    }

    private static double parseDouble(String value, String label) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(label + "必须是数字");
        }
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static ChannelTarget parseSingleChannel(String token) {
        String lowered = lower(token);
        if ("a".equals(lowered)) {
            return ChannelTarget.A;
        }
        if ("b".equals(lowered)) {
            return ChannelTarget.B;
        }
        return null;
    }

    private static AppConfig.ChannelStrengthProfile profile(AppConfig config, ChannelTarget channel) {
        return channel == ChannelTarget.B ? config.strength.channelB : config.strength.channelA;
    }

    private static void setChannelMax(AppConfig config, ChannelTarget channel, int value) {
        if (channel == ChannelTarget.B) {
            config.strength.maxStrengthB = value;
        } else {
            config.strength.maxStrengthA = value;
        }
    }

    private static String channelLabel(ChannelTarget channel) {
        return channel == ChannelTarget.B ? "B" : "A";
    }

    private static String trimDouble(double value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        while (text.contains(".") && (text.endsWith("0") || text.endsWith("."))) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }
}
