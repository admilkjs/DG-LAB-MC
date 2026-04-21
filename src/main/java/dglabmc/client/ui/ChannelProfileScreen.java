package dglabmc.client.ui;

import dglabmc.AppServices;
import dglabmc.config.AppConfig;
import dglabmc.rule.ChannelTarget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ChannelProfileScreen extends Screen {
    private final Screen parent;
    private final ChannelTarget channel;
    private String statusMessage = "";

    public ChannelProfileScreen(Screen parent, ChannelTarget channel) {
        super(Component.literal(channel == ChannelTarget.B ? "B 通道设置" : "A 通道设置"));
        this.parent = parent;
        this.channel = channel == ChannelTarget.B ? ChannelTarget.B : ChannelTarget.A;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int panelWidth = Math.min(560, this.width - 24);
        boolean compact = panelWidth < 520;
        int panelHeight = Math.min(compact ? 340 : 278, this.height - 24);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int innerLeft = left + 18;
        int innerTop = top + 52;
        int columnWidth = compact ? panelWidth - 36 : (panelWidth - 52) / 2;
        int rightLeft = innerLeft + columnWidth + 16;

        if (compact) {
            int rowY = innerTop;
            addRenderableWidget(actionButton(innerLeft, rowY, columnWidth, labelDamageScale(), button -> openDoublePrompt("每伤害强度", "支持小数。", currentProfile().damageScale, value -> updateProfile(profile -> profile.damageScale = clamp(value, 0.0D, 20.0D)))));
            rowY += 24;
            addRenderableWidget(actionButton(innerLeft, rowY, columnWidth, labelEventStrength(), button -> openIntPrompt("普通事件强度", "输入 0 到 200。", currentProfile().eventStrength, value -> updateProfile(profile -> profile.eventStrength = clamp(value, 0, 200)))));
            rowY += 24;
            addRenderableWidget(actionButton(innerLeft, rowY, columnWidth, labelDelay(), button -> openIntPrompt("下降等待", "单位毫秒。", currentProfile().delayMs, value -> updateProfile(profile -> profile.delayMs = clamp(value, 0, 600000)))));
            rowY += 24;
            addRenderableWidget(actionButton(innerLeft, rowY, columnWidth, labelDecayInterval(), button -> openIntPrompt("下降间隔", "单位毫秒，至少 50。", currentProfile().decayIntervalMs, value -> updateProfile(profile -> profile.decayIntervalMs = clamp(value, 50, 600000)))));
            rowY += 24;
            addRenderableWidget(actionButton(innerLeft, rowY, columnWidth, labelDecayValue(), button -> openIntPrompt("下降数值", "输入 0 到 200。", currentProfile().decayValue, value -> updateProfile(profile -> profile.decayValue = clamp(value, 0, 200)))));
            rowY += 24;
            addRenderableWidget(actionButton(innerLeft, rowY, columnWidth, labelDeathStrength(), button -> openIntPrompt("死亡增加", "输入 0 到 200。", currentProfile().deathStrength, value -> updateProfile(profile -> profile.deathStrength = clamp(value, 0, 200)))));
            rowY += 24;
            addRenderableWidget(actionButton(innerLeft, rowY, columnWidth, labelDeathDelay(), button -> openIntPrompt("死亡等待", "单位毫秒。", currentProfile().deathDelayMs, value -> updateProfile(profile -> profile.deathDelayMs = clamp(value, 0, 600000)))));
            rowY += 24;
            addRenderableWidget(actionButton(innerLeft, rowY, columnWidth, labelMinStrength(), button -> openIntPrompt("最低强度", "按缺血比例生效，输入 0 到 200。", currentProfile().minStrength, value -> updateProfile(profile -> profile.minStrength = clamp(value, 0, 200)))));
            rowY += 24;
            addRenderableWidget(actionButton(innerLeft, rowY, columnWidth, labelMaxStrength(), button -> openIntPrompt("全局上限", maxHint(), configuredMax(), value -> updateConfig(config -> setConfiguredMax(config, clamp(value, 0, 200))))));
            rowY += 24;
            addRenderableWidget(new StyledButton(innerLeft, rowY, columnWidth, 20, Component.literal("说明"), StyledButton.Variant.SECONDARY, button -> openGuide()));
        } else {
            addRenderableWidget(actionButton(innerLeft, innerTop, columnWidth, labelDamageScale(), button -> openDoublePrompt("每伤害强度", "支持小数。", currentProfile().damageScale, value -> updateProfile(profile -> profile.damageScale = clamp(value, 0.0D, 20.0D)))));
            addRenderableWidget(actionButton(rightLeft, innerTop, columnWidth, labelEventStrength(), button -> openIntPrompt("普通事件强度", "输入 0 到 200。", currentProfile().eventStrength, value -> updateProfile(profile -> profile.eventStrength = clamp(value, 0, 200)))));
            addRenderableWidget(actionButton(innerLeft, innerTop + 28, columnWidth, labelDelay(), button -> openIntPrompt("下降等待", "单位毫秒。", currentProfile().delayMs, value -> updateProfile(profile -> profile.delayMs = clamp(value, 0, 600000)))));
            addRenderableWidget(actionButton(rightLeft, innerTop + 28, columnWidth, labelDecayInterval(), button -> openIntPrompt("下降间隔", "单位毫秒，至少 50。", currentProfile().decayIntervalMs, value -> updateProfile(profile -> profile.decayIntervalMs = clamp(value, 50, 600000)))));
            addRenderableWidget(actionButton(innerLeft, innerTop + 56, columnWidth, labelDecayValue(), button -> openIntPrompt("下降数值", "输入 0 到 200。", currentProfile().decayValue, value -> updateProfile(profile -> profile.decayValue = clamp(value, 0, 200)))));
            addRenderableWidget(actionButton(rightLeft, innerTop + 56, columnWidth, labelDeathStrength(), button -> openIntPrompt("死亡增加", "输入 0 到 200。", currentProfile().deathStrength, value -> updateProfile(profile -> profile.deathStrength = clamp(value, 0, 200)))));
            addRenderableWidget(actionButton(innerLeft, innerTop + 84, columnWidth, labelDeathDelay(), button -> openIntPrompt("死亡等待", "单位毫秒。", currentProfile().deathDelayMs, value -> updateProfile(profile -> profile.deathDelayMs = clamp(value, 0, 600000)))));
            addRenderableWidget(actionButton(rightLeft, innerTop + 84, columnWidth, labelMinStrength(), button -> openIntPrompt("最低强度", "按缺血比例生效，输入 0 到 200。", currentProfile().minStrength, value -> updateProfile(profile -> profile.minStrength = clamp(value, 0, 200)))));
            addRenderableWidget(actionButton(innerLeft, innerTop + 112, columnWidth, labelMaxStrength(), button -> openIntPrompt("全局上限", maxHint(), configuredMax(), value -> updateConfig(config -> setConfiguredMax(config, clamp(value, 0, 200))))));
            addRenderableWidget(new StyledButton(rightLeft, innerTop + 112, columnWidth, 20, Component.literal("说明"), StyledButton.Variant.SECONDARY, button -> openGuide()));
        }

        addRenderableWidget(new StyledButton(left + panelWidth - 128, top + panelHeight - 34, 110, 20, Component.literal("返回"), StyledButton.Variant.SECONDARY, button -> this.minecraft.setScreen(this.parent)));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.fillGradient( 0, 0, this.width, this.height, UiPalette.BACKGROUND_TOP, UiPalette.BACKGROUND_BOTTOM);
        int panelWidth = Math.min(560, this.width - 24);
        boolean compact = panelWidth < 520;
        int panelHeight = Math.min(compact ? 340 : 278, this.height - 24);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        UiRender.drawPanel(guiGraphics, left, top, panelWidth, panelHeight, UiPalette.PANEL, UiPalette.ACCENT);
        UiRender.drawSectionTitle(guiGraphics, this.font, title(), "强度逻辑", left + 18, top + 16);

        AppServices.get().getRuleRuntimeSnapshot();
        AppConfig.ChannelStrengthProfile profile = currentProfile();
        int infoX = left + 18;
        int infoY = compact ? top + panelHeight - 86 : top + 182;
        UiRender.drawPanel(guiGraphics, infoX, infoY, panelWidth - 36, 48, 0x44172233, UiPalette.BORDER_STRONG);
        guiGraphics.drawString(this.font, "当前: " + currentStrength() + " | 上限: " + effectiveMaxText(), (infoX + 10), (infoY + 10), UiPalette.TEXT_PRIMARY);
        guiGraphics.drawString(this.font, "普通 " + profile.eventStrength + "  伤害 " + formatDouble(profile.damageScale) + "  死亡 +" + profile.deathStrength, (infoX + 10), (infoY + 24), UiPalette.TEXT_MUTED);
        guiGraphics.drawString(this.font, "等待 " + profile.delayMs + "  下降 " + profile.decayIntervalMs + "/" + profile.decayValue + "  最低 " + profile.minStrength, (infoX + 10), (infoY + 36), UiPalette.TEXT_MUTED);
        if (!this.statusMessage.isEmpty()) {
            UiRender.drawWrappedText(guiGraphics, this.font, this.statusMessage, left + 18, top + panelHeight - 106, panelWidth - 36, UiPalette.WARNING, 2);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private StyledButton actionButton(int x, int y, int width, String label, StyledButton.IPressable onPress) {
        return new StyledButton(x, y, width, 20, Component.literal(label), StyledButton.Variant.GHOST, onPress);
    }

    private void openGuide() {
        List<String> lines = new ArrayList<String>();
        lines.add("每伤害强度：受伤时按伤害值增加。");
        lines.add("普通事件强度：跳跃、冲刺这类事件使用。");
        lines.add("下降等待：触发后多久开始回落。");
        lines.add("下降间隔 / 数值：回落速度。");
        lines.add("死亡增加 / 死亡等待：死亡单独覆盖。");
        lines.add("最低强度：按缺血比例抬高下限。");
        lines.add("全局上限：本地配置上限，连接后会再受设备上限限制。");
        this.minecraft.setScreen(new InfoScreen(this, title(), "字段说明", lines));
    }

    private void openIntPrompt(String heading, String description, int initialValue, Consumer<Integer> consumer) {
        this.minecraft.setScreen(new TextPromptScreen(
            this,
            heading,
            description,
            "数值",
            "确定",
            Integer.toString(initialValue),
            value -> {
                try {
                    consumer.accept(Integer.valueOf(Integer.parseInt(value.trim())));
                    this.minecraft.setScreen(this);
                    this.statusMessage = "已更新。";
                    init();
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("请输入有效数字。");
                }
            }
        ));
    }

    private void openDoublePrompt(String heading, String description, double initialValue, Consumer<Double> consumer) {
        this.minecraft.setScreen(new TextPromptScreen(
            this,
            heading,
            description,
            "数值",
            "确定",
            formatDouble(initialValue),
            value -> {
                try {
                    consumer.accept(Double.valueOf(Double.parseDouble(value.trim())));
                    this.minecraft.setScreen(this);
                    this.statusMessage = "已更新。";
                    init();
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("请输入有效数字。");
                }
            }
        ));
    }

    private void updateProfile(Consumer<AppConfig.ChannelStrengthProfile> mutator) {
        updateConfig(config -> mutator.accept(profile(config)));
    }

    private void updateConfig(Consumer<AppConfig> mutator) {
        AppConfig config = AppServices.get().getConfig();
        mutator.accept(config);
        AppServices.get().saveConfig(config);
    }

    private AppConfig.ChannelStrengthProfile currentProfile() {
        return profile(AppServices.get().getConfig());
    }

    private AppConfig.ChannelStrengthProfile profile(AppConfig config) {
        return this.channel == ChannelTarget.B ? config.strength.channelB : config.strength.channelA;
    }

    private int configuredMax() {
        AppConfig config = AppServices.get().getConfig();
        return this.channel == ChannelTarget.B ? config.strength.maxStrengthB : config.strength.maxStrengthA;
    }

    private void setConfiguredMax(AppConfig config, int value) {
        if (this.channel == ChannelTarget.B) {
            config.strength.maxStrengthB = value;
        } else {
            config.strength.maxStrengthA = value;
        }
    }

    private String maxHint() {
        int cap = deviceCap();
        return cap >= 200 ? "输入 0 到 200。" : "输入 0 到 200，本次最多 " + cap + "。";
    }

    private int deviceCap() {
        int value = this.channel == ChannelTarget.B ? AppServices.get().getDeviceSnapshot().maxStrengthB : AppServices.get().getDeviceSnapshot().maxStrengthA;
        return value > 0 ? clamp(value, 0, 200) : 200;
    }

    private int currentStrength() {
        return this.channel == ChannelTarget.B
            ? AppServices.get().getRuleRuntimeSnapshot().channelB.currentStrength
            : AppServices.get().getRuleRuntimeSnapshot().channelA.currentStrength;
    }

    private String effectiveMaxText() {
        int configured = configuredMax();
        int device = deviceCap();
        int effective = Math.min(configured, device);
        return configured == effective ? Integer.toString(effective) : configured + " / 本次 " + effective;
    }

    private String title() {
        return this.channel == ChannelTarget.B ? "B 通道设置" : "A 通道设置";
    }

    private String labelDamageScale() {
        return "每伤害强度: " + formatDouble(currentProfile().damageScale);
    }

    private String labelEventStrength() {
        return "普通事件强度: " + currentProfile().eventStrength;
    }

    private String labelDelay() {
        return "下降等待: " + currentProfile().delayMs;
    }

    private String labelDecayInterval() {
        return "下降间隔: " + currentProfile().decayIntervalMs;
    }

    private String labelDecayValue() {
        return "下降数值: " + currentProfile().decayValue;
    }

    private String labelDeathStrength() {
        return "死亡增加: " + currentProfile().deathStrength;
    }

    private String labelDeathDelay() {
        return "死亡等待: " + currentProfile().deathDelayMs;
    }

    private String labelMinStrength() {
        return "最低强度: " + currentProfile().minStrength;
    }

    private String labelMaxStrength() {
        return "全局上限: " + configuredMax();
    }

    private String formatDouble(double value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        while (text.contains(".") && (text.endsWith("0") || text.endsWith("."))) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}


