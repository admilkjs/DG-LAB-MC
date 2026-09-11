package dglabmc.client.ui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;

public final class UiRender extends AbstractGui {
    private UiRender() {
    }

    public static void drawPanel(MatrixStack matrixStack, int x, int y, int width, int height, int backgroundColor, int accentColor) {
        fill(x + 1, y + height, x + width + 1, y + height + 1, 0x33000000);
        fill(x + width, y + 1, x + width + 1, y + height + 1, 0x33000000);
        fill(x, y, x + width, y + height, backgroundColor);
        fill(x, y, x + width, y + 2, accentColor);
        fill(x, y, x + 1, y + height, UiPalette.BORDER);
        fill(x + width - 1, y, x + width, y + height, UiPalette.BORDER);
        fill(x, y + height - 1, x + width, y + height, UiPalette.BORDER);
    }

    public static void drawSectionTitle(MatrixStack matrixStack, FontRenderer font, String title, String subtitle, int x, int y) {
        font.drawString(title, (float) x, (float) y, UiPalette.TEXT_PRIMARY);
        if (subtitle != null && !subtitle.isEmpty()) {
            font.drawString(subtitle, (float) x, (float) (y + UiConstants.LINE_HEIGHT), UiPalette.TEXT_MUTED);
        }
    }

    public static void drawStatusBadge(MatrixStack matrixStack, FontRenderer font, String text, int x, int y, int backgroundColor, int borderColor) {
        int textWidth = font.getStringWidth(text);
        int width = textWidth + UiConstants.BADGE_PAD * 2;
        fill(x, y, x + width, y + UiConstants.BADGE_HEIGHT, backgroundColor);
        fill(x, y, x + width, y + 1, borderColor);
        fill(x, y + UiConstants.BADGE_HEIGHT - 1, x + width, y + UiConstants.BADGE_HEIGHT, borderColor);
        fill(x, y, x + 1, y + UiConstants.BADGE_HEIGHT, borderColor);
        fill(x + width - 1, y, x + width, y + UiConstants.BADGE_HEIGHT, borderColor);
        font.drawString(text, (float) (x + UiConstants.BADGE_PAD), (float) (y + 3), UiPalette.TEXT_PRIMARY);
    }

    public static void drawDivider(MatrixStack matrixStack, int x, int y, int width) {
        fill(x, y, x + width, y + 1, UiPalette.BORDER);
    }

    public static int drawWrappedText(MatrixStack matrixStack, FontRenderer font, String text, int x, int y, int width, int color, int maxLines) {
        return drawWrappedText(matrixStack, font, text, x, y, width, color, maxLines, UiConstants.LINE_HEIGHT);
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
        return measureWrappedTextHeight(font, text, width, maxLines, UiConstants.LINE_HEIGHT);
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

    public static void drawScrollIndicator(MatrixStack matrixStack, int x, int y, int height, int viewSize, int totalSize, int scrollOffset) {
        if (totalSize <= viewSize || height <= 0) {
            return;
        }
        int trackWidth = 2;
        fill(x, y, x + trackWidth, y + height, UiPalette.BORDER);
        int thumbHeight = Math.max(8, height * viewSize / totalSize);
        int maxScroll = totalSize - viewSize;
        int thumbOffset = maxScroll > 0 ? (height - thumbHeight) * scrollOffset / maxScroll : 0;
        fill(x, y + thumbOffset, x + trackWidth, y + thumbOffset + thumbHeight, UiPalette.BORDER_STRONG);
    }
}
