package net.minecraft.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;

public class AbstractGui extends Gui {
    public static void fill(MatrixStack matrixStack, int left, int top, int right, int bottom, int color) {
        Gui.drawRect(left, top, right, bottom, color);
    }

    public static void drawCenteredString(MatrixStack matrixStack, FontRenderer fontRenderer, String text, int x, int y, int color) {
        if (fontRenderer != null && text != null) {
            fontRenderer.drawString(text, x - (fontRenderer.getStringWidth(text) / 2), y, color);
        }
    }
}
