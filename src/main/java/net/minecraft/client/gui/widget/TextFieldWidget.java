package net.minecraft.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import dglabmc.client.ui.FontAdapter;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.text.ITextComponent;

public class TextFieldWidget extends GuiTextField {
    public TextFieldWidget(FontAdapter font, int x, int y, int width, int height, ITextComponent message) {
        super(0, font == null ? null : font.raw(), x, y, width, height);
    }

    public void setValue(String value) {
        this.setText(value == null ? "" : value);
    }

    public void setMaxLength(int length) {
        this.setMaxStringLength(length);
    }

    public String getValue() {
        return this.getText();
    }

    public boolean canConsumeInput() {
        return this.isFocused();
    }

    public void setFocus(boolean focused) {
        this.setFocused(focused);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.textboxKeyTyped('\0', keyCode);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return this.textboxKeyTyped(codePoint, 0);
    }

    public void tick() {
        this.updateCursorCounter();
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.drawTextBox();
    }
}
