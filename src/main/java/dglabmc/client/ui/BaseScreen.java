package dglabmc.client.ui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;

public abstract class BaseScreen extends Screen {
    protected final Screen parent;

    protected int panelWidth;
    protected int panelHeight;
    protected int panelLeft;
    protected int panelTop;
    protected boolean compact;

    protected String statusMessage = "";
    protected long statusMessageTime;

    protected BaseScreen(ITextComponent title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    protected int maxPanelWidth() {
        return 560;
    }

    protected int compactThreshold() {
        return UiConstants.COMPACT_THRESHOLD_MD;
    }

    protected int panelHeightNormal() {
        return 200;
    }

    protected int panelHeightCompact() {
        return 240;
    }

    @Override
    protected void init() {
        this.buttons.clear();
        this.children.clear();
        computeLayout();
        buildWidgets();
    }

    protected void computeLayout() {
        this.panelWidth = Math.min(maxPanelWidth(), this.width - UiConstants.SCREEN_MARGIN);
        this.compact = this.panelWidth < compactThreshold();
        this.panelHeight = Math.min(this.compact ? panelHeightCompact() : panelHeightNormal(), this.height - UiConstants.SCREEN_MARGIN);
        this.panelLeft = (this.width - this.panelWidth) / 2;
        this.panelTop = (this.height - this.panelHeight) / 2;
    }

    protected abstract void buildWidgets();

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        fillGradient(matrixStack, 0, 0, this.width, this.height, UiPalette.BACKGROUND_TOP, UiPalette.BACKGROUND_BOTTOM);
        UiRender.drawPanel(matrixStack, this.panelLeft, this.panelTop, this.panelWidth, this.panelHeight, UiPalette.PANEL, UiPalette.ACCENT);
        renderContent(matrixStack, mouseX, mouseY, partialTicks);
        renderStatus(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    protected abstract void renderContent(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks);

    protected void renderStatus(MatrixStack matrixStack) {
        if (this.statusMessage == null || this.statusMessage.isEmpty()) {
            return;
        }
        float alpha = statusAlpha();
        if (alpha <= 0.0F) {
            this.statusMessage = "";
            return;
        }
        int color = UiPalette.WARNING;
        if (alpha < 1.0F) {
            color = UiUtil.withAlpha(color, (int) (alpha * 255.0F));
        }
        UiRender.drawWrappedText(matrixStack, this.font, this.statusMessage, this.panelLeft + UiConstants.PANEL_INNER_PAD, this.panelTop + this.panelHeight - 22, this.panelWidth - UiConstants.PANEL_INNER_PAD * 2, color, 2);
    }

    protected float statusAlpha() {
        if (this.statusMessageTime <= 0L) {
            return 1.0F;
        }
        long elapsed = System.currentTimeMillis() - this.statusMessageTime;
        long fadeStart = 3000L;
        long fadeDuration = 1000L;
        if (elapsed < fadeStart) {
            return 1.0F;
        }
        if (elapsed > fadeStart + fadeDuration) {
            return 0.0F;
        }
        return 1.0F - (float) (elapsed - fadeStart) / (float) fadeDuration;
    }

    protected void setStatus(String message) {
        this.statusMessage = message == null ? "" : message;
        this.statusMessageTime = System.currentTimeMillis();
    }

    protected int innerLeft() {
        return this.panelLeft + UiConstants.PANEL_INNER_PAD;
    }

    protected int innerTop() {
        return this.panelTop + UiConstants.PANEL_INNER_PAD;
    }

    protected int innerWidth() {
        return this.panelWidth - UiConstants.PANEL_INNER_PAD * 2;
    }

    @Override
    public void onClose() {
        if (this.parent != null) {
            this.minecraft.setScreen(this.parent);
        } else {
            super.onClose();
        }
    }
}
