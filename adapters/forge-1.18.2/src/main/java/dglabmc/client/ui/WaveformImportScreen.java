package dglabmc.client.ui;

import dglabmc.AppServices;
import dglabmc.platform.PlatformServices;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.TextComponent;

public class WaveformImportScreen extends Screen {
    private final Screen parent;
    private final String importMode;
    private EditBox nameField;
    private EditBox descriptionField;
    private EditBox rawInputField;
    private String status = "";

    public WaveformImportScreen(Screen parent, String importMode) {
        super(new TextComponent("导入波形"));
        this.parent = parent;
        this.importMode = importMode;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(420, this.width - 24);
        boolean compact = panelWidth < 400;
        int panelHeight = Math.min(compact ? 268 : 220, this.height - 24);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        this.nameField = new EditBox(this.font, left + 18, top + 52, panelWidth - 36, 20, new TextComponent("名称"));
        this.nameField.setMaxLength(80);
        this.nameField.setValue("pulse".equals(this.importMode) ? "导入的 pulse 波形" : "导入的 HEX 波形");
        this.addRenderableWidget(this.nameField);

        this.descriptionField = new EditBox(this.font, left + 18, top + 86, panelWidth - 36, 20, new TextComponent("说明"));
        this.descriptionField.setMaxLength(120);
        this.addRenderableWidget(this.descriptionField);

        this.rawInputField = new EditBox(this.font, left + 18, top + 120, panelWidth - 36, 20, new TextComponent("原始输入"));
        this.rawInputField.setMaxLength(16000);
        this.addRenderableWidget(this.rawInputField);
        this.setInitialFocus(this.rawInputField);

        if (compact) {
            this.addRenderableWidget(new StyledButton(left + 18, top + 164, panelWidth - 36, 20, new TextComponent("导入"), StyledButton.Variant.PRIMARY, button -> doImport()));
            this.addRenderableWidget(new StyledButton(left + 18, top + 188, panelWidth - 36, 20, new TextComponent("粘贴"), StyledButton.Variant.GHOST, button -> this.rawInputField.setValue(PlatformServices.client().readClipboard())));
            this.addRenderableWidget(new StyledButton(left + 18, top + 212, panelWidth - 36, 20, new TextComponent("取消"), StyledButton.Variant.SECONDARY, button -> this.minecraft.setScreen(this.parent)));
        } else {
            this.addRenderableWidget(new StyledButton(left + 18, top + 164, 108, 20, new TextComponent("导入"), StyledButton.Variant.PRIMARY, button -> doImport()));
            this.addRenderableWidget(new StyledButton(left + 134, top + 164, 108, 20, new TextComponent("粘贴"), StyledButton.Variant.GHOST, button -> this.rawInputField.setValue(PlatformServices.client().readClipboard())));
            this.addRenderableWidget(new StyledButton(left + 250, top + 164, 152, 20, new TextComponent("取消"), StyledButton.Variant.SECONDARY, button -> this.minecraft.setScreen(this.parent)));
        }
    }

    private void doImport() {
        try {
            if ("pulse".equals(this.importMode)) {
                AppServices.get().importPulseWaveform(this.nameField.getValue().trim(), this.descriptionField.getValue().trim(), this.rawInputField.getValue().trim());
            } else {
                AppServices.get().importHexWaveform(this.nameField.getValue().trim(), this.descriptionField.getValue().trim(), this.rawInputField.getValue().trim());
            }
            this.minecraft.setScreen(new ControlCenterScreen(ControlCenterScreen.Tab.WAVEFORMS, "波形已导入。"));
        } catch (RuntimeException exception) {
            this.status = exception.getMessage() == null ? "导入失败。" : exception.getMessage();
        }
    }

    @Override
    public void tick() {
        this.nameField.tick();
        this.descriptionField.tick();
        this.rawInputField.tick();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.nameField.keyPressed(keyCode, scanCode, modifiers) || this.nameField.canConsumeInput()) {
            return true;
        }
        if (this.descriptionField.keyPressed(keyCode, scanCode, modifiers) || this.descriptionField.canConsumeInput()) {
            return true;
        }
        if (this.rawInputField.keyPressed(keyCode, scanCode, modifiers) || this.rawInputField.canConsumeInput()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.nameField.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (this.descriptionField.charTyped(codePoint, modifiers)) {
            return true;
        }
        return this.rawInputField.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        fillGradient(matrixStack, 0, 0, this.width, this.height, UiPalette.BACKGROUND_TOP, UiPalette.BACKGROUND_BOTTOM);
        int panelWidth = Math.min(420, this.width - 24);
        boolean compact = panelWidth < 400;
        int panelHeight = Math.min(compact ? 268 : 220, this.height - 24);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        UiRender.drawPanel(matrixStack, left, top, panelWidth, panelHeight, UiPalette.PANEL_ELEVATED, UiPalette.ACCENT);
        UiRender.drawSectionTitle(
            matrixStack,
            this.font,
            "导入 " + ("pulse".equals(this.importMode) ? "Dungeonlab+pulse" : "HEX 帧"),
            "粘贴 pulse 文本或 HEX 帧。",
            left + 18,
            top + 16
        );
        this.font.draw(matrixStack, "名称", (float) (left + 18), (float) (top + 42), UiPalette.TEXT_MUTED);
        this.font.draw(matrixStack, "说明", (float) (left + 18), (float) (top + 76), UiPalette.TEXT_MUTED);
        this.font.draw(matrixStack, "原始输入", (float) (left + 18), (float) (top + 110), UiPalette.TEXT_MUTED);
        this.nameField.render(matrixStack, mouseX, mouseY, partialTicks);
        this.descriptionField.render(matrixStack, mouseX, mouseY, partialTicks);
        this.rawInputField.render(matrixStack, mouseX, mouseY, partialTicks);
        if (!this.status.isEmpty()) {
            this.font.draw(matrixStack, this.status, (float) (left + 18), (float) (top + panelHeight - 22), UiPalette.WARNING);
        }
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }
}


