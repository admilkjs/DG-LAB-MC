package dglabmc.client.ui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.FontRenderer;

import java.util.Collections;
import java.util.List;

public final class FontAdapter {
    private FontRenderer delegate;

    public FontAdapter() {
    }

    public FontAdapter(FontRenderer delegate) {
        this.delegate = delegate;
    }

    public FontAdapter bind(FontRenderer fontRenderer) {
        this.delegate = fontRenderer;
        return this;
    }

    public FontRenderer raw() {
        return this.delegate;
    }

    public int width(String text) {
        return this.delegate == null || text == null ? 0 : this.delegate.getStringWidth(text);
    }

    public void draw(MatrixStack matrixStack, String text, float x, float y, int color) {
        if (this.delegate != null && text != null) {
            this.delegate.drawString(text, (int) x, (int) y, color);
        }
    }

    public String plainSubstrByWidth(String text, int width) {
        if (this.delegate == null || text == null) {
            return "";
        }
        return this.delegate.trimStringToWidth(text, Math.max(0, width));
    }

    public List<String> wrap(String text, int width) {
        if (this.delegate == null || text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        return this.delegate.listFormattedStringToWidth(text, Math.max(1, width));
    }
}
