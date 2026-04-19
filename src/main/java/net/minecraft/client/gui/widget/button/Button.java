package net.minecraft.client.gui.widget.button;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class Button extends GuiButton {
    protected final IPressable onPress;
    protected StringTextComponent message;
    public boolean active = true;

    public Button(int x, int y, int width, int height, ITextComponent title, IPressable onPress) {
        super(0, x, y, width, height, title == null ? "" : title.getUnformattedText());
        this.onPress = onPress;
        this.setMessage(title);
    }

    public StringTextComponent getMessage() {
        return this.message;
    }

    public void setMessage(ITextComponent title) {
        if (title instanceof StringTextComponent) {
            this.message = (StringTextComponent) title;
        } else {
            this.message = new StringTextComponent(title == null ? "" : title.getUnformattedText());
        }
        this.displayString = this.message.getString();
    }

    public boolean isHovered() {
        return this.hovered;
    }

    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, partialTicks);
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
        this.enabled = this.active;
        this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
        if (this.visible) {
            renderButton(new MatrixStack(), mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public boolean mousePressed(Minecraft minecraft, int mouseX, int mouseY) {
        boolean pressed = super.mousePressed(minecraft, mouseX, mouseY);
        if (pressed && this.active && this.onPress != null) {
            this.onPress.onPress(this);
        }
        return pressed;
    }

    public interface IPressable {
        void onPress(Button button);
    }
}
