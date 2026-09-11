package net.minecraft.client.gui.widget.button;

import net.minecraft.client.gui.GuiButton;

public class Button extends GuiButton {
    public interface IPressable {
        void onPress(Button button);
    }

    protected final IPressable onPress;
    public boolean active = true;

    public Button(int x, int y, int width, int height, String message, IPressable onPress) {
        super(0, x, y, width, height, message == null ? "" : message);
        this.onPress = onPress;
    }

    public String getMessage() {
        return this.displayString;
    }

    public void setMessage(String message) {
        this.displayString = message == null ? "" : message;
    }

    public boolean isHovered() {
        return this.hovered;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.enabled = this.active;
        this.visible = true;
        super.render(mouseX, mouseY, partialTicks);
    }

    public void renderButton(int mouseX, int mouseY, float partialTicks) {
        super.render(mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (this.onPress != null && this.active) {
            this.onPress.onPress(this);
        }
    }
}
