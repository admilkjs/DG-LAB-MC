package dglabmc.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class UiRender {
    private UiRender() {
    }

    public static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int backgroundColor, int accentColor) {
        guiGraphics.fill(x, y, x + width, y + height, backgroundColor);
        guiGraphics.fill(x, y, x + width, y + 2, accentColor);
        guiGraphics.fill(x, y, x + 1, y + height, UiPalette.BORDER);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, UiPalette.BORDER);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, UiPalette.BORDER);
    }

    public static void drawSectionTitle(GuiGraphics guiGraphics, Font font, String title, String subtitle, int x, int y) {
        guiGraphics.drawString(font, title, x, y, UiPalette.TEXT_PRIMARY);
        if (subtitle != null && !subtitle.isEmpty()) {
            guiGraphics.drawString(font, subtitle, x, y + 12, UiPalette.TEXT_MUTED);
        }
    }

    public static void drawStatusBadge(GuiGraphics guiGraphics, Font font, String text, int x, int y, int backgroundColor, int borderColor) {
        int textWidth = font.width(text);
        int width = textWidth + 12;
        guiGraphics.fill(x, y, x + width, y + 14, backgroundColor);
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + 13, x + width, y + 14, borderColor);
        guiGraphics.fill(x, y, x + 1, y + 14, borderColor);
        guiGraphics.fill(x + width - 1, y, x + width, y + 14, borderColor);
        guiGraphics.drawString(font, text, x + 6, y + 3, UiPalette.TEXT_PRIMARY);
    }

    public static void drawDivider(GuiGraphics guiGraphics, int x, int y, int width) {
        guiGraphics.fill(x, y, x + width, y + 1, UiPalette.BORDER);
    }

    public static int drawWrappedText(GuiGraphics guiGraphics, Font font, String text, int x, int y, int width, int color, int maxLines) {
        return drawWrappedText(guiGraphics, font, text, x, y, width, color, maxLines, 12);
    }

    public static int drawWrappedText(GuiGraphics guiGraphics, Font font, String text, int x, int y, int width, int color, int maxLines, int lineHeight) {
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
            guiGraphics.drawString(font, line, x, drawY, color);
            remaining = remaining.substring(line.length()).trim();
            drawY += lineHeight;
            lines++;
        }
        return lines;
    }

    public static int measureWrappedTextHeight(Font font, String text, int width, int maxLines) {
        return measureWrappedTextHeight(font, text, width, maxLines, 12);
    }

    public static int measureWrappedTextHeight(Font font, String text, int width, int maxLines, int lineHeight) {
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
