package net.minecraft.client.gui;

public class AbstractGui extends Gui {
    public static void fill(int left, int top, int right, int bottom, int color) {
        drawRect(left, top, right, bottom, color);
    }
}
