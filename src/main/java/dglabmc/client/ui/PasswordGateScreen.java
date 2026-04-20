package dglabmc.client.ui;

import dglabmc.client.ClientHooks;

import dglabmc.security.DailyPasswordLock;
import dglabmc.platform.PlatformServices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class PasswordGateScreen extends Screen {
    private static final String TITLE = "输入密码";
    private static final String TODAY_PASSWORD = "今日密码";
    private static final String UNLOCK = "解锁";
    private static final String PASTE = "粘贴";
    private static final String PASSWORD_ERROR = "密码不对";
    private static final String PASSWORD_LABEL = "密码";
    private static final String SCREEN_TITLE = "输入今日密码";
    private static final String SCREEN_SUBTITLE = "解锁后可用界面和指令";

    private final Screen nextScreen;

    private EditBox passwordField;
    private String status = "";
    private boolean suppressInitialChar;

    public PasswordGateScreen(Screen nextScreen) {
        super(Component.literal(TITLE));
        this.nextScreen = nextScreen;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.suppressInitialChar = ClientHooks.consumePendingScreenCharSuppression();

        int panelWidth = Math.min(360, this.width - 24);
        int panelHeight = 148;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;

        this.passwordField = new EditBox(this.font, left + 18, top + 54, panelWidth - 36, 20, Component.literal(TODAY_PASSWORD));
        this.passwordField.setMaxLength(64);
        this.passwordField.setValue("");
        this.addRenderableWidget(this.passwordField);
        this.setInitialFocus(this.passwordField);

        int buttonWidth = (panelWidth - 44) / 2;
        this.addRenderableWidget(new StyledButton(left + 18, top + 90, buttonWidth, 20, Component.literal(UNLOCK), StyledButton.Variant.PRIMARY, button -> submitPassword()));
        this.addRenderableWidget(new StyledButton(left + 26 + buttonWidth, top + 90, buttonWidth, 20, Component.literal(PASTE), StyledButton.Variant.GHOST, button -> this.passwordField.setValue(PlatformServices.client().readClipboard())));
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
        this.status = PASSWORD_ERROR;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        guiGraphics.fillGradient( 0, 0, this.width, this.height, UiPalette.BACKGROUND_TOP, UiPalette.BACKGROUND_BOTTOM);
        int panelWidth = Math.min(360, this.width - 24);
        int panelHeight = 148;
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        UiRender.drawPanel(guiGraphics, left, top, panelWidth, panelHeight, UiPalette.PANEL, UiPalette.ACCENT);
        UiRender.drawSectionTitle(guiGraphics, this.font, SCREEN_TITLE, SCREEN_SUBTITLE, left + 18, top + 14);
        guiGraphics.drawString(this.font, PASSWORD_LABEL, (left + 18), (top + 42), UiPalette.TEXT_MUTED);
        if (this.passwordField != null) {
            this.passwordField.render(guiGraphics, mouseX, mouseY, partialTicks);
        }
        if (!this.status.isEmpty()) {
            guiGraphics.drawString(this.font, this.status, (left + 18), (top + panelHeight - 22), UiPalette.WARNING);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }
}



