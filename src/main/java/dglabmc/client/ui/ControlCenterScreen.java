package dglabmc.client.ui;

import dglabmc.AppServices;
import dglabmc.DgLabMcMod;
import dglabmc.config.AppConfig;
import dglabmc.device.DeviceChannel;
import dglabmc.device.DeviceSessionManager;
import dglabmc.platform.PlatformServices;
import dglabmc.rule.ChannelTarget;
import dglabmc.rule.IntensityMode;
import dglabmc.rule.RuleDefinition;
import dglabmc.rule.StrengthAction;
import dglabmc.rule.TriggerDefinition;
import dglabmc.rule.TriggerRegistry;
import dglabmc.security.DailyPasswordLock;
import dglabmc.wave.WaveformDefinition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

public class ControlCenterScreen extends Screen {
    public enum Tab {
        DASHBOARD,
        RULES,
        WAVEFORMS,
        TRANSFER
    }

    private Tab activeTab;
    private String statusMessage;
    private int selectedRuleIndex = -1;
    private int ruleListScroll;
    private int selectedWaveformIndex;
    private RuleDefinition editingRule;
    private boolean creatingRule;
    private String pairingLink;
    private String lastExportPath = "";
    private QrCodeHelper.QrMatrix pairingQrMatrix;
    private String pairingQrSource = "";
    private String pairingQrError = "";

    public ControlCenterScreen() {
        this(resolveInitialTab(), "");
    }

    public ControlCenterScreen(Tab activeTab, String statusMessage) {
        super(Component.literal("DG-LAB 控制中心"));
        this.activeTab = activeTab;
        this.statusMessage = statusMessage == null ? "" : statusMessage;
        this.pairingLink = AppServices.get().getPairingLink();
    }

    @Override
    protected void init() {
        DailyPasswordLock.clearExpiredLock();
        if (!DailyPasswordLock.isUnlocked()) {
            this.minecraft.setScreen(new PasswordGateScreen(new ControlCenterScreen(this.activeTab, this.statusMessage)));
            return;
        }
        rebuildWidgets();
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (this.activeTab != Tab.TRANSFER || paths == null || paths.isEmpty()) {
            return;
        }
        openImportConfirm(paths.get(0));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (this.activeTab == Tab.RULES && isMouseWithinRuleList(mouseX, mouseY)) {
            AppConfig config = AppServices.get().getConfig();
            int visibleSlots = visibleRuleSlots();
            int maxScroll = Math.max(0, config.rules.size() - visibleSlots);
            if (maxScroll > 0) {
                this.ruleListScroll = clamp(this.ruleListScroll + (deltaY < 0.0D ? 1 : -1), 0, maxScroll);
                rebuildWidgets();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private static Tab resolveInitialTab() {
        AppConfig config = AppServices.get().getConfig();
        if (config.ui == null || config.ui.lastOpenedTab == null) {
            return Tab.DASHBOARD;
        }
        try {
            return Tab.valueOf(config.ui.lastOpenedTab.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return Tab.DASHBOARD;
        }
    }

    protected void rebuildWidgets() {
        this.clearWidgets();
        this.pairingLink = AppServices.get().getPairingLink();

        addNavigationButtons();
        if (this.activeTab == Tab.DASHBOARD) {
            buildDashboardWidgets();
        } else if (this.activeTab == Tab.RULES) {
            buildRuleWidgets();
        } else if (this.activeTab == Tab.WAVEFORMS) {
            buildWaveformWidgets();
        } else {
            buildTransferWidgets();
        }
    }

    private void refreshPairingQr() {
        if (this.pairingLink == null || this.pairingLink.trim().isEmpty()) {
            this.pairingQrMatrix = null;
            this.pairingQrSource = "";
            this.pairingQrError = "配对链接为空。";
            return;
        }
        if (this.pairingLink.equals(this.pairingQrSource)) {
            return;
        }
        try {
            this.pairingQrMatrix = QrCodeHelper.encode(this.pairingLink);
            this.pairingQrSource = this.pairingLink;
            this.pairingQrError = "";
        } catch (Throwable throwable) {
            this.pairingQrMatrix = null;
            this.pairingQrSource = this.pairingLink;
            this.pairingQrError = "二维码失败: " + explainThrowable(throwable);
            DgLabMcMod.LOGGER.warn("QR generation failed for pairing link {}", this.pairingLink, throwable);
        }
    }

    private void addNavigationButtons() {
        int left = sidebarLeft() + 12;
        int top = sidebarTop() + 74;
        int width = sidebarWidth() - 24;
        int height = 22;
        int gap = 26;
        addRenderableWidget(navButton(left, top, width, height, Tab.DASHBOARD));
        addRenderableWidget(navButton(left, top + gap, width, height, Tab.RULES));
        addRenderableWidget(navButton(left, top + gap * 2, width, height, Tab.WAVEFORMS));
        addRenderableWidget(navButton(left, top + gap * 3, width, height, Tab.TRANSFER));
        addRenderableWidget(new StyledButton(left, contentBottom() - 26, width, 20, Component.literal("关闭"), StyledButton.Variant.GHOST, button -> onClose()));
    }

    private StyledButton navButton(int x, int y, int width, int height, Tab tab) {
        StyledButton.Variant variant = this.activeTab == tab ? StyledButton.Variant.TAB_ACTIVE : StyledButton.Variant.TAB_IDLE;
        return new StyledButton(x, y, width, height, Component.literal(tabLabel(tab)), variant, button -> switchTab(tab));
    }

    private void buildDashboardWidgets() {
        boolean compact = compactContentLayout();
        int panelLeft = contentLeft() + 10;
        int panelTop = contentTop() + 48;
        int panelWidth = contentWidth() - 20;
        int gap = 12;
        int rightX;
        int rightY;
        int rightWidth;
        int controlLeft;
        int controlTop;
        int controlWidth;
        int halfWidth;

        if (compact) {
            rightX = panelLeft;
            rightY = panelTop + 108;
            rightWidth = panelWidth;
            controlLeft = rightX + 14;
            controlTop = rightY + 86;
            controlWidth = rightWidth - 28;
            halfWidth = Math.max(96, (controlWidth - 8) / 2);
        } else {
            int leftWidth = Math.max(250, Math.min(344, (contentWidth() - gap) * 3 / 5));
            rightWidth = contentWidth() - leftWidth - gap;
            rightX = panelLeft + leftWidth + gap;
            rightY = panelTop;
            controlLeft = rightX + 14;
            controlTop = rightY + 140;
            controlWidth = rightWidth - 28;
            halfWidth = Math.max(90, (controlWidth - 8) / 2);
        }

        addRenderableWidget(new StyledButton(controlLeft, controlTop, halfWidth, 20, Component.literal("刷新链接"), StyledButton.Variant.PRIMARY, button -> {
            this.pairingLink = AppServices.get().refreshPairingLink();
            this.statusMessage = "配对链接已刷新。";
            rebuildWidgets();
        }));
        addRenderableWidget(new StyledButton(controlLeft + halfWidth + 8, controlTop, halfWidth, 20, Component.literal("复制链接"), StyledButton.Variant.SECONDARY, button -> {
            PlatformServices.client().copyToClipboard(this.pairingLink);
            this.statusMessage = "配对链接已复制。";
            rebuildWidgets();
        }));
        addRenderableWidget(new StyledButton(controlLeft, controlTop + 24, halfWidth, 20, Component.literal("A 通道设置"), StyledButton.Variant.GHOST, button -> openChannelProfileScreen(ChannelTarget.A)));
        addRenderableWidget(new StyledButton(controlLeft + halfWidth + 8, controlTop + 24, halfWidth, 20, Component.literal("B 通道设置"), StyledButton.Variant.GHOST, button -> openChannelProfileScreen(ChannelTarget.B)));
        addRenderableWidget(new StyledButton(controlLeft, controlTop + 48, controlWidth, 20, Component.literal("显示二维码"), StyledButton.Variant.GHOST, button -> openPairingQrScreen()));
        addRenderableWidget(new StyledButton(controlLeft, controlTop + 72, controlWidth, 20, Component.literal("名字状态显示： " + booleanLabel(AppServices.get().getConfig().ui.showPlayerStatus)), StyledButton.Variant.GHOST, button -> {
            AppConfig config = AppServices.get().getConfig();
            config.ui.showPlayerStatus = !config.ui.showPlayerStatus;
            AppServices.get().saveConfig(config);
            this.statusMessage = "名字状态显示已" + (config.ui.showPlayerStatus ? "开启" : "关闭");
            rebuildWidgets();
        }));
    }

    private void buildRuleWidgets() {
        AppConfig config = AppServices.get().getConfig();
        if (!this.creatingRule && this.editingRule != null && this.editingRule.id != null && !this.editingRule.id.trim().isEmpty()) {
            this.selectedRuleIndex = findRuleIndexById(this.editingRule.id);
        }
        if (config.rules.isEmpty()) {
            this.creatingRule = true;
            this.selectedRuleIndex = -1;
            if (this.editingRule == null) {
                this.editingRule = createDefaultRuleDraft();
            }
        } else if (this.creatingRule) {
            if (this.editingRule == null) {
                this.editingRule = createDefaultRuleDraft();
            }
        } else {
            this.selectedRuleIndex = Math.max(0, Math.min(this.selectedRuleIndex < 0 ? 0 : this.selectedRuleIndex, config.rules.size() - 1));
            RuleDefinition selected = config.rules.get(this.selectedRuleIndex);
            if (this.editingRule == null || !selected.id.equals(this.editingRule.id)) {
                this.editingRule = copyRule(selected);
            }
        }

        int panelTop = contentTop() + 34;
        int panelHeight = contentHeight() - 44;
        boolean compact = compactContentLayout();
        int listPanelLeft = contentLeft() + 10;
        int listPanelWidth = compact ? contentWidth() - 20 : ruleListWidth();
        int listPanelHeight = compact ? Math.min(160, Math.max(132, panelHeight / 3)) : panelHeight;
        int listLeft = listPanelLeft + 14;
        int listTop = panelTop + 36;
        int listWidth = listPanelWidth - 28;
        int listActionY = panelTop + listPanelHeight - 30;
        int visibleSlots = Math.max(1, (listActionY - listTop) / 24);
        int maxRuleScroll = Math.max(0, config.rules.size() - visibleSlots);
        this.ruleListScroll = clamp(this.ruleListScroll, 0, maxRuleScroll);
        if (!this.creatingRule && this.selectedRuleIndex >= 0) {
            if (this.selectedRuleIndex < this.ruleListScroll) {
                this.ruleListScroll = this.selectedRuleIndex;
            } else if (this.selectedRuleIndex >= this.ruleListScroll + visibleSlots) {
                this.ruleListScroll = this.selectedRuleIndex - visibleSlots + 1;
            }
        }
        int startIndex = Math.max(0, Math.min(this.ruleListScroll, maxRuleScroll));
        int visibleRules = Math.min(visibleSlots, Math.max(0, config.rules.size() - startIndex));
        for (int i = 0; i < visibleRules; i++) {
            final int index = startIndex + i;
            RuleDefinition rule = config.rules.get(index);
            StyledButton.Variant variant = (!this.creatingRule && index == this.selectedRuleIndex) ? StyledButton.Variant.TAB_ACTIVE : StyledButton.Variant.TAB_IDLE;
            addRenderableWidget(new StyledButton(listLeft, listTop + i * 24, listWidth, 20, Component.literal(trim(labelForRule(rule), 26)), variant, button -> {
                this.selectedRuleIndex = index;
                this.creatingRule = false;
                this.editingRule = copyRule(config.rules.get(index));
                rebuildWidgets();
            }));
        }
        int listActionWidth = (listWidth - 8) / 2;
        addRenderableWidget(new StyledButton(listLeft, listActionY, listActionWidth, 20, Component.literal("新建规则"), StyledButton.Variant.GHOST, button -> {
            this.creatingRule = true;
            this.selectedRuleIndex = -1;
            this.editingRule = createDefaultRuleDraft();
            rebuildWidgets();
        }));
        addRenderableWidget(new StyledButton(listLeft + listActionWidth + 8, listActionY, listActionWidth, 20, Component.literal("顺序设置"), StyledButton.Variant.SECONDARY, button -> openRuleOrderScreen()));

        int editorPanelTop = compact ? panelTop + listPanelHeight + 12 : panelTop;
        int editorPanelLeft = compact ? contentLeft() + 10 : contentLeft() + ruleListWidth() + 22;
        int editorTop = editorPanelTop + 38;
        int editorLeft = editorPanelLeft + 14;
        int editorWidth = contentRight() - editorPanelLeft - 10 - 28;
        if (compact) {
            editorWidth = contentWidth() - 48;
        }
        int halfWidth = (editorWidth - 8) / 2;
        boolean stackedEditor = editorWidth < 312;
        boolean stackedActions = editorWidth < 404;
        int rowY = editorTop;

        addRenderableWidget(new StyledButton(editorLeft, rowY, editorWidth, 20, Component.literal("规则名： " + trim(ruleDisplayName(this.editingRule), stackedEditor ? 22 : 28)), StyledButton.Variant.SECONDARY, button -> openRuleNamePrompt()));
        rowY += 24;
        addRenderableWidget(new StyledButton(editorLeft, rowY, editorWidth, 20, Component.literal("触发： " + trim(triggerLabel(this.editingRule.trigger), stackedEditor ? 22 : 28)), StyledButton.Variant.SECONDARY, button -> openTriggerPicker()));
        rowY += 24;
        addRenderableWidget(new StyledButton(editorLeft, rowY, editorWidth, 20, Component.literal("波形： " + trim(resolveWaveformName(this.editingRule), stackedEditor ? 22 : 28)), StyledButton.Variant.SECONDARY, button -> openWaveformPicker()));
        rowY += 24;
        if (stackedEditor) {
            addRenderableWidget(new StyledButton(editorLeft, rowY, editorWidth, 20, Component.literal("通道： " + channelLabel(this.editingRule.channel)), StyledButton.Variant.GHOST, button -> {
                this.editingRule.channel = nextChannel(this.editingRule.channel);
                rebuildWidgets();
            }));
            rowY += 24;
            addRenderableWidget(new StyledButton(editorLeft, rowY, editorWidth, 20, Component.literal("清空队列： " + booleanLabel(this.editingRule.clearBeforeSend)), StyledButton.Variant.GHOST, button -> {
                this.editingRule.clearBeforeSend = !this.editingRule.clearBeforeSend;
                rebuildWidgets();
            }));
            rowY += 24;
        } else {
            addRenderableWidget(new StyledButton(editorLeft, rowY, halfWidth, 20, Component.literal("通道： " + channelLabel(this.editingRule.channel)), StyledButton.Variant.GHOST, button -> {
                this.editingRule.channel = nextChannel(this.editingRule.channel);
                rebuildWidgets();
            }));
            addRenderableWidget(new StyledButton(editorLeft + halfWidth + 8, rowY, halfWidth, 20, Component.literal("清空队列： " + booleanLabel(this.editingRule.clearBeforeSend)), StyledButton.Variant.GHOST, button -> {
                this.editingRule.clearBeforeSend = !this.editingRule.clearBeforeSend;
                rebuildWidgets();
            }));
            rowY += 24;
        }
        StyledButton conditionButton = new StyledButton(editorLeft, rowY, editorWidth, 20, Component.literal("条件： " + trim(conditionSummary(this.editingRule), stackedEditor ? 24 : 30)), StyledButton.Variant.GHOST, button -> openRuleConditionPrompt());
        conditionButton.active = isConditionEditable(this.editingRule);
        addRenderableWidget(conditionButton);
        rowY += 24;

        addRenderableWidget(new StyledButton(editorLeft, rowY, editorWidth, 20, Component.literal("冷却： " + this.editingRule.cooldownMs + "ms"), StyledButton.Variant.GHOST, button -> openLongPrompt(
            "冷却时间",
            "输入毫秒，不小于 0。",
            "毫秒",
            Long.toString(this.editingRule.cooldownMs),
            value -> {
                this.editingRule.cooldownMs = Math.max(0L, value);
                rebuildWidgets();
            }
        )));
        rowY += 28;

        if (stackedEditor && editorWidth < 320) {
            addRenderableWidget(new StyledButton(editorLeft, rowY, editorWidth, 20, Component.literal("启用： " + booleanLabel(this.editingRule.enabled)), StyledButton.Variant.SECONDARY, button -> {
                this.editingRule.enabled = !this.editingRule.enabled;
                rebuildWidgets();
            }));
            rowY += 24;
            addRenderableWidget(new StyledButton(editorLeft, rowY, editorWidth, 20, Component.literal("说明"), StyledButton.Variant.SECONDARY, button -> openRuleGuideScreen()));
            rowY += 28;
        } else {
            addRenderableWidget(new StyledButton(editorLeft, rowY, halfWidth, 20, Component.literal("启用： " + booleanLabel(this.editingRule.enabled)), StyledButton.Variant.SECONDARY, button -> {
                this.editingRule.enabled = !this.editingRule.enabled;
                rebuildWidgets();
            }));
            addRenderableWidget(new StyledButton(editorLeft + halfWidth + 8, rowY, halfWidth, 20, Component.literal("说明"), StyledButton.Variant.SECONDARY, button -> openRuleGuideScreen()));
            rowY += 28;
        }

        boolean deviceReady = AppServices.get().isDeviceBound();
        if (stackedActions) {
            StyledButton testButton = new StyledButton(editorLeft, rowY, editorWidth, 20, Component.literal("测试"), StyledButton.Variant.PRIMARY, button -> {
                try {
                    AppServices.get().testRule(this.editingRule);
                    this.statusMessage = "规则测试已发送。";
                } catch (RuntimeException exception) {
                    this.statusMessage = safeMessage(exception, "规则测试失败。");
                }
                rebuildWidgets();
            });
            testButton.active = deviceReady;
            addRenderableWidget(testButton);
            rowY += 24;

            StyledButton saveButton = new StyledButton(editorLeft, rowY, editorWidth, 20, Component.literal("保存"), StyledButton.Variant.SECONDARY, button -> saveEditingRule());
            saveButton.active = canSaveRule(this.editingRule);
            addRenderableWidget(saveButton);
            rowY += 24;

            StyledButton deleteButton = new StyledButton(editorLeft, rowY, editorWidth, 20, Component.literal(this.creatingRule ? "放弃" : "删除"), this.creatingRule ? StyledButton.Variant.GHOST : StyledButton.Variant.DANGER, button -> {
                if (this.creatingRule || this.editingRule.id == null || this.editingRule.id.trim().isEmpty()) {
                    cancelRuleEditing();
                    return;
                }
                try {
                    AppServices.get().deleteRule(this.editingRule.id);
                    this.statusMessage = "规则已删除。";
                    this.creatingRule = false;
                    AppConfig current = AppServices.get().getConfig();
                    if (current.rules.isEmpty()) {
                        this.selectedRuleIndex = -1;
                        this.editingRule = createDefaultRuleDraft();
                        this.creatingRule = true;
                    } else {
                        this.selectedRuleIndex = Math.max(0, Math.min(this.selectedRuleIndex, current.rules.size() - 1));
                        this.editingRule = copyRule(current.rules.get(this.selectedRuleIndex));
                    }
                } catch (RuntimeException exception) {
                    this.statusMessage = safeMessage(exception, "删除规则失败。");
                }
                rebuildWidgets();
            });
            deleteButton.active = this.creatingRule || (this.editingRule.id != null && !this.editingRule.id.trim().isEmpty());
            addRenderableWidget(deleteButton);
            return;
        }

        int actionWidth = (editorWidth - 16) / 3;
        StyledButton testButton = new StyledButton(editorLeft, rowY, actionWidth, 20, Component.literal("测试"), StyledButton.Variant.PRIMARY, button -> {
            try {
                AppServices.get().testRule(this.editingRule);
                this.statusMessage = "规则测试已发送。";
            } catch (RuntimeException exception) {
                this.statusMessage = safeMessage(exception, "规则测试失败。");
            }
            rebuildWidgets();
        });
        testButton.active = deviceReady;
        addRenderableWidget(testButton);

        StyledButton saveButton = new StyledButton(editorLeft + actionWidth + 8, rowY, actionWidth, 20, Component.literal("保存"), StyledButton.Variant.SECONDARY, button -> saveEditingRule());
        saveButton.active = canSaveRule(this.editingRule);
        addRenderableWidget(saveButton);

        StyledButton deleteButton = new StyledButton(editorLeft + (actionWidth + 8) * 2, rowY, actionWidth, 20, Component.literal(this.creatingRule ? "放弃" : "删除"), this.creatingRule ? StyledButton.Variant.GHOST : StyledButton.Variant.DANGER, button -> {
            if (this.creatingRule || this.editingRule.id == null || this.editingRule.id.trim().isEmpty()) {
                cancelRuleEditing();
                return;
            }
            try {
                AppServices.get().deleteRule(this.editingRule.id);
                this.statusMessage = "规则已删除。";
                this.creatingRule = false;
                AppConfig current = AppServices.get().getConfig();
                if (current.rules.isEmpty()) {
                    this.selectedRuleIndex = -1;
                    this.editingRule = createDefaultRuleDraft();
                    this.creatingRule = true;
                } else {
                    this.selectedRuleIndex = Math.max(0, Math.min(this.selectedRuleIndex, current.rules.size() - 1));
                    this.editingRule = copyRule(current.rules.get(this.selectedRuleIndex));
                }
            } catch (RuntimeException exception) {
                this.statusMessage = safeMessage(exception, "删除规则失败。");
            }
            rebuildWidgets();
        });
        deleteButton.active = this.creatingRule || (this.editingRule.id != null && !this.editingRule.id.trim().isEmpty());
        addRenderableWidget(deleteButton);
    }

    private void buildWaveformWidgets() {
        AppConfig config = AppServices.get().getConfig();
        if (!config.waveforms.isEmpty()) {
            this.selectedWaveformIndex = Math.max(0, Math.min(this.selectedWaveformIndex, config.waveforms.size() - 1));
        }

        int panelTop = contentTop() + 34;
        int panelHeight = contentHeight() - 44;
        boolean compact = compactContentLayout();
        int listPanelLeft = contentLeft() + 10;
        int listPanelWidth = compact ? contentWidth() - 20 : ruleListWidth();
        int listPanelHeight = compact ? Math.min(140, Math.max(116, panelHeight / 3)) : panelHeight;
        int listLeft = listPanelLeft + 14;
        int listTop = panelTop + 36;
        int listWidth = listPanelWidth - 28;
        int visibleWaveforms = compact ? Math.min(3, config.waveforms.size()) : Math.min(7, config.waveforms.size());
        for (int i = 0; i < visibleWaveforms; i++) {
            final int index = i;
            WaveformDefinition waveform = config.waveforms.get(i);
            StyledButton.Variant variant = index == this.selectedWaveformIndex ? StyledButton.Variant.TAB_ACTIVE : StyledButton.Variant.TAB_IDLE;
            addRenderableWidget(new StyledButton(listLeft, listTop + i * 24, listWidth, 20, Component.literal(trim(waveform.name, 26)), variant, button -> {
                this.selectedWaveformIndex = index;
                rebuildWidgets();
            }));
        }

        int listActionY = panelTop + listPanelHeight - 30;
        addRenderableWidget(new StyledButton(listLeft, listActionY, listWidth, 20, Component.literal("导入说明"), StyledButton.Variant.GHOST, button -> openWaveformImportGuide()));

        int detailTop = compact ? panelTop + listPanelHeight + 12 : panelTop;
        int detailPanelLeft = compact ? contentLeft() + 10 : contentLeft() + ruleListWidth() + 22;
        int actionLeft = detailPanelLeft + 14;
        int actionTop = detailTop + 36;
        int actionWidth = compact ? contentWidth() - 48 : contentRight() - detailPanelLeft - 10 - 28;
        int halfWidth = (actionWidth - 8) / 2;
        boolean stackedTests = compact || actionWidth < 320;
        addRenderableWidget(new StyledButton(actionLeft, actionTop, actionWidth, 20, Component.literal("导入 pulse 文本"), StyledButton.Variant.PRIMARY, button -> this.minecraft.setScreen(new WaveformImportScreen(this, "pulse"))));
        addRenderableWidget(new StyledButton(actionLeft, actionTop + 24, actionWidth, 20, Component.literal("导入 HEX 帧"), StyledButton.Variant.SECONDARY, button -> this.minecraft.setScreen(new WaveformImportScreen(this, "hex"))));

        if (!config.waveforms.isEmpty()) {
            WaveformDefinition selected = config.waveforms.get(this.selectedWaveformIndex);
            addRenderableWidget(new StyledButton(actionLeft, actionTop + 52, actionWidth, 20, Component.literal("重命名： " + trim(selected.name, 28)), StyledButton.Variant.GHOST, button -> openWaveformRenamePrompt(selected)));
            addRenderableWidget(new StyledButton(actionLeft, actionTop + 76, actionWidth, 20, Component.literal("删除波形"), StyledButton.Variant.DANGER, button -> {
                try {
                    AppServices.get().deleteWaveform(selected.id);
                    this.statusMessage = "波形已删除。";
                    this.selectedWaveformIndex = 0;
                } catch (RuntimeException exception) {
                    this.statusMessage = safeMessage(exception, "删除波形失败。");
                }
                rebuildWidgets();
            }));

            boolean deviceReady = AppServices.get().isDeviceBound();
            int actionRowY = actionTop + 104;
            StyledButton testAButton = new StyledButton(actionLeft, actionRowY, stackedTests ? actionWidth : halfWidth, 20, Component.literal("试发 A"), StyledButton.Variant.GHOST, button -> {
                try {
                    AppServices.get().testWaveform(selected.id, DeviceChannel.A);
                    this.statusMessage = "波形已发送到 A 通道。";
                } catch (RuntimeException exception) {
                    this.statusMessage = safeMessage(exception, "发送 A 通道失败。");
                }
                rebuildWidgets();
            });
            testAButton.active = deviceReady;
            addRenderableWidget(testAButton);

            StyledButton testBButton = new StyledButton(stackedTests ? actionLeft : actionLeft + halfWidth + 8, stackedTests ? actionRowY + 24 : actionRowY, stackedTests ? actionWidth : halfWidth, 20, Component.literal("试发 B"), StyledButton.Variant.GHOST, button -> {
                try {
                    AppServices.get().testWaveform(selected.id, DeviceChannel.B);
                    this.statusMessage = "波形已发送到 B 通道。";
                } catch (RuntimeException exception) {
                    this.statusMessage = safeMessage(exception, "发送 B 通道失败。");
                }
                rebuildWidgets();
            });
            testBButton.active = deviceReady;
            addRenderableWidget(testBButton);
        }
    }

    private void buildTransferWidgets() {
        int left = contentLeft() + 14;
        int top = contentTop() + 48;
        int width = contentWidth() - 28;
        boolean compact = width < 420;
        int buttonWidth = compact ? width : (width - 16) / 3;
        addRenderableWidget(new StyledButton(left, top, buttonWidth, 20, Component.literal("导出 ZIP"), StyledButton.Variant.PRIMARY, button -> {
            try {
                Path path = AppServices.get().exportConfigArchive();
                this.lastExportPath = path.toString();
                PlatformServices.client().copyToClipboard(this.lastExportPath);
                PlatformServices.client().openInFileManager(path);
                this.statusMessage = "配置已导出，已打开文件夹。";
            } catch (RuntimeException exception) {
                this.statusMessage = safeMessage(exception, "导出配置失败。");
            }
            rebuildWidgets();
        }));
        int secondTop = compact ? top + 24 : top;
        int thirdTop = compact ? top + 48 : top;
        int secondLeft = compact ? left : left + buttonWidth + 8;
        int thirdLeft = compact ? left : left + (buttonWidth + 8) * 2;
        addRenderableWidget(new StyledButton(secondLeft, secondTop, buttonWidth, 20, Component.literal("导入路径"), StyledButton.Variant.SECONDARY, button -> openImportPrompt()));
        addRenderableWidget(new StyledButton(thirdLeft, thirdTop, buttonWidth, 20, Component.literal("恢复默认"), StyledButton.Variant.DANGER, button -> openRestoreDefaultsPrompt()));
    }

    private void openImportPrompt() {
        this.minecraft.setScreen(new TextPromptScreen(
            this,
            "导入 ZIP 配置",
            "输入 ZIP 路径，导入前会自动备份当前配置。",
            "ZIP 路径",
            "下一步",
            PlatformServices.client().readClipboard(),
            value -> {
                if (value == null || value.trim().isEmpty()) {
                    throw new IllegalArgumentException("ZIP 路径不能为空。");
                }
                openImportConfirm(Paths.get(value));
            }
        ));
    }

    private void openImportConfirm(final Path path) {
        this.minecraft.setScreen(new ConfirmDialogScreen(
            this,
            "确认导入",
            "将导入 " + path.getFileName() + "，当前配置会被覆盖，导入前会自动备份。是否继续？",
            "确认导入",
            () -> importZipPath(path)
        ));
    }

    private void openRestoreDefaultsPrompt() {
        this.minecraft.setScreen(new ConfirmDialogScreen(
            this,
            "恢复默认配置",
            "恢复默认前会自动备份当前配置。该操作会重置规则和波形，但保留连接信息。是否继续？",
            "确认恢复",
            () -> {
                AppServices.get().restoreDefaultConfig();
                this.minecraft.setScreen(new ControlCenterScreen(Tab.TRANSFER, "已恢复默认配置。"));
            }
        ));
    }

    private void importZipPath(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("ZIP 路径不能为空。");
        }
        String value = path.toString();
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("ZIP 路径不能为空。");
        }
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("ZIP 文件不存在： " + value);
        }
        if (Files.isDirectory(path)) {
            throw new IllegalArgumentException("请选择 ZIP 文件，不要拖入文件夹。");
        }
        try (InputStream inputStream = Files.newInputStream(path)) {
            AppServices.get().importConfigArchive(inputStream);
        } catch (IOException exception) {
            throw new RuntimeException(safeMessage(exception, "读取 ZIP 文件失败。"), exception);
        }
        this.minecraft.setScreen(new ControlCenterScreen(Tab.TRANSFER, "配置已导入。"));
    }

    private void openRuleNamePrompt() {
        this.minecraft.setScreen(new TextPromptScreen(
            this,
            "修改规则名称",
            "留空时会按触发时机自动命名。",
            "规则名称",
            "确定",
            this.editingRule.name,
            value -> {
                this.editingRule.name = value;
                rebuildWidgets();
                this.minecraft.setScreen(this);
            }
        ));
    }

    private void openWaveformRenamePrompt(final WaveformDefinition selected) {
        this.minecraft.setScreen(new TextPromptScreen(
            this,
            "重命名波形",
            "修改当前选中的波形名称。",
            "波形名称",
            "确定",
            selected.name,
            value -> {
                String trimmedValue = value.trim();
                if (trimmedValue.isEmpty()) {
                    throw new IllegalArgumentException("波形名称不能为空。");
                }
                WaveformDefinition updated = selected.copy();
                updated.name = trimmedValue;
                AppServices.get().saveWaveform(updated);
                this.statusMessage = "波形名称已更新。";
                this.minecraft.setScreen(this);
                rebuildWidgets();
            }
        ));
    }

    private void openTriggerPicker() {
        final List<TriggerDefinition> triggers = AppServices.get().getTriggers();
        this.minecraft.setScreen(new OptionPickerScreen<TriggerDefinition>(
            this,
            "选择触发时机",
            "点左侧条目直接切换",
            triggers,
            definition -> definition.label,
            definition -> definition.description,
            definition -> {
                this.editingRule.trigger = definition.id;
                if (this.editingRule.name == null || this.editingRule.name.trim().isEmpty() || this.editingRule.name.endsWith("规则")) {
                    this.editingRule.name = defaultRuleName(this.editingRule.trigger);
                }
                syncConditionDefaults(this.editingRule);
                rebuildWidgets();
                this.minecraft.setScreen(this);
            }
        ));
    }

    private void openWaveformPicker() {
        final List<WaveformDefinition> waveforms = AppServices.get().getConfig().waveforms;
        if (waveforms.isEmpty()) {
            this.statusMessage = "请先导入波形。";
            rebuildWidgets();
            return;
        }
        this.minecraft.setScreen(new OptionPickerScreen<WaveformDefinition>(
            this,
            "选择波形",
            "点左侧条目直接切换",
            waveforms,
            waveform -> waveform.name,
            waveform -> sourceTypeLabel(waveform.sourceType) + " / " + waveform.estimatedDurationMs + " 毫秒",
            waveform -> {
                this.editingRule.waveformId = waveform.id;
                rebuildWidgets();
                this.minecraft.setScreen(this);
            }
        ));
    }

    private void openChannelProfileScreen(ChannelTarget channel) {
        this.minecraft.setScreen(new ChannelProfileScreen(this, channel));
    }

    private void openPairingQrScreen() {
        this.minecraft.setScreen(new PairingQrScreen(this));
    }

    private void openRuleOrderScreen() {
        this.minecraft.setScreen(new RuleOrderScreen(this));
    }

    private void openRuleGuideScreen() {
        List<String> lines = new ArrayList<String>();
        lines.add("规则名称：备注。");
        lines.add("触发时机：什么时候发。");
        lines.add("波形：触发后发哪条。");
        lines.add("通道：A、B 或 A和B。");
        lines.add("条件：低血、低饥饿、护甲低耐久会用到。");
        lines.add("冷却：两次触发的最短间隔。");
        lines.add("清空队列：发送前先清旧波形。");
        lines.add("强度走 A/B 通道设置。");
        this.minecraft.setScreen(new InfoScreen(this, "新建规则说明", "规则字段简表", lines));
    }

    private void openRuleConditionPrompt() {
        if (!isConditionEditable(this.editingRule)) {
            return;
        }
        if (TriggerRegistry.ARMOR_LOW.equals(this.editingRule.trigger)) {
            openIntegerPrompt(
                "护甲耐久阈值",
                "输入百分比，范围 1 到 100。",
                "百分比",
                Integer.toString((int) Math.round(resolveRuleCondition(this.editingRule, "ratio", 0.15D) * 100.0D)),
                value -> {
                    this.editingRule.conditions.put("ratio", Double.valueOf(clamp(value, 1, 100) / 100.0D));
                    rebuildWidgets();
                }
            );
            return;
        }
        openIntegerPrompt(
            "触发阈值",
            TriggerRegistry.LOW_HEALTH.equals(this.editingRule.trigger) ? "输入生命阈值，范围 1 到 20。" : "输入饥饿阈值，范围 1 到 20。",
            TriggerRegistry.LOW_HEALTH.equals(this.editingRule.trigger) ? "生命值" : "饥饿值",
            Integer.toString((int) Math.round(resolveRuleCondition(this.editingRule, "threshold", 6.0D))),
            value -> {
                this.editingRule.conditions.put("threshold", Double.valueOf(clamp(value, 1, 20)));
                rebuildWidgets();
            }
        );
    }

    private void openBaseStrengthPrompt() {
        if (this.editingRule.useGlobalStrength) {
            openGlobalBaseStrengthPrompt();
            return;
        }
        openIntegerPrompt(
            "基础强度",
            "输入 0 到 200。",
            "强度",
            Integer.toString(this.editingRule.baseStrength),
            value -> {
                this.editingRule.baseStrength = clamp(value, 0, 200);
                if (this.editingRule.maxStrength < this.editingRule.baseStrength) {
                    this.editingRule.maxStrength = this.editingRule.baseStrength;
                }
                rebuildWidgets();
            }
        );
    }

    private void openGlobalBaseStrengthPrompt() {
        AppConfig config = AppServices.get().getConfig();
        openIntegerPrompt(
            "全局基础强度",
            "输入 0 到 200。",
            "强度",
            Integer.toString(config.strength.baseStrength),
            value -> {
                AppConfig latest = AppServices.get().getConfig();
                latest.strength.baseStrength = clamp(value, 0, 200);
                if (latest.strength.maxStrength < latest.strength.baseStrength) {
                    latest.strength.maxStrength = latest.strength.baseStrength;
                }
                AppServices.get().saveConfig(latest);
                rebuildWidgets();
            }
        );
    }

    private void openMaxStrengthPrompt() {
        if (this.editingRule.useGlobalStrength) {
            openGlobalMaxStrengthPrompt(this.editingRule.channel);
            return;
        }
        openIntegerPrompt(
            "最高强度",
            maxStrengthPromptText(this.editingRule.channel),
            "强度",
            Integer.toString(this.editingRule.maxStrength),
            value -> {
                this.editingRule.maxStrength = clamp(value, this.editingRule.baseStrength, 200);
                rebuildWidgets();
            }
        );
    }

    private void openGlobalMaxStrengthPrompt(ChannelTarget channel) {
        AppConfig config = AppServices.get().getConfig();
        openIntegerPrompt(
            "全局最高强度",
            maxStrengthPromptText(channel),
            "强度",
            Integer.toString(config.strength.maxStrength),
            value -> {
                AppConfig latest = AppServices.get().getConfig();
                latest.strength.maxStrength = clamp(value, latest.strength.baseStrength, 200);
                AppServices.get().saveConfig(latest);
                rebuildWidgets();
            }
        );
    }

    private String maxStrengthPromptText(ChannelTarget channel) {
        int cap = deviceCap(channel);
        return cap >= 200 ? "输入 0 到 200。" : "输入 0 到 200，本次最多 " + cap + "。";
    }

    private void openIntegerPrompt(String heading, String description, String inputLabel, String initialValue, IntConsumer consumer) {
        this.minecraft.setScreen(new TextPromptScreen(
            this,
            heading,
            description,
            inputLabel,
            "确定",
            initialValue,
            value -> {
                try {
                    consumer.accept(Integer.parseInt(value));
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("请输入有效数字。");
                }
                this.minecraft.setScreen(this);
            }
        ));
    }

    private void openLongPrompt(String heading, String description, String inputLabel, String initialValue, LongConsumer consumer) {
        this.minecraft.setScreen(new TextPromptScreen(
            this,
            heading,
            description,
            inputLabel,
            "确定",
            initialValue,
            value -> {
                try {
                    consumer.accept(Long.parseLong(value));
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("请输入有效数字。");
                }
                this.minecraft.setScreen(this);
            }
        ));
    }

    private void saveEditingRule() {
        syncConditionDefaults(this.editingRule);
        RuleDefinition saved = copyRule(this.editingRule);
        if (saved.name == null || saved.name.trim().isEmpty()) {
            saved.name = defaultRuleName(saved.trigger);
        }
        if (saved.id == null || saved.id.trim().isEmpty()) {
            saved.id = "rule-" + UUID.randomUUID().toString();
        }
        AppServices.get().saveRule(saved);
        this.creatingRule = false;
        this.editingRule = copyRule(saved);
        this.selectedRuleIndex = findRuleIndexById(saved.id);
        this.statusMessage = "规则已保存。";
        rebuildWidgets();
    }

    private void cancelRuleEditing() {
        AppConfig config = AppServices.get().getConfig();
        if (config.rules.isEmpty()) {
            this.creatingRule = true;
            this.selectedRuleIndex = -1;
            this.editingRule = createDefaultRuleDraft();
        } else {
            this.creatingRule = false;
            this.selectedRuleIndex = Math.max(0, Math.min(this.selectedRuleIndex < 0 ? 0 : this.selectedRuleIndex, config.rules.size() - 1));
            this.editingRule = copyRule(config.rules.get(this.selectedRuleIndex));
        }
        this.statusMessage = "已取消未保存修改。";
        rebuildWidgets();
    }

    private void switchTab(Tab target) {
        this.activeTab = target;
        AppConfig config = AppServices.get().getConfig();
        config.ui.lastOpenedTab = target.name().toLowerCase();
        AppServices.get().saveConfig(config);
        rebuildWidgets();
    }

    private RuleDefinition createDefaultRuleDraft() {
        RuleDefinition rule = new RuleDefinition();
        rule.id = "";
        rule.orderGroup = nextRuleOrderGroup();
        List<TriggerDefinition> triggers = AppServices.get().getTriggers();
        rule.trigger = triggers.isEmpty() ? TriggerRegistry.PLAYER_HURT : triggers.get(0).id;
        rule.name = defaultRuleName(rule.trigger);
        List<WaveformDefinition> waveforms = AppServices.get().getConfig().waveforms;
        rule.waveformId = waveforms.isEmpty() ? "" : waveforms.get(0).id;
        rule.channel = ChannelTarget.A;
        rule.conditions = new LinkedHashMap<String, Double>();
        syncConditionDefaults(rule);
        return rule;
    }

    private RuleDefinition copyRule(RuleDefinition original) {
        RuleDefinition copy = new RuleDefinition();
        copy.id = original.id;
        copy.name = original.name;
        copy.enabled = original.enabled;
        copy.orderGroup = original.orderGroup;
        copy.trigger = original.trigger;
        copy.channel = original.channel;
        copy.waveformId = original.waveformId;
        copy.strengthAction = original.strengthAction;
        copy.strengthScale = original.strengthScale;
        copy.intensityMode = original.intensityMode;
        copy.useGlobalStrength = original.useGlobalStrength;
        copy.baseStrength = original.baseStrength;
        copy.maxStrength = original.maxStrength;
        copy.clearBeforeSend = original.clearBeforeSend;
        copy.cooldownMs = original.cooldownMs;
        copy.conditions = new LinkedHashMap<String, Double>();
        if (original.conditions != null) {
            copy.conditions.putAll(original.conditions);
        }
        return copy;
    }

    private void cycleIntensityMode() {
        IntensityMode[] modes = IntensityMode.values();
        this.editingRule.intensityMode = modes[(this.editingRule.intensityMode.ordinal() + 1) % modes.length];
    }

    private void syncConditionDefaults(RuleDefinition rule) {
        Map<String, Double> nextConditions = new LinkedHashMap<String, Double>();
        if (TriggerRegistry.LOW_HEALTH.equals(rule.trigger) || TriggerRegistry.LOW_FOOD.equals(rule.trigger)) {
            nextConditions.put("threshold", Double.valueOf(resolveRuleCondition(rule, "threshold", 6.0D)));
        } else if (TriggerRegistry.ARMOR_LOW.equals(rule.trigger)) {
            nextConditions.put("ratio", Double.valueOf(resolveRuleCondition(rule, "ratio", 0.15D)));
        }
        rule.conditions.clear();
        rule.conditions.putAll(nextConditions);
    }

    private double resolveRuleCondition(RuleDefinition rule, String key, double fallback) {
        if (rule.conditions == null) {
            return fallback;
        }
        Double value = rule.conditions.get(key);
        return value == null ? fallback : value.doubleValue();
    }

    private boolean isConditionEditable(RuleDefinition rule) {
        return TriggerRegistry.LOW_HEALTH.equals(rule.trigger) || TriggerRegistry.LOW_FOOD.equals(rule.trigger) || TriggerRegistry.ARMOR_LOW.equals(rule.trigger);
    }

    private int findRuleIndexById(String ruleId) {
        List<RuleDefinition> rules = AppServices.get().getConfig().rules;
        for (int i = 0; i < rules.size(); i++) {
            if (ruleId.equals(rules.get(i).id)) {
                return i;
            }
        }
        return rules.isEmpty() ? -1 : 0;
    }

    private int nextRuleOrderGroup() {
        List<RuleDefinition> rules = AppServices.get().getConfig().rules;
        if (rules.isEmpty()) {
            return 0;
        }
        return Math.max(0, rules.get(rules.size() - 1).orderGroup + 1);
    }

    private String resolveWaveformName(RuleDefinition rule) {
        if (rule == null) {
            return "未选择";
        }
        String waveformId = rule.waveformId;
        for (WaveformDefinition waveform : AppServices.get().getConfig().waveforms) {
            if (waveform.id.equals(waveformId)) {
                return waveform.name;
            }
        }
        return rule.strengthAction == StrengthAction.DECREASE ? "保持当前" : "未选择";
    }

    private String labelForRule(RuleDefinition rule) {
        String name = ruleDisplayName(rule);
        String main = name.isEmpty() ? triggerLabel(rule.trigger) + " / " + resolveWaveformName(rule) : name;
        return rowLabel(rule) + " | " + main;
    }

    private boolean canSaveRule(RuleDefinition rule) {
        if (rule == null) {
            return false;
        }
        if (rule.strengthAction == StrengthAction.DECREASE) {
            return true;
        }
        return rule.waveformId != null && !rule.waveformId.trim().isEmpty();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.fillGradient( 0, 0, this.width, this.height, UiPalette.BACKGROUND_TOP, UiPalette.BACKGROUND_BOTTOM);
        UiRender.drawPanel(guiGraphics, sidebarLeft(), sidebarTop(), sidebarWidth(), contentHeight(), UiPalette.SIDEBAR, UiPalette.ACCENT);
        UiRender.drawPanel(guiGraphics, contentLeft(), contentTop(), contentWidth(), contentHeight(), UiPalette.PANEL_MUTED, UiPalette.BORDER_STRONG);

        UiRender.drawSectionTitle(guiGraphics, this.font, "DG-LAB", "NeoForge 1.20.2", sidebarLeft() + 12, sidebarTop() + 12);
        UiRender.drawSectionTitle(guiGraphics, this.font, "控制中心", "设备 / 规则 / 波形", contentLeft() + 14, contentTop() + 12);
        int badgeWidth = this.font.width(tabLabel(this.activeTab)) + 12;
        UiRender.drawStatusBadge(guiGraphics, this.font, tabLabel(this.activeTab), contentRight() - badgeWidth - 14, contentTop() + 12, 0x77202838, UiPalette.ACCENT);

        if (!this.statusMessage.isEmpty()) {
            UiRender.drawWrappedText(guiGraphics, this.font, this.statusMessage, contentLeft() + 14, contentBottom() - 22, contentWidth() - 28, UiPalette.WARNING, 2);
        }

        if (this.activeTab == Tab.DASHBOARD) {
            renderDashboard(guiGraphics);
        } else if (this.activeTab == Tab.RULES) {
            renderRules(guiGraphics);
        } else if (this.activeTab == Tab.WAVEFORMS) {
            renderWaveforms(guiGraphics);
        } else {
            renderTransfer(guiGraphics);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void renderDashboard(GuiGraphics guiGraphics) {
        DeviceSessionManager.DeviceSnapshot snapshot = AppServices.get().getDeviceSnapshot();
        dglabmc.rule.RuleEngine.RuntimeSnapshot runtime = AppServices.get().getRuleRuntimeSnapshot();
        boolean compact = compactContentLayout();
        int panelLeft = contentLeft() + 10;
        int panelTop = contentTop() + 48;
        int panelWidth = contentWidth() - 20;
        int gap = 12;
        int leftX = panelLeft;
        int leftY = panelTop;
        int leftWidth;
        int leftHeight;
        int rightX;
        int rightY;
        int rightWidth;
        int rightHeight;
        int bottomX = panelLeft;
        int bottomY;
        int bottomWidth = panelWidth;
        int bottomHeight;

        if (compact) {
            leftWidth = panelWidth;
            leftHeight = 96;
            rightX = panelLeft;
            rightY = leftY + leftHeight + gap;
            rightWidth = panelWidth;
            rightHeight = 188;
            bottomY = rightY + rightHeight + gap;
            bottomHeight = Math.max(116, contentBottom() - bottomY - 10);
        } else {
            int topHeight = 246;
            leftWidth = Math.max(250, Math.min(344, (contentWidth() - gap) * 3 / 5));
            leftHeight = topHeight;
            rightX = leftX + leftWidth + gap;
            rightY = leftY;
            rightWidth = contentWidth() - leftWidth - gap;
            rightHeight = topHeight;
            bottomY = leftY + topHeight + gap;
            bottomHeight = Math.max(122, contentBottom() - bottomY - 10);
        }

        UiRender.drawPanel(guiGraphics, leftX, leftY, leftWidth, leftHeight, UiPalette.PANEL, UiPalette.INFO);
        UiRender.drawPanel(guiGraphics, rightX, rightY, rightWidth, rightHeight, UiPalette.PANEL, snapshot.bound ? UiPalette.SUCCESS : UiPalette.DANGER);
        UiRender.drawPanel(guiGraphics, bottomX, bottomY, bottomWidth, bottomHeight, UiPalette.PANEL, UiPalette.ACCENT);

        UiRender.drawSectionTitle(guiGraphics, this.font, "设备会话", "当前连接", leftX + 14, leftY + 14);
        guiGraphics.drawString(this.font, "端口： " + AppServices.get().getDevicePort(), (leftX + 14), (leftY + 42), UiPalette.TEXT_MUTED);
        guiGraphics.drawString(this.font, "客户端 ID： " + trim(snapshot.clientId, compact ? 28 : 34), (leftX + 14), (leftY + 58), UiPalette.TEXT_MUTED);
        guiGraphics.drawString(this.font, "目标 ID： " + trim(snapshot.targetId, compact ? 28 : 34), (leftX + 14), (leftY + 74), UiPalette.TEXT_MUTED);
        if (!compact) {
            guiGraphics.drawString(this.font, "设备强度 A/B： " + snapshot.currentStrengthA + " / " + snapshot.currentStrengthB, (leftX + 14), (leftY + 90), UiPalette.TEXT_MUTED);
            guiGraphics.drawString(this.font, "设备上限 A/B： " + displayStrength(snapshot.maxStrengthA) + " / " + displayStrength(snapshot.maxStrengthB), (leftX + 14), (leftY + 106), UiPalette.TEXT_MUTED);
            guiGraphics.drawString(this.font, "输出状态： " + (runtime.channelA.outputActive || runtime.channelB.outputActive ? "运行中" : "空闲"), (leftX + 14), (leftY + 122), UiPalette.TEXT_MUTED);
        }

        UiRender.drawSectionTitle(guiGraphics, this.font, "通道状态", "A / B", rightX + 14, rightY + 14);
        UiRender.drawStatusBadge(guiGraphics, this.font, snapshot.bound ? "已绑定" : snapshot.connected ? "待绑定" : "未连接", rightX + 14, rightY + 42, snapshot.bound ? 0x6630522A : 0x66402222, snapshot.bound ? UiPalette.SUCCESS : UiPalette.DANGER);
        guiGraphics.drawString(this.font, "A  " + runtime.channelA.currentStrength + " | " + runtime.channelA.effectiveMaxStrength + " | " + (runtime.channelA.outputActive ? "输出中" : "未输出"), (rightX + 14), (rightY + 74), UiPalette.TEXT_PRIMARY);
        guiGraphics.drawString(this.font, "B  " + runtime.channelB.currentStrength + " | " + runtime.channelB.effectiveMaxStrength + " | " + (runtime.channelB.outputActive ? "输出中" : "未输出"), (rightX + 14), (rightY + 90), UiPalette.TEXT_PRIMARY);
        guiGraphics.drawString(this.font, "A 普通/伤害： " + runtime.channelA.eventStrength + " / " + formatDouble(runtime.channelA.damageScale), (rightX + 14), (rightY + 106), UiPalette.TEXT_MUTED);

        UiRender.drawSectionTitle(guiGraphics, this.font, "配对链接", snapshot.bound ? "已绑定" : "点按钮显示二维码", bottomX + 16, bottomY + 14);
        if (compact) {
            guiGraphics.drawString(this.font, "A/B 上限： " + runtime.channelA.effectiveMaxStrength + " / " + runtime.channelB.effectiveMaxStrength, (bottomX + 16), (bottomY + 42), UiPalette.TEXT_PRIMARY);
            UiRender.drawWrappedText(guiGraphics, this.font, "链接： " + this.pairingLink, bottomX + 16, bottomY + 58, bottomWidth - 32, UiPalette.TEXT_MUTED, 2);
        } else {
            guiGraphics.drawString(this.font, "规则顺序： " + ruleOrderSummary(), (bottomX + 16), (bottomY + 42), UiPalette.TEXT_PRIMARY);
            guiGraphics.drawString(this.font, "A 上限： " + runtime.channelA.configuredMaxStrength + " / 本次 " + runtime.channelA.effectiveMaxStrength, (bottomX + 16), (bottomY + 58), UiPalette.TEXT_PRIMARY);
            guiGraphics.drawString(this.font, "B 上限： " + runtime.channelB.configuredMaxStrength + " / 本次 " + runtime.channelB.effectiveMaxStrength, (bottomX + 16), (bottomY + 74), UiPalette.TEXT_PRIMARY);
            UiRender.drawWrappedText(guiGraphics, this.font, "链接： " + this.pairingLink, bottomX + 16, bottomY + 96, bottomWidth - 32, UiPalette.TEXT_MUTED, 4);
        }
    }

    private void renderRules(GuiGraphics guiGraphics) {
        int panelTop = contentTop() + 34;
        int panelHeight = contentHeight() - 44;
        int listLeft = contentLeft() + 10;
        boolean compact = compactContentLayout();
        int listWidth = compact ? contentWidth() - 20 : ruleListWidth();
        int listHeight = compact ? Math.min(160, Math.max(132, panelHeight / 3)) : panelHeight;
        int editorLeft = compact ? listLeft : listLeft + listWidth + 12;
        int editorTop = compact ? panelTop + listHeight + 12 : panelTop;
        int editorWidth = compact ? contentWidth() - 20 : contentWidth() - listWidth - 22;
        int editorHeight = compact ? panelHeight - listHeight - 12 : panelHeight;

        UiRender.drawPanel(guiGraphics, listLeft, panelTop, listWidth, listHeight, UiPalette.PANEL, UiPalette.INFO);
        UiRender.drawPanel(guiGraphics, editorLeft, editorTop, editorWidth, editorHeight, UiPalette.PANEL, UiPalette.ACCENT);
        UiRender.drawSectionTitle(guiGraphics, this.font, "规则列表", ruleOrderSummary(), listLeft + 14, panelTop + 14);
        UiRender.drawSectionTitle(guiGraphics, this.font, ruleDisplayName(this.editingRule), this.creatingRule ? rowLabel(this.editingRule) : rowLabel(this.editingRule) + " / " + triggerLabel(this.editingRule.trigger), editorLeft + 14, editorTop + 14);
        if (AppServices.get().getConfig().rules.isEmpty()) {
            guiGraphics.drawString(this.font, "当前没有规则，先新建一条。", (listLeft + 14), (panelTop + 56), UiPalette.TEXT_MUTED);
        } else if (AppServices.get().getConfig().rules.size() > visibleRuleSlots()) {
            String rangeLabel = "显示 " + (this.ruleListScroll + 1) + " - " + Math.min(AppServices.get().getConfig().rules.size(), this.ruleListScroll + visibleRuleSlots()) + " / " + AppServices.get().getConfig().rules.size();
            guiGraphics.drawString(this.font, rangeLabel, (listLeft + 14), (panelTop + listHeight - 44), UiPalette.TEXT_MUTED);
        }
    }

    private void renderWaveforms(GuiGraphics guiGraphics) {
        int panelTop = contentTop() + 34;
        int panelHeight = contentHeight() - 44;
        int listLeft = contentLeft() + 10;
        boolean compact = compactContentLayout();
        int listWidth = compact ? contentWidth() - 20 : ruleListWidth();
        int listHeight = compact ? Math.min(140, Math.max(116, panelHeight / 3)) : panelHeight;
        int detailLeft = compact ? listLeft : listLeft + listWidth + 12;
        int detailTop = compact ? panelTop + listHeight + 12 : panelTop;
        int detailWidth = compact ? contentWidth() - 20 : contentWidth() - listWidth - 22;
        int detailHeight = compact ? panelHeight - listHeight - 12 : panelHeight;
        UiRender.drawPanel(guiGraphics, listLeft, panelTop, listWidth, listHeight, UiPalette.PANEL, UiPalette.INFO);
        UiRender.drawPanel(guiGraphics, detailLeft, detailTop, detailWidth, detailHeight, UiPalette.PANEL, UiPalette.ACCENT);
        UiRender.drawSectionTitle(guiGraphics, this.font, "波形库", "导入与试发", listLeft + 14, panelTop + 14);
        AppConfig config = AppServices.get().getConfig();
        if (!config.waveforms.isEmpty()) {
            WaveformDefinition selected = config.waveforms.get(this.selectedWaveformIndex);
            UiRender.drawSectionTitle(guiGraphics, this.font, selected.name, sourceTypeLabel(selected.sourceType) + " / " + selected.estimatedDurationMs + "ms", detailLeft + 14, detailTop + 14);
            int detailTextY = waveformDetailTextY(detailTop, detailWidth, compact, true);
            int detailTextWidth = detailWidth - 28;
            int maxDescriptionLines = compact ? 2 : 4;
            int previewLines = compact ? 3 : 5;
            int descriptionHeight = UiRender.measureWrappedTextHeight(this.font, fallbackText(selected.description, "未填写说明"), detailTextWidth, maxDescriptionLines);
            UiRender.drawWrappedText(guiGraphics, this.font, fallbackText(selected.description, "未填写说明"), detailLeft + 14, detailTextY, detailTextWidth, UiPalette.TEXT_MUTED, maxDescriptionLines);
            int metaY = detailTextY + Math.max(16, descriptionHeight + 8);
            guiGraphics.drawString(this.font, "帧数： " + selected.frames.size(), (detailLeft + 14), metaY, UiPalette.TEXT_PRIMARY);
            guiGraphics.drawString(this.font, "预览", (detailLeft + 14), (metaY + 18), UiPalette.TEXT_PRIMARY);
            int previewY = metaY + 34;
            for (int i = 0; i < Math.min(previewLines, selected.frames.size()); i++) {
                UiRender.drawWrappedText(guiGraphics, this.font, selected.frames.get(i), detailLeft + 14, previewY, detailWidth - 28, UiPalette.TEXT_MUTED, 1);
                previewY += 14;
            }
        } else {
            UiRender.drawSectionTitle(guiGraphics, this.font, "暂无波形", "先导入", detailLeft + 14, detailTop + 14);
        }
    }

    private void renderTransfer(GuiGraphics guiGraphics) {
        int panelLeft = contentLeft() + 10;
        int panelTop = contentTop() + 34;
        int panelWidth = contentWidth() - 20;
        int panelHeight = contentHeight() - 44;
        int innerLeft = panelLeft + 14;
        int innerWidth = panelWidth - 28;
        UiRender.drawPanel(guiGraphics, panelLeft, panelTop, panelWidth, panelHeight, UiPalette.PANEL, UiPalette.ACCENT);
        UiRender.drawSectionTitle(guiGraphics, this.font, "配置迁移", "ZIP 导入导出", innerLeft, panelTop + 14);

        UiRender.drawPanel(guiGraphics, innerLeft, panelTop + 76, innerWidth, 84, 0x66172233, UiPalette.INFO);
        UiRender.drawWrappedText(guiGraphics, this.font, "把配置文件ZIP拖到这里导入", innerLeft + 16, panelTop + 100, innerWidth - 32, UiPalette.TEXT_PRIMARY, 2);
        guiGraphics.drawString(this.font, "导入前会自动备份旧配置", (innerLeft + 16), (panelTop + 124), UiPalette.TEXT_MUTED);

        UiRender.drawPanel(guiGraphics, innerLeft, panelTop + 176, innerWidth, 94, 0x44172233, UiPalette.BORDER_STRONG);
        guiGraphics.drawString(this.font, "最近导出", (innerLeft + 16), (panelTop + 192), UiPalette.TEXT_PRIMARY);
        if (this.lastExportPath.isEmpty()) {
            guiGraphics.drawString(this.font, "还没有导出记录。", (innerLeft + 16), (panelTop + 212), UiPalette.TEXT_MUTED);
        } else {
            UiRender.drawWrappedText(guiGraphics, this.font, this.lastExportPath, innerLeft + 16, panelTop + 212, innerWidth - 32, UiPalette.TEXT_MUTED, 4);
        }
        guiGraphics.drawString(this.font, "配置版本： " + AppServices.get().getConfig().schemaVersion + " / 加载器： " + AppServices.get().getConfig().loaderFlavor, innerLeft, (panelTop + panelHeight - 18), UiPalette.TEXT_DIM);
    }

    private int sidebarLeft() {
        return 16;
    }

    private int sidebarTop() {
        return 16;
    }

    private int sidebarWidth() {
        return Math.max(120, Math.min(164, this.width / 5));
    }

    private boolean compactContentLayout() {
        return contentWidth() < 430;
    }

    private int waveformDetailTextY(int detailTop, int detailWidth, boolean compact, boolean hasSelection) {
        if (!hasSelection) {
            return detailTop + 86;
        }
        int rows = 5;
        if (compact || detailWidth < 320) {
            rows++;
        }
        return detailTop + 36 + rows * 24 + 10;
    }

    private int contentLeft() {
        return sidebarLeft() + sidebarWidth() + 12;
    }

    private int contentTop() {
        return 16;
    }

    private int contentWidth() {
        return Math.max(160, this.width - contentLeft() - 16);
    }

    private int contentHeight() {
        return Math.max(160, this.height - contentTop() - 16);
    }

    private int contentRight() {
        return contentLeft() + contentWidth();
    }

    private int contentBottom() {
        return contentTop() + contentHeight();
    }

    private int ruleListWidth() {
        int width = contentWidth();
        int preferred = Math.max(176, Math.min(236, width / 3));
        return Math.min(preferred, Math.max(176, width - 232));
    }

    private int visibleRuleSlots() {
        int panelTop = contentTop() + 34;
        int panelHeight = contentHeight() - 44;
        int listPanelHeight = compactContentLayout() ? Math.min(160, Math.max(132, panelHeight / 3)) : panelHeight;
        int listTop = panelTop + 36;
        int listActionY = panelTop + listPanelHeight - 30;
        return Math.max(1, (listActionY - listTop) / 24);
    }

    private boolean isMouseWithinRuleList(double mouseX, double mouseY) {
        int panelTop = contentTop() + 34;
        int panelHeight = contentHeight() - 44;
        int listPanelLeft = contentLeft() + 10;
        int listPanelWidth = compactContentLayout() ? contentWidth() - 20 : ruleListWidth();
        int listPanelHeight = compactContentLayout() ? Math.min(160, Math.max(132, panelHeight / 3)) : panelHeight;
        return mouseX >= listPanelLeft
            && mouseX <= listPanelLeft + listPanelWidth
            && mouseY >= panelTop
            && mouseY <= panelTop + listPanelHeight;
    }

    private String tabLabel(Tab tab) {
        switch (tab) {
            case RULES:
                return "规则";
            case WAVEFORMS:
                return "波形";
            case TRANSFER:
                return "迁移";
            case DASHBOARD:
            default:
                return "总览";
        }
    }

    private String ruleOrderSummary() {
        return "同行并行，命中一行后停止";
    }

    private String rowLabel(RuleDefinition rule) {
        if (rule == null) {
            return "第 1 行";
        }
        return "第 " + (Math.max(0, rule.orderGroup) + 1) + " 行";
    }

    private ChannelTarget nextChannel(ChannelTarget current) {
        if (current == ChannelTarget.A) {
            return ChannelTarget.B;
        }
        if (current == ChannelTarget.B) {
            return ChannelTarget.BOTH;
        }
        return ChannelTarget.A;
    }

    private String channelLabel(ChannelTarget channel) {
        switch (channel) {
            case B:
                return "B";
            case BOTH:
                return "A和B";
            case A:
            default:
                return "A";
        }
    }

    private String intensityModeLabel(IntensityMode mode) {
        switch (mode) {
            case SCALE_BY_DAMAGE:
                return "随伤害变化";
            case SCALE_BY_MISSING_HEALTH:
                return "随缺失生命";
            case SCALE_BY_FOOD_DEFICIT:
                return "随饥饿缺口";
            case FIXED:
            default:
                return "固定强度";
        }
    }

    private String booleanLabel(boolean value) {
        return value ? "是" : "否";
    }

    private String ruleDisplayName(RuleDefinition rule) {
        if (rule == null) {
            return "未选择规则";
        }
        if (rule.name != null && !rule.name.trim().isEmpty()) {
            return rule.name;
        }
        return defaultRuleName(rule.trigger);
    }

    private String defaultRuleName(String triggerId) {
        return triggerLabel(triggerId) + "规则";
    }

    private String triggerLabel(String triggerId) {
        for (TriggerDefinition definition : AppServices.get().getTriggers()) {
            if (definition.id.equals(triggerId)) {
                return definition.label;
            }
        }
        return triggerId == null || triggerId.trim().isEmpty() ? "未设置" : triggerId;
    }

    private String triggerDescription(String triggerId) {
        for (TriggerDefinition definition : AppServices.get().getTriggers()) {
            if (definition.id.equals(triggerId)) {
                return definition.description;
            }
        }
        return "未找到触发说明。";
    }

    private String conditionSummary(RuleDefinition rule) {
        if (rule == null) {
            return "无";
        }
        if (TriggerRegistry.LOW_HEALTH.equals(rule.trigger)) {
            return "生命 <= " + (int) Math.round(resolveRuleCondition(rule, "threshold", 6.0D));
        }
        if (TriggerRegistry.LOW_FOOD.equals(rule.trigger)) {
            return "饥饿 <= " + (int) Math.round(resolveRuleCondition(rule, "threshold", 6.0D));
        }
        if (TriggerRegistry.ARMOR_LOW.equals(rule.trigger)) {
            return "耐久 <= " + (int) Math.round(resolveRuleCondition(rule, "ratio", 0.15D) * 100.0D) + "%";
        }
        return "无";
    }

    private String conditionDetail(RuleDefinition rule) {
        if (rule == null) {
            return "当前触发时机没有额外条件。";
        }
        if (TriggerRegistry.LOW_HEALTH.equals(rule.trigger)) {
            return "生命值小于等于阈值时触发";
        }
        if (TriggerRegistry.LOW_FOOD.equals(rule.trigger)) {
            return "饥饿值小于等于阈值时触发";
        }
        if (TriggerRegistry.ARMOR_LOW.equals(rule.trigger)) {
            return "任一护甲耐久比例小于等于阈值时触发";
        }
        return "当前触发时机没有额外条件。";
    }

    private String sourceTypeLabel(String sourceType) {
        if ("pulse_text".equals(sourceType)) {
            return "脉冲文本";
        }
        if ("hex_frames".equals(sourceType)) {
            return "HEX";
        }
        if ("builtin".equals(sourceType)) {
            return "内置";
        }
        if (sourceType == null || sourceType.trim().isEmpty()) {
            return "未标记";
        }
        return sourceType;
    }

    private void openWaveformImportGuide() {
        List<String> lines = new ArrayList<String>();
        lines.add("pulse 文本：直接贴导出文本。");
        lines.add("HEX 帧：一行一帧。");
        lines.add("导入后可重命名，也能试发。");
        this.minecraft.setScreen(new InfoScreen(this, "导入说明", "波形格式", lines));
    }

    private String fallbackText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String strengthBaseLabel() {
        int value = this.editingRule.useGlobalStrength ? AppServices.get().getConfig().strength.baseStrength : this.editingRule.baseStrength;
        return (this.editingRule.useGlobalStrength ? "全局基础： " : "基础强度： ") + value;
    }

    private String strengthMaxLabel() {
        int configured = this.editingRule.useGlobalStrength ? AppServices.get().getConfig().strength.maxStrength : this.editingRule.maxStrength;
        int effective = effectiveDisplayCap(this.editingRule.channel, configured);
        String prefix = this.editingRule.useGlobalStrength ? "全局上限： " : "最高强度： ";
        return configured == effective ? prefix + configured : prefix + configured + " / 本次 " + effective;
    }

    private String displayStrength(int value) {
        return value <= 0 ? "--" : Integer.toString(value);
    }

    private int deviceCap(ChannelTarget channel) {
        DeviceSessionManager.DeviceSnapshot snapshot = AppServices.get().getDeviceSnapshot();
        int capA = snapshot.maxStrengthA > 0 ? Math.min(snapshot.maxStrengthA, 200) : 200;
        int capB = snapshot.maxStrengthB > 0 ? Math.min(snapshot.maxStrengthB, 200) : 200;
        switch (channel) {
            case B:
                return capB;
            case BOTH:
                return Math.min(capA, capB);
            case A:
            default:
                return capA;
        }
    }

    private int effectiveDisplayCap(ChannelTarget channel, int configured) {
        return Math.min(clamp(configured, 0, 200), deviceCap(channel));
    }

    private String effectiveDisplayCapText(ChannelTarget channel, int configured) {
        int effective = effectiveDisplayCap(channel, configured);
        return effective == configured ? Integer.toString(effective) : configured + " / 本次 " + effective;
    }

    private String safeMessage(Throwable throwable, String fallback) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().trim().isEmpty()) {
            return fallback;
        }
        return throwable.getMessage();
    }

    private String formatDouble(double value) {
        String text = String.format(Locale.ROOT, "%.2f", value);
        while (text.contains(".") && (text.endsWith("0") || text.endsWith("."))) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private String explainThrowable(Throwable throwable) {
        if (throwable == null) {
            return "未知错误";
        }
        String message = throwable.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return throwable.getClass().getSimpleName();
        }
        return trim(throwable.getClass().getSimpleName() + ": " + message, 56);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String trim(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxChars ? text : text.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

}


