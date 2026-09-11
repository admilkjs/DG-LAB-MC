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
    private float hoverProgress;

    public StyledButton(int x, int y, int width, int height, ITextComponent title, IPressable onPress) {
        this(x, y, width, height, title, Variant.SECONDARY, onPress);
    }

    public StyledButton(int x, int y, int width, int height, ITextComponent title, Variant variant, IPressable onPress) {
        super(x, y, width, height, title.getString(), onPress);
        this.variant = variant;
    }

    @Override
    public void renderButton(int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();

        float target = this.isHovered() && this.active ? 1.0F : 0.0F;
        float speed = 0.15F;
        this.hoverProgress += (target - this.hoverProgress) * speed;
        if (Math.abs(this.hoverProgress - target) < 0.01F) {
            this.hoverProgress = target;
        }

        int background = backgroundColor();
        int border = borderColor();
        int textColor = this.active ? UiPalette.TEXT_PRIMARY : UiPalette.TEXT_DIM;

        AbstractGui.fill(this.x, this.y, this.x + this.width, this.y + this.height, background);
        AbstractGui.fill(this.x, this.y, this.x + this.width, this.y + 1, border);
        AbstractGui.fill(this.x, this.y + this.height - 1, this.x + this.width, this.y + this.height, border);
        AbstractGui.fill(this.x, this.y, this.x + 1, this.y + this.height, border);
        AbstractGui.fill(this.x + this.width - 1, this.y, this.x + this.width, this.y + this.height, border);

        String label = fitLabel(minecraft, this.getMessage(), this.width - 10);
        drawCenteredString(minecraft.fontRenderer, label, this.x + this.width / 2, this.y + (this.height - 8) / 2, textColor);
    }

    private int backgroundColor() {
        if (!this.active) {
            return UiPalette.BTN_DISABLED_BG;
        }
        switch (this.variant) {
            case PRIMARY:
                return UiUtil.lerpColor(UiPalette.BTN_PRIMARY_IDLE, UiPalette.BTN_PRIMARY_HOVER, this.hoverProgress);
            case DANGER:
                return UiUtil.lerpColor(UiPalette.BTN_DANGER_IDLE, UiPalette.BTN_DANGER_HOVER, this.hoverProgress);
            case GHOST:
                return UiUtil.lerpColor(UiPalette.BTN_GHOST_IDLE, UiPalette.BTN_GHOST_HOVER, this.hoverProgress);
            case TAB_ACTIVE:
                return UiUtil.lerpColor(UiPalette.BTN_TAB_ACTIVE_IDLE, UiPalette.BTN_TAB_ACTIVE_HOVER, this.hoverProgress);
            case TAB_IDLE:
                return UiUtil.lerpColor(UiPalette.BTN_TAB_IDLE_IDLE, UiPalette.BTN_TAB_IDLE_HOVER, this.hoverProgress);
            case SECONDARY:
            default:
                return UiUtil.lerpColor(UiPalette.BTN_SECONDARY_IDLE, UiPalette.BTN_SECONDARY_HOVER, this.hoverProgress);
        }
    }

    private int borderColor() {
        if (!this.active) {
            return UiPalette.BORDER;
        }
        switch (this.variant) {
            case PRIMARY:
                return UiPalette.BTN_PRIMARY_BORDER;
            case DANGER:
                return UiPalette.BTN_DANGER_BORDER;
            case TAB_ACTIVE:
                return UiPalette.ACCENT;
            case TAB_IDLE:
            case GHOST:
            case SECONDARY:
            default:
                return UiUtil.lerpColor(UiPalette.BORDER, UiPalette.BORDER_STRONG, this.hoverProgress);
        }
    }

    private String fitLabel(Minecraft minecraft, String raw, int maxWidth) {
        if (raw == null || raw.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (minecraft.fontRenderer.getStringWidth(raw) <= maxWidth) {
            return raw;
        }
        String clipped = minecraft.fontRenderer.trimStringToWidth(raw, Math.max(0, maxWidth - minecraft.fontRenderer.getStringWidth("...")));
        if (clipped == null || clipped.isEmpty()) {
            return "";
        }
        return clipped + "...";
    }
}
