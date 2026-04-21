package dglabmc.client.ui;

import dglabmc.AppServices;
import dglabmc.config.AppConfig;
import dglabmc.rule.ChannelTarget;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChannelProfileScreen extends BaseScreen {
    private final ChannelTarget channel;

    public ChannelProfileScreen(Screen parent, ChannelTarget channel) {
        super(new StringTextComponent(channel == ChannelTarget.B ? "B 通道设置" : "A 通道设置"), parent);
        this.channel = channel == ChannelTarget.B ? ChannelTarget.B : ChannelTarget.A;
    }

    @Override protected int maxPanelWidth() { return 560; }
    @Override protected int compactThreshold() { return UiConstants.COMPACT_THRESHOLD_XL; }
    @Override protected int panelHeightNormal() { return 278; }
    @Override protected int panelHeightCompact() { return 340; }

    @Override
    protected void buildWidgets() {
        int il = innerLeft();
        int innerTop = this.panelTop + 52;
        int columnWidth = compact ? innerWidth() : (this.panelWidth - 52) / 2;
        int rightLeft = il + columnWidth + 16;

        if (compact) {
            int rowY = innerTop;
            addButton(actionButton(il, rowY, columnWidth, labelDamageScale(), button -> openDoublePrompt("每伤害强度", "支持小数。", currentProfile().damageScale, value -> updateProfile(profile -> profile.damageScale = UiUtil.clamp(value, 0.0D, 20.0D)))));
            rowY += UiConstants.ROW_HEIGHT;
            addButton(actionButton(il, rowY, columnWidth, labelEventStrength(), button -> openIntPrompt("普通事件强度", "输入 0 到 200。", currentProfile().eventStrength, value -> updateProfile(profile -> profile.eventStrength = UiUtil.clamp(value, 0, 200)))));
            rowY += UiConstants.ROW_HEIGHT;
            addButton(actionButton(il, rowY, columnWidth, labelDelay(), button -> openIntPrompt("下降等待", "单位毫秒。", currentProfile().delayMs, value -> updateProfile(profile -> profile.delayMs = UiUtil.clamp(value, 0, 600000)))));
            rowY += UiConstants.ROW_HEIGHT;
            addButton(actionButton(il, rowY, columnWidth, labelDecayInterval(), button -> openIntPrompt("下降间隔", "单位毫秒，至少 50。", currentProfile().decayIntervalMs, value -> updateProfile(profile -> profile.decayIntervalMs = UiUtil.clamp(value, 50, 600000)))));
            rowY += UiConstants.ROW_HEIGHT;
            addButton(actionButton(il, rowY, columnWidth, labelDecayValue(), button -> openIntPrompt("下降数值", "输入 0 到 200。", currentProfile().decayValue, value -> updateProfile(profile -> profile.decayValue = UiUtil.clamp(value, 0, 200)))));
            rowY += UiConstants.ROW_HEIGHT;
            addButton(actionButton(il, rowY, columnWidth, labelDeathStrength(), button -> openIntPrompt("死亡增加", "输入 0 到 200。", currentProfile().deathStrength, value -> updateProfile(profile -> profile.deathStrength = UiUtil.clamp(value, 0, 200)))));
            rowY += UiConstants.ROW_HEIGHT;
            addButton(actionButton(il, rowY, columnWidth, labelDeathDelay(), button -> openIntPrompt("死亡等待", "单位毫秒。", currentProfile().deathDelayMs, value -> updateProfile(profile -> profile.deathDelayMs = UiUtil.clamp(value, 0, 600000)))));
            rowY += UiConstants.ROW_HEIGHT;
            addButton(actionButton(il, rowY, columnWidth, labelMinStrength(), button -> openIntPrompt("最低强度", "按缺血比例生效，输入 0 到 200。", currentProfile().minStrength, value -> updateProfile(profile -> profile.minStrength = UiUtil.clamp(value, 0, 200)))));
            rowY += UiConstants.ROW_HEIGHT;
            addButton(actionButton(il, rowY, columnWidth, labelMaxStrength(), button -> openIntPrompt("全局上限", maxHint(), configuredMax(), value -> updateConfig(config -> setConfiguredMax(config, UiUtil.clamp(value, 0, 200))))));
            rowY += UiConstants.ROW_HEIGHT;
            addButton(new StyledButton(il, rowY, columnWidth, UiConstants.BTN_HEIGHT, new StringTextComponent("说明"), StyledButton.Variant.SECONDARY, button -> openGuide()));
        } else {
            addButton(actionButton(il, innerTop, columnWidth, labelDamageScale(), button -> openDoublePrompt("每伤害强度", "支持小数。", currentProfile().damageScale, value -> updateProfile(profile -> profile.damageScale = UiUtil.clamp(value, 0.0D, 20.0D)))));
            addButton(actionButton(rightLeft, innerTop, columnWidth, labelEventStrength(), button -> openIntPrompt("普通事件强度", "输入 0 到 200。", currentProfile().eventStrength, value -> updateProfile(profile -> profile.eventStrength = UiUtil.clamp(value, 0, 200)))));
            addButton(actionButton(il, innerTop + 28, columnWidth, labelDelay(), button -> openIntPrompt("下降等待", "单位毫秒。", currentProfile().delayMs, value -> updateProfile(profile -> profile.delayMs = UiUtil.clamp(value, 0, 600000)))));
            addButton(actionButton(rightLeft, innerTop + 28, columnWidth, labelDecayInterval(), button -> openIntPrompt("下降间隔", "单位毫秒，至少 50。", currentProfile().decayIntervalMs, value -> updateProfile(profile -> profile.decayIntervalMs = UiUtil.clamp(value, 50, 600000)))));
            addButton(actionButton(il, innerTop + 56, columnWidth, labelDecayValue(), button -> openIntPrompt("下降数值", "输入 0 到 200。", currentProfile().decayValue, value -> updateProfile(profile -> profile.decayValue = UiUtil.clamp(value, 0, 200)))));
            addButton(actionButton(rightLeft, innerTop + 56, columnWidth, labelDeathStrength(), button -> openIntPrompt("死亡增加", "输入 0 到 200。", currentProfile().deathStrength, value -> updateProfile(profile -> profile.deathStrength = UiUtil.clamp(value, 0, 200)))));
            addButton(actionButton(il, innerTop + 84, columnWidth, labelDeathDelay(), button -> openIntPrompt("死亡等待", "单位毫秒。", currentProfile().deathDelayMs, value -> updateProfile(profile -> profile.deathDelayMs = UiUtil.clamp(value, 0, 600000)))));
            addButton(actionButton(rightLeft, innerTop + 84, columnWidth, labelMinStrength(), button -> openIntPrompt("最低强度", "按缺血比例生效，输入 0 到 200。", currentProfile().minStrength, value -> updateProfile(profile -> profile.minStrength = UiUtil.clamp(value, 0, 200)))));
            addButton(actionButton(il, innerTop + 112, columnWidth, labelMaxStrength(), button -> openIntPrompt("全局上限", maxHint(), configuredMax(), value -> updateConfig(config -> setConfiguredMax(config, UiUtil.clamp(value, 0, 200))))));
            addButton(new StyledButton(rightLeft, innerTop + 112, columnWidth, UiConstants.BTN_HEIGHT, new StringTextComponent("说明"), StyledButton.Variant.SECONDARY, button -> openGuide()));
        }

        addButton(new StyledButton(this.panelLeft + this.panelWidth - 128, this.panelTop + this.panelHeight - 34, 110, UiConstants.BTN_HEIGHT, new StringTextComponent("返回"), StyledButton.Variant.SECONDARY, button -> this.minecraft.displayGuiScreen(this.parent)));
    }

    @Override
    protected void renderContent(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        int il = innerLeft();
        UiRender.drawSectionTitle(matrixStack, this.font, title(), "强度逻辑", il, this.panelTop + 16);

        AppServices.get().getRuleRuntimeSnapshot();
        AppConfig.ChannelStrengthProfile profile = currentProfile();
        int infoX = il;
        int infoY = compact ? this.panelTop + this.panelHeight - 86 : this.panelTop + 182;
        UiRender.drawPanel(matrixStack, infoX, infoY, this.panelWidth - 36, 48, UiPalette.PANEL_INFO_DIM, UiPalette.BORDER_STRONG);
        this.font.drawString("当前: " + currentStrength() + " | 上限: " + effectiveMaxText(), (float) (infoX + 10), (float) (infoY + 10), UiPalette.TEXT_PRIMARY);
        this.font.drawString("普通 " + profile.eventStrength + "  伤害 " + UiUtil.formatDouble(profile.damageScale) + "  死亡 +" + profile.deathStrength, (float) (infoX + 10), (float) (infoY + 24), UiPalette.TEXT_MUTED);
        this.font.drawString("等待 " + profile.delayMs + "  下降 " + profile.decayIntervalMs + "/" + profile.decayValue + "  最低 " + profile.minStrength, (float) (infoX + 10), (float) (infoY + 36), UiPalette.TEXT_MUTED);
    }

    private StyledButton actionButton(int x, int y, int width, String label, StyledButton.IPressable onPress) {
        return new StyledButton(x, y, width, UiConstants.BTN_HEIGHT, new StringTextComponent(label), StyledButton.Variant.GHOST, onPress);
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
        this.minecraft.displayGuiScreen(new InfoScreen(this, title(), "字段说明", lines));
    }

    private void openIntPrompt(String heading, String description, int initialValue, Consumer<Integer> consumer) {
        this.minecraft.displayGuiScreen(new TextPromptScreen(
            this,
            heading,
            description,
            "数值",
            "确定",
            Integer.toString(initialValue),
            value -> {
                try {
                    consumer.accept(Integer.valueOf(Integer.parseInt(value.trim())));
                    this.minecraft.displayGuiScreen(this);
                    setStatus("已更新。");
                    init();
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("请输入有效数字。");
                }
            }
        ));
    }

    private void openDoublePrompt(String heading, String description, double initialValue, Consumer<Double> consumer) {
        this.minecraft.displayGuiScreen(new TextPromptScreen(
            this,
            heading,
            description,
            "数值",
            "确定",
            UiUtil.formatDouble(initialValue),
            value -> {
                try {
                    consumer.accept(Double.valueOf(Double.parseDouble(value.trim())));
                    this.minecraft.displayGuiScreen(this);
                    setStatus("已更新。");
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
        return value > 0 ? UiUtil.clamp(value, 0, 200) : 200;
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

    private String labelDamageScale() { return "每伤害强度: " + UiUtil.formatDouble(currentProfile().damageScale); }
    private String labelEventStrength() { return "普通事件强度: " + currentProfile().eventStrength; }
    private String labelDelay() { return "下降等待: " + currentProfile().delayMs; }
    private String labelDecayInterval() { return "下降间隔: " + currentProfile().decayIntervalMs; }
    private String labelDecayValue() { return "下降数值: " + currentProfile().decayValue; }
    private String labelDeathStrength() { return "死亡增加: " + currentProfile().deathStrength; }
    private String labelDeathDelay() { return "死亡等待: " + currentProfile().deathDelayMs; }
    private String labelMinStrength() { return "最低强度: " + currentProfile().minStrength; }
    private String labelMaxStrength() { return "全局上限: " + configuredMax(); }
}
