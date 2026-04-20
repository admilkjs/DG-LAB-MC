package dglabmc.client.ui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Matrix4f;

public final class UiRender extends AbstractGui {
    private UiRender() {
    }

    public static void drawPanel(MatrixStack matrixStack, int x, int y, int width, int height, int backgroundColor, int accentColor) {
        Matrix4f matrix = matrixStack.getLast().getMatrix();
        fill(matrix, x, y, x + width, y + height, backgroundColor);
        fill(matrix, x, y, x + width, y + 2, accentColor);
        fill(matrix, x, y, x + 1, y + height, UiPalette.BORDER);
        fill(matrix, x + width - 1, y, x + width, y + height, UiPalette.BORDER);
        fill(matrix, x, y + height - 1, x + width, y + height, UiPalette.BORDER);
    }

    public static void drawSectionTitle(MatrixStack matrixStack, FontRenderer font, String title, String subtitle, int x, int y) {
        font.drawString(title, (float) x, (float) y, UiPalette.TEXT_PRIMARY);
        if (subtitle != null && !subtitle.isEmpty()) {
            font.drawString(subtitle, (float) x, (float) (y + 12), UiPalette.TEXT_MUTED);
        }
    }

    public static void drawStatusBadge(MatrixStack matrixStack, FontRenderer font, String text, int x, int y, int backgroundColor, int borderColor) {
        int textWidth = font.getStringWidth(text);
        int width = textWidth + 12;
        Matrix4f matrix = matrixStack.getLast().getMatrix();
        fill(matrix, x, y, x + width, y + 14, backgroundColor);
        fill(matrix, x, y, x + width, y + 1, borderColor);
        fill(matrix, x, y + 13, x + width, y + 14, borderColor);
        fill(matrix, x, y, x + 1, y + 14, borderColor);
        fill(matrix, x + width - 1, y, x + width, y + 14, borderColor);
        font.drawString(text, (float) (x + 6), (float) (y + 3), UiPalette.TEXT_PRIMARY);
    }

    public static void drawDivider(MatrixStack matrixStack, int x, int y, int width) {
        fill(matrixStack.getLast().getMatrix(), x, y, x + width, y + 1, UiPalette.BORDER);
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
            String line = font.trimStringToWidth(remaining, width);
            if (line.isEmpty()) {
                break;
            }
            font.drawString(line, (float) x, (float) drawY, color);
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
            String line = font.trimStringToWidth(remaining, width);
            if (line.isEmpty()) {
                break;
            }
            remaining = remaining.substring(line.length()).trim();
            lines++;
        }
        return lines * lineHeight;
    }
}
