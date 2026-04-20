package dglabmc.client.ui;

import dglabmc.client.ClientHooks;

import dglabmc.security.DailyPasswordLock;
import dglabmc.platform.PlatformServices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class PasswordGateScreen extends Screen {
    private final Screen nextScreen;

    private EditBox passwordField;
    private String status = "";`r`n    private boolean suppressInitialChar;

    public PasswordGateScreen(Screen nextScreen) {
        super(Component.literal("杈撳叆瀵嗙爜"));
        this.nextScreen = nextScreen;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.suppressInitialChar = ClientHooks.consumePendingScreenCharSuppression();`r`n
        int panelWidth = Math.min(360, this.width - 24);
        int panelHeight = 148;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        this.passwordField = new EditBox(this.font, left + 18, top + 54, panelWidth - 36, 20, Component.literal("浠婃棩瀵嗙爜"));
        this.passwordField.setMaxLength(64);
        this.passwordField.setValue("");
        this.addRenderableWidget(this.passwordField);
        this.setInitialFocus(this.passwordField);

        int buttonWidth = (panelWidth - 44) / 2;
        this.addRenderableWidget(new StyledButton(left + 18, top + 90, buttonWidth, 20, Component.literal("瑙ｉ攣"), StyledButton.Variant.PRIMARY, button -> submitPassword()));
        this.addRenderableWidget(new StyledButton(left + 26 + buttonWidth, top + 90, buttonWidth, 20, Component.literal("绮樿创"), StyledButton.Variant.GHOST, button -> this.passwordField.setValue(PlatformServices.client().readClipboard())));
    }

    @Override
    public void tick() {
        if (this.passwordField != null) {
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

    @Override`r`n    public boolean charTyped(char codePoint, int modifiers) {`r`n        if (this.suppressInitialChar && this.passwordField != null && this.passwordField.getValue().isEmpty()) {`r`n            this.suppressInitialChar = false;`r`n            return true;`r`n        }`r`n        this.suppressInitialChar = false;`r`n        return this.passwordField != null && (this.passwordField.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers));`r`n    }

    private void submitPassword() {
        if (DailyPasswordLock.unlock(this.passwordField == null ? "" : this.passwordField.getValue().trim())) {
            this.status = "";
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.nextScreen);
            }
            return;
        }
        this.status = "瀵嗙爜涓嶅";
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.fillGradient( 0, 0, this.width, this.height, UiPalette.BACKGROUND_TOP, UiPalette.BACKGROUND_BOTTOM);
        int panelWidth = Math.min(360, this.width - 24);
        int panelHeight = 148;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        UiRender.drawPanel(guiGraphics, left, top, panelWidth, panelHeight, UiPalette.PANEL, UiPalette.ACCENT);
        UiRender.drawSectionTitle(guiGraphics, this.font, "杈撳叆浠婃棩瀵嗙爜", "鏈В閿佸墠涓嶈兘浣跨敤鐣岄潰鍜屾寚浠?, left + 18, top + 14);
        guiGraphics.drawString(this.font, "瀵嗙爜", (left + 18), (top + 42), UiPalette.TEXT_MUTED);
        if (this.passwordField != null) {
            this.passwordField.render(guiGraphics, mouseX, mouseY, partialTicks);
        }
        if (!this.status.isEmpty()) {
            guiGraphics.drawString(this.font, this.status, (left + 18), (top + panelHeight - 22), UiPalette.WARNING);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }
}



