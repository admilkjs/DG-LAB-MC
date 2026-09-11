package dglabmc.client.ui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;

public final class UiRender extends AbstractGui {
    private UiRender() {
    }

    public static void drawPanel(MatrixStack matrixStack, int x, int y, int width, int height, int backgroundColor, int accentColor) {
        fill(matrixStack, x, y, x + width, y + height, backgroundColor);
        fill(matrixStack, x, y, x + width, y + 2, accentColor);
        fill(matrixStack, x, y, x + 1, y + height, UiPalette.BORDER);
        fill(matrixStack, x + width - 1, y, x + width, y + height, UiPalette.BORDER);
        fill(matrixStack, x, y + height - 1, x + width, y + height, UiPalette.BORDER);
    }

    public static void drawSectionTitle(MatrixStack matrixStack, FontRenderer font, String title, String subtitle, int x, int y) {
        font.draw(matrixStack, title, (float) x, (float) y, UiPalette.TEXT_PRIMARY);
        if (subtitle != null && !subtitle.isEmpty()) {
            font.draw(matrixStack, subtitle, (float) x, (float) (y + 12), UiPalette.TEXT_MUTED);
        }
    }

    public static void drawStatusBadge(MatrixStack matrixStack, FontRenderer font, String text, int x, int y, int backgroundColor, int borderColor) {
        int textWidth = font.width(text);
        int width = textWidth + 12;
        fill(matrixStack, x, y, x + width, y + 14, backgroundColor);
        fill(matrixStack, x, y, x + width, y + 1, borderColor);
        fill(matrixStack, x, y + 13, x + width, y + 14, borderColor);
        fill(matrixStack, x, y, x + 1, y + 14, borderColor);
        fill(matrixStack, x + width - 1, y, x + width, y + 14, borderColor);
        font.draw(matrixStack, text, (float) (x + 6), (float) (y + 3), UiPalette.TEXT_PRIMARY);
    }

    public static void drawDivider(MatrixStack matrixStack, int x, int y, int width) {
        fill(matrixStack, x, y, x + width, y + 1, UiPalette.BORDER);
    }

    public static int drawWrappedText(MatrixStack matrixStack, FontRenderer font, String text, int x, int y, int width, int color, int maxLines) {
        return drawWrappedText(matrixStack, font, text, x, y, width, color, maxLines, 12);
    }

    public static int drawWrappedText(MatrixStack matrixStack, FontRenderer font, String text, int x, int y, int width, int color, int maxLines, int lineHeight) {
        if (text == null || text.isEmpty() || width <= 0 || maxLines <= 0) {
            return 0;
        }
        String remaining = text;
        int lines = 0;
        int drawY = y;
        while (!remaining.isEmpty() && lines < maxLines) {
            String line = font.plainSubstrByWidth(remaining, width);
            if (line.isEmpty()) {
                break;
            }
            font.draw(matrixStack, line, (float) x, (float) drawY, color);
            remaining = remaining.substring(line.length()).trim();
            drawY += lineHeight;
            lines++;
        }
        return lines;
    }

    public static int measureWrappedTextHeight(FontRenderer font, String text, int width, int maxLines) {
        return measureWrappedTextHeight(font, text, width, maxLines, 12);
    }

    public static int measureWrappedTextHeight(FontRenderer font, String text, int width, int maxLines, int lineHeight) {
        if (text == null || text.isEmpty() || width <= 0 || maxLines <= 0) {
            return 0;
        }
        String remaining = text;
        int lines = 0;
        while (!remaining.isEmpty() && lines < maxLines) {
            String line = font.plainSubstrByWidth(remaining, width);
            if (line.isEmpty()) {
                break;
            }
            remaining = remaining.substring(line.length()).trim();
            lines++;
        }
        return lines * lineHeight;
    }
}
