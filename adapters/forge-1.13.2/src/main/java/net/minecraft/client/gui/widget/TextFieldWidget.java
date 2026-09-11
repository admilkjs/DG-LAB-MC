package net.minecraft.client.gui.widget;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiTextField;

public class TextFieldWidget extends GuiTextField {
    public TextFieldWidget(FontRenderer font, int x, int y, int width, int height, String message) {
        super(0, font, x, y, width, height);
        setText(message == null ? "" : message);
    }

    public void tick() {
    }

    public void setText(String value) {
        super.setText(value == null ? "" : value);
    }

    public String getText() {
        return super.getText();
    }

    public void setFocused2(boolean focused) {
        this.setFocused(focused);
    }

    public void setMaxStringLength(int length) {
        super.setMaxStringLength(length);
    }

    public void render(int mouseX, int mouseY, float partialTicks) {
        this.drawTextField(mouseX, mouseY, partialTicks);
    }
}
