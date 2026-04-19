package dglabmc.client.ui;

import dglabmc.platform.PlatformServices;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.TextComponent;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public class TextPromptScreen extends Screen {
    private final Screen parent;
    private final String heading;
    private final String description;
    private final String inputLabel;
    private final String confirmLabel;
    private final String initialValue;
    private final Consumer<String> submitHandler;

    private EditBox inputField;
    private String status = "";

    public TextPromptScreen(Screen parent, String heading, String description, String confirmLabel, String initialValue, Consumer<String> submitHandler) {
        this(parent, heading, description, "输入内容", confirmLabel, initialValue, submitHandler);
    }

    public TextPromptScreen(Screen parent, String heading, String description, String inputLabel, String confirmLabel, String initialValue, Consumer<String> submitHandler) {
        super(new TextComponent(heading));
        this.parent = parent;
        this.heading = heading;
        this.description = description;
        this.inputLabel = inputLabel;
        this.confirmLabel = confirmLabel;
        this.initialValue = initialValue == null ? "" : initialValue;
        this.submitHandler = submitHandler;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(380, this.width - 24);
        boolean compact = panelWidth < 360;
        int panelHeight = compact ? 194 : 146;
        panelHeight = Math.min(panelHeight, this.height - 24);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        this.inputField = new EditBox(this.font, left + 18, top + 54, panelWidth - 36, 20, new TextComponent(this.inputLabel));
        this.inputField.setMaxLength(512);
        this.inputField.setValue(this.initialValue);
        this.addRenderableWidget(this.inputField);
        this.setInitialFocus(this.inputField);

        if (compact) {
            this.addRenderableWidget(new StyledButton(left + 18, top + 90, panelWidth - 36, 20, new TextComponent(this.confirmLabel), StyledButton.Variant.PRIMARY, button -> onSubmit()));
            this.addRenderableWidget(new StyledButton(left + 18, top + 114, panelWidth - 36, 20, new TextComponent("粘贴"), StyledButton.Variant.GHOST, button -> this.inputField.setValue(PlatformServices.client().readClipboard())));
            this.addRenderableWidget(new StyledButton(left + 18, top + 138, panelWidth - 36, 20, new TextComponent("取消"), StyledButton.Variant.SECONDARY, button -> this.minecraft.setScreen(this.parent)));
        } else {
            this.addRenderableWidget(new StyledButton(left + 18, top + 90, 100, 20, new TextComponent(this.confirmLabel), StyledButton.Variant.PRIMARY, button -> onSubmit()));
            this.addRenderableWidget(new StyledButton(left + 126, top + 90, 100, 20, new TextComponent("粘贴"), StyledButton.Variant.GHOST, button -> this.inputField.setValue(PlatformServices.client().readClipboard())));
            this.addRenderableWidget(new StyledButton(left + 234, top + 90, 128, 20, new TextComponent("取消"), StyledButton.Variant.SECONDARY, button -> this.minecraft.setScreen(this.parent)));
        }
    }

    @Override
    public void tick() {
        this.inputField.tick();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.inputField.keyPressed(keyCode, scanCode, modifiers) || this.inputField.canConsumeInput()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.inputField.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        if (paths == null || paths.isEmpty()) {
            return;
        }
        this.inputField.setValue(paths.get(0).toString());
        this.status = "已填入拖入的文件路径。";
    }

    private void onSubmit() {
        try {
            this.submitHandler.accept(this.inputField.getValue().trim());
        } catch (RuntimeException exception) {
            this.status = exception.getMessage() == null ? "操作失败。" : exception.getMessage();
        }
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        fillGradient(matrixStack, 0, 0, this.width, this.height, UiPalette.BACKGROUND_TOP, UiPalette.BACKGROUND_BOTTOM);
        int panelWidth = Math.min(380, this.width - 24);
        boolean compact = panelWidth < 360;
        int panelHeight = Math.min(compact ? 194 : 146, this.height - 24);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        UiRender.drawPanel(matrixStack, left, top, panelWidth, panelHeight, UiPalette.PANEL, UiPalette.ACCENT);
        UiRender.drawSectionTitle(matrixStack, this.font, this.heading, this.description, left + 18, top + 14);
        this.font.draw(matrixStack, this.inputLabel, (float) (left + 18), (float) (top + 42), UiPalette.TEXT_MUTED);
        this.inputField.render(matrixStack, mouseX, mouseY, partialTicks);
        if (!this.status.isEmpty()) {
            this.font.draw(matrixStack, this.status, (float) (left + 18), (float) (top + panelHeight - 22), UiPalette.WARNING);
        }
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }
}
