package dglabmc.client.ui;

import dglabmc.AppServices;
import dglabmc.platform.PlatformServices;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.StringTextComponent;

public class WaveformImportScreen extends BaseScreen {
    private final String importMode;
    private TextFieldWidget nameField;
    private TextFieldWidget descriptionField;
    private TextFieldWidget rawInputField;

    public WaveformImportScreen(Screen parent, String importMode) {
        super(new StringTextComponent("导入波形"), parent);
        this.importMode = importMode;
    }

    @Override protected int maxPanelWidth() { return 420; }
    @Override protected int compactThreshold() { return 400; }
    @Override protected int panelHeightNormal() { return 220; }
    @Override protected int panelHeightCompact() { return 268; }

    @Override
    protected void buildWidgets() {
        int il = innerLeft();
        int iw = innerWidth();

        this.nameField = new TextFieldWidget(this.font, il, this.panelTop + 52, iw, UiConstants.BTN_HEIGHT, new StringTextComponent("名称"));
        this.nameField.setMaxLength(80);
        this.nameField.setValue("pulse".equals(this.importMode) ? "导入的 pulse 波形" : "导入的 HEX 波形");
        this.children.add(this.nameField);

        this.descriptionField = new TextFieldWidget(this.font, il, this.panelTop + 86, iw, UiConstants.BTN_HEIGHT, new StringTextComponent("说明"));
        this.descriptionField.setMaxLength(120);
        this.children.add(this.descriptionField);

        this.rawInputField = new TextFieldWidget(this.font, il, this.panelTop + 120, iw, UiConstants.BTN_HEIGHT, new StringTextComponent("原始输入"));
        this.rawInputField.setMaxLength(16000);
        this.children.add(this.rawInputField);
        this.setInitialFocus(this.rawInputField);

        int btnY = this.panelTop + 164;
        if (compact) {
            this.addButton(new StyledButton(il, btnY, iw, UiConstants.BTN_HEIGHT, new StringTextComponent("导入"), StyledButton.Variant.PRIMARY, button -> doImport()));
            this.addButton(new StyledButton(il, btnY + UiConstants.BTN_STRIDE, iw, UiConstants.BTN_HEIGHT, new StringTextComponent("粘贴"), StyledButton.Variant.GHOST, button -> this.rawInputField.setValue(PlatformServices.client().readClipboard())));
            this.addButton(new StyledButton(il, btnY + UiConstants.BTN_STRIDE * 2, iw, UiConstants.BTN_HEIGHT, new StringTextComponent("取消"), StyledButton.Variant.SECONDARY, button -> this.minecraft.setScreen(this.parent)));
        } else {
            this.addButton(new StyledButton(il, btnY, 108, UiConstants.BTN_HEIGHT, new StringTextComponent("导入"), StyledButton.Variant.PRIMARY, button -> doImport()));
            this.addButton(new StyledButton(this.panelLeft + 134, btnY, 108, UiConstants.BTN_HEIGHT, new StringTextComponent("粘贴"), StyledButton.Variant.GHOST, button -> this.rawInputField.setValue(PlatformServices.client().readClipboard())));
            this.addButton(new StyledButton(this.panelLeft + 250, btnY, 152, UiConstants.BTN_HEIGHT, new StringTextComponent("取消"), StyledButton.Variant.SECONDARY, button -> this.minecraft.setScreen(this.parent)));
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
            setStatus(exception.getMessage() == null ? "导入失败。" : exception.getMessage());
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
    protected void renderContent(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        int il = innerLeft();
        int iw = innerWidth();
        UiRender.drawSectionTitle(
            matrixStack,
            this.font,
            "导入 " + ("pulse".equals(this.importMode) ? "Dungeonlab+pulse" : "HEX 帧"),
            "粘贴 pulse 文本或 HEX 帧。",
            il,
            this.panelTop + 16
        );
        this.font.draw(matrixStack, "名称", (float) il, (float) (this.panelTop + 42), UiPalette.TEXT_MUTED);
        this.font.draw(matrixStack, "说明", (float) il, (float) (this.panelTop + 76), UiPalette.TEXT_MUTED);
        this.font.draw(matrixStack, "原始输入", (float) il, (float) (this.panelTop + 110), UiPalette.TEXT_MUTED);
        this.nameField.render(matrixStack, mouseX, mouseY, partialTicks);
        this.descriptionField.render(matrixStack, mouseX, mouseY, partialTicks);
        this.rawInputField.render(matrixStack, mouseX, mouseY, partialTicks);
    }
}
