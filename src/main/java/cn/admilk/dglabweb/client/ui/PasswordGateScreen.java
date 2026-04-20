package cn.admilk.dglabweb.client.ui;

import cn.admilk.dglabweb.client.ClientHooks;
import cn.admilk.dglabweb.platform.PlatformServices;
import cn.admilk.dglabweb.security.DailyPasswordLock;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.StringTextComponent;

public class PasswordGateScreen extends Screen {
    private final Screen nextScreen;

    private TextFieldWidget passwordField;
    private String status = "";
    private boolean suppressInitialChar;

    public PasswordGateScreen(Screen nextScreen) {
        super(new StringTextComponent("输入密码"));
        this.nextScreen = nextScreen;
    }

    @Override
    protected void init() {
        this.buttons.clear();
        this.children.clear();
        this.suppressInitialChar = ClientHooks.consumePendingScreenCharSuppression();

        int panelWidth = Math.min(360, this.width - 24);
        int panelHeight = 148;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        this.passwordField = new TextFieldWidget(this.font, left + 18, top + 54, panelWidth - 36, 20, new StringTextComponent("今日密码"));
        this.passwordField.setMaxLength(64);
        this.passwordField.setValue("");
        this.children.add(this.passwordField);
        this.setInitialFocus(this.passwordField);

        int buttonWidth = (panelWidth - 44) / 2;
        this.addButton(new StyledButton(left + 18, top + 90, buttonWidth, 20, new StringTextComponent("解锁"), StyledButton.Variant.PRIMARY, button -> submitPassword()));
        this.addButton(new StyledButton(left + 26 + buttonWidth, top + 90, buttonWidth, 20, new StringTextComponent("粘贴"), StyledButton.Variant.GHOST, button -> this.passwordField.setValue(PlatformServices.client().readClipboard())));
    }

    @Override
    public void tick() {
        if (this.passwordField != null) {
            this.passwordField.tick();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            submitPassword();
            return true;
        }
        if (this.passwordField != null && (this.passwordField.keyPressed(keyCode, scanCode, modifiers) || this.passwordField.canConsumeInput())) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.suppressInitialChar && this.passwordField != null && this.passwordField.getValue().isEmpty()) {
            this.suppressInitialChar = false;
            return true;
        }
        this.suppressInitialChar = false;
        return this.passwordField != null && (this.passwordField.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers));
    }

    private void submitPassword() {
        if (DailyPasswordLock.unlock(this.passwordField == null ? "" : this.passwordField.getValue().trim())) {
            this.status = "";
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.nextScreen);
            }
            return;
        }
        this.status = "密码不对";
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        fillGradient(matrixStack, 0, 0, this.width, this.height, UiPalette.BACKGROUND_TOP, UiPalette.BACKGROUND_BOTTOM);
        int panelWidth = Math.min(360, this.width - 24);
        int panelHeight = 148;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        UiRender.drawPanel(matrixStack, left, top, panelWidth, panelHeight, UiPalette.PANEL, UiPalette.ACCENT);
        UiRender.drawSectionTitle(matrixStack, this.font, "输入今日密码", "未解锁前不能使用界面和指令", left + 18, top + 14);
        this.font.draw(matrixStack, "密码", (float) (left + 18), (float) (top + 42), UiPalette.TEXT_MUTED);
        if (this.passwordField != null) {
            this.passwordField.render(matrixStack, mouseX, mouseY, partialTicks);
        }
        if (!this.status.isEmpty()) {
            this.font.draw(matrixStack, this.status, (float) (left + 18), (float) (top + panelHeight - 22), UiPalette.WARNING);
        }
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }
}
