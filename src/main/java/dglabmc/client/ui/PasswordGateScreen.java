package dglabmc.client.ui;

import dglabmc.client.ClientHooks;
import dglabmc.platform.PlatformServices;
import dglabmc.security.DailyPasswordLock;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.StringTextComponent;

public class PasswordGateScreen extends BaseScreen {
    private final net.minecraft.client.gui.screen.Screen nextScreen;

    private TextFieldWidget passwordField;
    private boolean suppressInitialChar;

    public PasswordGateScreen(net.minecraft.client.gui.screen.Screen nextScreen) {
        super(new StringTextComponent("输入密码"), null);
        this.nextScreen = nextScreen;
    }

    @Override protected int maxPanelWidth() { return 360; }
    @Override protected int compactThreshold() { return 0; }
    @Override protected int panelHeightNormal() { return 148; }

    @Override
    protected void buildWidgets() {
        this.suppressInitialChar = ClientHooks.consumePendingScreenCharSuppression();

        int il = innerLeft();
        int iw = innerWidth();
        this.passwordField = new TextFieldWidget(this.font, il, this.panelTop + 54, iw, UiConstants.BTN_HEIGHT, new StringTextComponent("今日密码"));
        this.passwordField.setMaxLength(64);
        this.passwordField.setValue("");
        this.passwordField.setFormatter((text, pos) -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                sb.append('*');
            }
            return new StringTextComponent(sb.toString()).getVisualOrderText();
        });
        this.children.add(this.passwordField);
        this.setInitialFocus(this.passwordField);

        int buttonWidth = (iw - UiConstants.PAD_SM) / 2;
        this.addButton(new StyledButton(il, this.panelTop + 90, buttonWidth, UiConstants.BTN_HEIGHT, new StringTextComponent("解锁"), StyledButton.Variant.PRIMARY, button -> submitPassword()));
        this.addButton(new StyledButton(il + buttonWidth + UiConstants.PAD_SM, this.panelTop + 90, buttonWidth, UiConstants.BTN_HEIGHT, new StringTextComponent("粘贴"), StyledButton.Variant.GHOST, button -> this.passwordField.setValue(PlatformServices.client().readClipboard())));
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
            setStatus("");
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.nextScreen);
            }
            return;
        }
        setStatus("密码不对");
    }

    @Override
    public void onClose() {
    }

    @Override
    protected void renderContent(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        int il = innerLeft();
        UiRender.drawSectionTitle(matrixStack, this.font, "输入今日密码", "未解锁前不能使用界面和指令", il, this.panelTop + 14);
        this.font.draw(matrixStack, "密码", (float) il, (float) (this.panelTop + 42), UiPalette.TEXT_MUTED);
        if (this.passwordField != null) {
            this.passwordField.render(matrixStack, mouseX, mouseY, partialTicks);
        }
    }
}
