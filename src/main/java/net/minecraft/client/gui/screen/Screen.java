package net.minecraft.client.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;

public abstract class Screen extends GuiScreen {
    protected final ITextComponent title;
    protected Minecraft minecraft;
    protected FontRenderer font;

    protected Screen(ITextComponent title) {
        super();
        this.title = title;
    }

    public ITextComponent getTitle() {
        return this.title;
    }

    protected void init() {
    }

    @Override
    protected void initGui() {
        this.minecraft = this.mc;
        this.font = this.fontRenderer;
        init();
    }

    public void renderBackground() {
        this.drawDefaultBackground();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return super.mouseScrolled(delta);
    }

    public void onClose() {
        this.close();
    }

    public boolean isPauseScreen() {
        return this.doesGuiPauseGame();
    }

    protected void fill(int left, int top, int right, int bottom, int color) {
        net.minecraft.client.gui.AbstractGui.fill(left, top, right, bottom, color);
    }

    protected void fillGradient(int left, int top, int right, int bottom, int startColor, int endColor) {
        this.drawGradientRect(left, top, right, bottom, startColor, endColor);
    }
}
