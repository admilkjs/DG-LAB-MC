package dglabmc.client.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
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
        super(x, y, width, height, title, onPress);
        this.variant = variant;
    }

    @Override
    public void renderButton(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        int background = backgroundColor();
        int border = borderColor();
        int textColor = this.active ? UiPalette.TEXT_PRIMARY : UiPalette.TEXT_DIM;
        GuiComponent.fill(matrixStack, this.x, this.y, this.x + this.width, this.y + this.height, background);
        GuiComponent.fill(matrixStack, this.x, this.y, this.x + this.width, this.y + 1, border);
        GuiComponent.fill(matrixStack, this.x, this.y + this.height - 1, this.x + this.width, this.y + this.height, border);
        GuiComponent.fill(matrixStack, this.x, this.y, this.x + 1, this.y + this.height, border);
        GuiComponent.fill(matrixStack, this.x + this.width - 1, this.y, this.x + this.width, this.y + this.height, border);
        String label = fitLabel(minecraft, this.getMessage().getString(), this.width - 10);
        drawCenteredString(matrixStack, minecraft.font, label, this.x + this.width / 2, this.y + (this.height - 8) / 2, textColor);
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
