package dglabmc.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class StyledButton extends Button {
    public interface IPressable extends Button.OnPress {
    }

    public enum Variant {
        PRIMARY,
        SECONDARY,
        GHOST,
        DANGER,
        TAB_ACTIVE,
        TAB_IDLE
    }

    private final Variant variant;

    public StyledButton(int x, int y, int width, int height, Component title, IPressable onPress) {
        this(x, y, width, height, title, Variant.SECONDARY, onPress);
    }

    public StyledButton(int x, int y, int width, int height, Component title, Variant variant, IPressable onPress) {
        super(x, y, width, height, title, onPress, DEFAULT_NARRATION);
        this.variant = variant;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        int background = backgroundColor();
        int border = borderColor();
        int textColor = this.active ? UiPalette.TEXT_PRIMARY : UiPalette.TEXT_DIM;
        int x = this.getX();
        int y = this.getY();
        int width = this.getWidth();
        int height = this.getHeight();
        guiGraphics.fill(x, y, x + width, y + height, background);
        guiGraphics.fill(x, y, x + width, y + 1, border);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, border);
        guiGraphics.fill(x, y, x + 1, y + height, border);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, border);
        String label = fitLabel(minecraft, this.getMessage().getString(), width - 10);
        guiGraphics.drawCenteredString(minecraft.font, label, x + width / 2, y + (height - 8) / 2, textColor);
    }

    private int backgroundColor() {
        if (!this.active) {
            return 0x99202838;
        }
        switch (this.variant) {
            case PRIMARY:
                return this.isHoveredOrFocused() ? 0xFFF97316 : 0xFFE85D04;
            case DANGER:
                return this.isHoveredOrFocused() ? 0xFFDC2626 : 0xFF991B1B;
            case GHOST:
                return this.isHoveredOrFocused() ? 0xCC1E293B : 0x88202B39;
            case TAB_ACTIVE:
                return this.isHoveredOrFocused() ? 0xFF2A374B : 0xFF1E293B;
            case TAB_IDLE:
                return this.isHoveredOrFocused() ? 0xD9233043 : 0x99172233;
            case SECONDARY:
            default:
                return this.isHoveredOrFocused() ? 0xCC243041 : 0xB31B2635;
        }
    }

    private int borderColor() {
        if (!this.active) {
            return UiPalette.BORDER;
        }
        switch (this.variant) {
            case PRIMARY:
                return 0xFFFFEDD5;
            case DANGER:
                return 0xFFFCA5A5;
            case TAB_ACTIVE:
                return UiPalette.ACCENT;
            case TAB_IDLE:
            case GHOST:
            case SECONDARY:
            default:
                return this.isHoveredOrFocused() ? UiPalette.BORDER_STRONG : UiPalette.BORDER;
        }
    }

    private String fitLabel(Minecraft minecraft, String raw, int maxWidth) {
        if (raw == null || raw.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (minecraft.font.width(raw) <= maxWidth) {
            return raw;
        }
        String clipped = minecraft.font.plainSubstrByWidth(raw, Math.max(0, maxWidth - minecraft.font.width("...")));
        if (clipped == null || clipped.isEmpty()) {
            return "";
        }
        return clipped + "...";
    }
}
