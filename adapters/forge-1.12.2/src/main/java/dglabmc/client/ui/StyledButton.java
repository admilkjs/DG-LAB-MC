package dglabmc.client.ui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.ITextComponent;

public class StyledButton extends Button {
    public enum Variant {
        PRIMARY,
        SECONDARY,
        GHOST,
        DANGER,
        TAB_ACTIVE,
        TAB_IDLE
    }

    private final Variant variant;

    public StyledButton(int x, int y, int width, int height, ITextComponent title, IPressable onPress) {
        this(x, y, width, height, title, Variant.SECONDARY, onPress);
    }

    public StyledButton(int x, int y, int width, int height, ITextComponent title, Variant variant, IPressable onPress) {
        super(x, y, width, height, title, onPress);
        this.variant = variant;
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getMinecraft();
        FontAdapter font = new FontAdapter(minecraft.fontRenderer);
        int background = backgroundColor();
        int border = borderColor();
        int textColor = this.active ? UiPalette.TEXT_PRIMARY : UiPalette.TEXT_DIM;
        AbstractGui.fill(matrixStack, this.x, this.y, this.x + this.width, this.y + this.height, background);
        AbstractGui.fill(matrixStack, this.x, this.y, this.x + this.width, this.y + 1, border);
        AbstractGui.fill(matrixStack, this.x, this.y + this.height - 1, this.x + this.width, this.y + this.height, border);
        AbstractGui.fill(matrixStack, this.x, this.y, this.x + 1, this.y + this.height, border);
        AbstractGui.fill(matrixStack, this.x + this.width - 1, this.y, this.x + this.width, this.y + this.height, border);
        String label = fitLabel(font, this.getMessage().getString(), this.width - 10);
        AbstractGui.drawCenteredString(matrixStack, minecraft.fontRenderer, label, this.x + this.width / 2, this.y + (this.height - 8) / 2, textColor);
    }

    private int backgroundColor() {
        if (!this.active) {
            return 0x99202838;
        }
        switch (this.variant) {
            case PRIMARY:
                return this.isHovered() ? 0xFFF97316 : 0xFFE85D04;
            case DANGER:
                return this.isHovered() ? 0xFFDC2626 : 0xFF991B1B;
            case GHOST:
                return this.isHovered() ? 0xCC1E293B : 0x88202B39;
            case TAB_ACTIVE:
                return this.isHovered() ? 0xFF2A374B : 0xFF1E293B;
            case TAB_IDLE:
                return this.isHovered() ? 0xD9233043 : 0x99172233;
            case SECONDARY:
            default:
                return this.isHovered() ? 0xCC243041 : 0xB31B2635;
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
                return this.isHovered() ? UiPalette.BORDER_STRONG : UiPalette.BORDER;
        }
    }

    private String fitLabel(FontAdapter font, String raw, int maxWidth) {
        if (raw == null || raw.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (font.width(raw) <= maxWidth) {
            return raw;
        }
        String clipped = font.plainSubstrByWidth(raw, Math.max(0, maxWidth - font.width("...")));
        if (clipped == null || clipped.isEmpty()) {
            return "";
        }
        return clipped + "...";
    }
}
