package dglabmc.client.ui;

import dglabmc.platform.PlatformServices;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.StringTextComponent;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class TextPromptScreen extends BaseScreen {
    private final String heading;
    private final String description;
    private final String inputLabel;
    private final String confirmLabel;
    private final String initialValue;
    private final Consumer<String> submitHandler;

    private TextFieldWidget inputField;

    public TextPromptScreen(Screen parent, String heading, String description, String confirmLabel, String initialValue, Consumer<String> submitHandler) {
        this(parent, heading, description, "输入内容", confirmLabel, initialValue, submitHandler);
    }

    public TextPromptScreen(Screen parent, String heading, String description, String inputLabel, String confirmLabel, String initialValue, Consumer<String> submitHandler) {
        super(new StringTextComponent(heading), parent);
        this.heading = heading;
        this.description = description;
        this.inputLabel = inputLabel;
        this.confirmLabel = confirmLabel;
        this.initialValue = initialValue == null ? "" : initialValue;
        this.submitHandler = submitHandler;
    }

    @Override protected int maxPanelWidth() { return 380; }
    @Override protected int compactThreshold() { return 360; }
    @Override protected int panelHeightNormal() { return 146; }
    @Override protected int panelHeightCompact() { return 194; }

    @Override
    protected void buildWidgets() {
        int il = innerLeft();
        int iw = innerWidth();
        this.inputField = new TextFieldWidget(this.font, il, this.panelTop + 54, iw, UiConstants.BTN_HEIGHT, this.inputLabel);
        this.inputField.setMaxStringLength(512);
        this.inputField.setText(this.initialValue);
        this.children.add(this.inputField);
        this.inputField.setFocused2(true);

        int btnY = this.panelTop + 90;
        if (compact) {
            this.addButton(new StyledButton(il, btnY, iw, UiConstants.BTN_HEIGHT, new StringTextComponent(this.confirmLabel), StyledButton.Variant.PRIMARY, button -> onSubmit()));
            this.addButton(new StyledButton(il, btnY + UiConstants.BTN_STRIDE, iw, UiConstants.BTN_HEIGHT, new StringTextComponent("粘贴"), StyledButton.Variant.GHOST, button -> this.inputField.setText(PlatformServices.client().readClipboard())));
            this.addButton(new StyledButton(il, btnY + UiConstants.BTN_STRIDE * 2, iw, UiConstants.BTN_HEIGHT, new StringTextComponent("取消"), StyledButton.Variant.SECONDARY, button -> this.minecraft.displayGuiScreen(this.parent)));
        } else {
            this.addButton(new StyledButton(il, btnY, 100, UiConstants.BTN_HEIGHT, new StringTextComponent(this.confirmLabel), StyledButton.Variant.PRIMARY, button -> onSubmit()));
            this.addButton(new StyledButton(this.panelLeft + 126, btnY, 100, UiConstants.BTN_HEIGHT, new StringTextComponent("粘贴"), StyledButton.Variant.GHOST, button -> this.inputField.setText(PlatformServices.client().readClipboard())));
            this.addButton(new StyledButton(this.panelLeft + 234, btnY, 128, UiConstants.BTN_HEIGHT, new StringTextComponent("取消"), StyledButton.Variant.SECONDARY, button -> this.minecraft.displayGuiScreen(this.parent)));
        }
    }

    @Override
    public void tick() {
        this.inputField.tick();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.inputField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.inputField.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
    }

    public void onFilesDrop(List<Path> paths) {
        if (paths == null || paths.isEmpty()) {
            return;
        }
        this.inputField.setText(paths.get(0).toString());
        setStatus("已填入拖入的文件路径。");
    }

    private void onSubmit() {
        try {
            this.submitHandler.accept(this.inputField.getText().trim());
        } catch (RuntimeException exception) {
            setStatus(exception.getMessage() == null ? "操作失败。" : exception.getMessage());
        }
    }

    @Override
    protected void renderContent(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        int il = innerLeft();
        UiRender.drawSectionTitle(matrixStack, this.font, this.heading, this.description, il, this.panelTop + 14);
        this.font.drawString(this.inputLabel, (float) il, (float) (this.panelTop + 42), UiPalette.TEXT_MUTED);
        this.inputField.render(mouseX, mouseY, partialTicks);
    }
}
