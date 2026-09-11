package net.minecraft.client.gui.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import dglabmc.client.ui.FontAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

public class Screen extends GuiScreen {
    protected final ClientAccess minecraft = new ClientAccess();
    protected final FontAdapter font = new FontAdapter();
    public final List<Button> buttons = new ButtonList();
    public final List<Object> children = new ArrayList<Object>();
    protected final ITextComponent title;

    protected Screen(ITextComponent title) {
        this.title = title;
    }

    protected void init() {
    }

    @Override
    public void initGui() {
        this.minecraft.bind(this.mc);
        this.font.bind(this.fontRenderer);
        this.buttonList.clear();
        this.children.clear();
        this.init();
    }

    public void tick() {
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.minecraft.bind(this.mc);
        this.font.bind(this.fontRenderer);
        this.tick();
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.minecraft.bind(this.mc);
        this.font.bind(this.fontRenderer);
        this.render(new MatrixStack(), mouseX, mouseY, partialTicks);
    }

    protected <T extends GuiButton> T addButton(T button) {
        this.buttonList.add(button);
        return button;
    }

    protected void clearWidgets() {
        this.buttonList.clear();
        this.children.clear();
    }

    protected void setInitialFocus(Object child) {
        if (child instanceof TextFieldWidget) {
            ((TextFieldWidget) child).setFocus(true);
        }
    }

    protected void renderBackground(MatrixStack matrixStack) {
        this.drawDefaultBackground();
    }

    protected void fillGradient(MatrixStack matrixStack, int left, int top, int right, int bottom, int startColor, int endColor) {
        this.drawGradientRect(left, top, right, bottom, startColor, endColor);
    }

    protected void fill(MatrixStack matrixStack, int left, int top, int right, int bottom, int color) {
        GuiScreen.drawRect(left, top, right, bottom, color);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 1) {
            this.onClose();
            return true;
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (!this.keyPressed(keyCode, 0, 0) && !this.charTyped(typedChar, 0)) {
            super.keyTyped(typedChar, keyCode);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        try {
            super.mouseClicked((int) mouseX, (int) mouseY, button);
        } catch (IOException ignored) {
        }
        return true;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.mouseClicked((double) mouseX, (double) mouseY, mouseButton);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        this.mouseDragged((double) mouseX, (double) mouseY, clickedMouseButton, 0.0D, 0.0D);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        super.mouseReleased((int) mouseX, (int) mouseY, button);
        return true;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        this.mouseReleased((double) mouseX, (double) mouseY, state);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            this.mouseScrolled(mouseX, mouseY, wheel > 0 ? 1.0D : -1.0D);
        }
    }

    public void onClose() {
        this.minecraft.setScreen((GuiScreen) null);
    }

    public void onFilesDrop(List<Path> paths) {
    }

    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return this.isPauseScreen();
    }

    public static final class ClientAccess {
        private Minecraft delegate;

        private void bind(Minecraft minecraft) {
            this.delegate = minecraft;
        }

        public void setScreen(Screen screen) {
            if (this.delegate != null) {
                this.delegate.displayGuiScreen(screen);
            }
        }

        public void setScreen(GuiScreen screen) {
            if (this.delegate != null) {
                this.delegate.displayGuiScreen(screen);
            }
        }
    }

    private final class ButtonList extends AbstractList<Button> {
        @Override
        public Button get(int index) {
            return (Button) Screen.this.buttonList.get(index);
        }

        @Override
        public int size() {
            return Screen.this.buttonList.size();
        }

        @Override
        public Button set(int index, Button element) {
            return (Button) Screen.this.buttonList.set(index, element);
        }

        @Override
        public void add(int index, Button element) {
            Screen.this.buttonList.add(index, element);
        }

        @Override
        public Button remove(int index) {
            return (Button) Screen.this.buttonList.remove(index);
        }

        @Override
        public void clear() {
            Screen.this.buttonList.clear();
        }
    }
}
