package dglabmc.client.ui;

import dglabmc.client.ClientHooks;

import dglabmc.security.DailyPasswordLock;
import dglabmc.platform.PlatformServices;
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
        super(new StringTextComponent("杈撳叆瀵嗙爜"));
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

        this.passwordField = new TextFieldWidget(this.font, left + 18, top + 54, panelWidth - 36, 20, "浠婃棩瀵嗙爜");
        this.passwordField.setMaxStringLength(64);
        this.passwordField.setText("");
        this.children.add(this.passwordField);
        this.passwordField.setFocused2(true);

        int buttonWidth = (panelWidth - 44) / 2;
        this.addButton(new StyledButton(left + 18, top + 90, buttonWidth, 20, new StringTextComponent("瑙ｉ攣"), StyledButton.Variant.PRIMARY, button -> submitPassword()));
        this.addButton(new StyledButton(left + 26 + buttonWidth, top + 90, buttonWidth, 20, new StringTextComponent("绮樿创"), StyledButton.Variant.GHOST, button -> this.passwordField.setText(PlatformServices.client().readClipboard())));
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
        if (this.passwordField != null && this.passwordField.keyPressed(keyCode, scanCode, modifiers)) {
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
        if (DailyPasswordLock.unlock(this.passwordField == null ? "" : this.passwordField.getText().trim())) {
            this.status = "";
            if (this.minecraft != null) {
                this.minecraft.displayGuiScreen(this.nextScreen);
            }
            return;
        }
        this.status = "瀵嗙爜涓嶅";
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        MatrixStack matrixStack = new MatrixStack();
        this.renderBackground();
        fillGradient(0, 0, this.width, this.height, UiPalette.BACKGROUND_TOP, UiPalette.BACKGROUND_BOTTOM);
        int panelWidth = Math.min(360, this.width - 24);
        int panelHeight = 148;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        UiRender.drawPanel(matrixStack, left, top, panelWidth, panelHeight, UiPalette.PANEL, UiPalette.ACCENT);
        UiRender.drawSectionTitle(matrixStack, this.font, "杈撳叆浠婃棩瀵嗙爜", "鏈В閿佸墠涓嶈兘浣跨敤鐣岄潰鍜屾寚浠?, left + 18, top + 14);
        this.font.drawString("瀵嗙爜", (float) (left + 18), (float) (top + 42), UiPalette.TEXT_MUTED);
        if (this.passwordField != null) {
            this.passwordField.render(mouseX, mouseY, partialTicks);
        }
        if (!this.status.isEmpty()) {
            this.font.drawString(this.status, (float) (left + 18), (float) (top + panelHeight - 22), UiPalette.WARNING);
        }
        super.render(mouseX, mouseY, partialTicks);
    }
}

