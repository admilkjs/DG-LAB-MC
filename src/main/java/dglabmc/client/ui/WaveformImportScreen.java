package dglabmc.client.ui;

import dglabmc.AppServices;
import dglabmc.platform.PlatformServices;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.StringTextComponent;

public class WaveformImportScreen extends Screen {
    private final Screen parent;
    private final String importMode;
    private TextFieldWidget nameField;
    private TextFieldWidget descriptionField;
    private TextFieldWidget rawInputField;
    private String status = "";

    public WaveformImportScreen(Screen parent, String importMode) {
        super(new StringTextComponent("导入波形"));
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

        this.nameField = new TextFieldWidget(this.font, left + 18, top + 52, panelWidth - 36, 20, "名称");
        this.nameField.setMaxStringLength(80);
        this.nameField.setText("pulse".equals(this.importMode) ? "导入的 pulse 波形" : "导入的 HEX 波形");
        this.children.add(this.nameField);

        this.descriptionField = new TextFieldWidget(this.font, left + 18, top + 86, panelWidth - 36, 20, "说明");
        this.descriptionField.setMaxStringLength(120);
        this.children.add(this.descriptionField);

        this.rawInputField = new TextFieldWidget(this.font, left + 18, top + 120, panelWidth - 36, 20, "原始输入");
        this.rawInputField.setMaxStringLength(16000);
        this.children.add(this.rawInputField);
        this.rawInputField.setFocused2(true);

        if (compact) {
            this.addButton(new StyledButton(left + 18, top + 164, panelWidth - 36, 20, new StringTextComponent("导入"), StyledButton.Variant.PRIMARY, button -> doImport()));
            this.addButton(new StyledButton(left + 18, top + 188, panelWidth - 36, 20, new StringTextComponent("粘贴"), StyledButton.Variant.GHOST, button -> this.rawInputField.setText(PlatformServices.client().readClipboard())));
            this.addButton(new StyledButton(left + 18, top + 212, panelWidth - 36, 20, new StringTextComponent("取消"), StyledButton.Variant.SECONDARY, button -> this.minecraft.displayGuiScreen(this.parent)));
        } else {
            this.addButton(new StyledButton(left + 18, top + 164, 108, 20, new StringTextComponent("导入"), StyledButton.Variant.PRIMARY, button -> doImport()));
            this.addButton(new StyledButton(left + 134, top + 164, 108, 20, new StringTextComponent("粘贴"), StyledButton.Variant.GHOST, button -> this.rawInputField.setText(PlatformServices.client().readClipboard())));
            this.addButton(new StyledButton(left + 250, top + 164, 152, 20, new StringTextComponent("取消"), StyledButton.Variant.SECONDARY, button -> this.minecraft.displayGuiScreen(this.parent)));
        }
    }

    private void doImport() {
        try {
            if ("pulse".equals(this.importMode)) {
                AppServices.get().importPulseWaveform(this.nameField.getText().trim(), this.descriptionField.getText().trim(), this.rawInputField.getText().trim());
            } else {
                AppServices.get().importHexWaveform(this.nameField.getText().trim(), this.descriptionField.getText().trim(), this.rawInputField.getText().trim());
            }
            this.minecraft.displayGuiScreen(new ControlCenterScreen(ControlCenterScreen.Tab.WAVEFORMS, "波形已导入。"));
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
        if (this.nameField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.descriptionField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.rawInputField.keyPressed(keyCode, scanCode, modifiers)) {
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
    public void render(int mouseX, int mouseY, float partialTicks) {
        MatrixStack matrixStack = new MatrixStack();
        this.renderBackground();
        fillGradient(0, 0, this.width, this.height, UiPalette.BACKGROUND_TOP, UiPalette.BACKGROUND_BOTTOM);
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
        this.font.drawString("名称", (float) (left + 18), (float) (top + 42), UiPalette.TEXT_MUTED);
        this.font.drawString("说明", (float) (left + 18), (float) (top + 76), UiPalette.TEXT_MUTED);
        this.font.drawString("原始输入", (float) (left + 18), (float) (top + 110), UiPalette.TEXT_MUTED);
        this.nameField.render(mouseX, mouseY, partialTicks);
        this.descriptionField.render(mouseX, mouseY, partialTicks);
        this.rawInputField.render(mouseX, mouseY, partialTicks);
        if (!this.status.isEmpty()) {
            this.font.drawString(this.status, (float) (left + 18), (float) (top + panelHeight - 22), UiPalette.WARNING);
        }
        super.render(mouseX, mouseY, partialTicks);
    }
}
