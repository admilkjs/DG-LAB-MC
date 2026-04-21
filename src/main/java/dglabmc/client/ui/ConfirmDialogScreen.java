package dglabmc.client.ui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

public class ConfirmDialogScreen extends BaseScreen {
    private final String heading;
    private final String description;
    private final String confirmLabel;
    private final Runnable confirmAction;

    public ConfirmDialogScreen(Screen parent, String heading, String description, String confirmLabel, Runnable confirmAction) {
        super(new StringTextComponent(heading), parent);
        this.heading = heading;
        this.description = description;
        this.confirmLabel = confirmLabel;
        this.confirmAction = confirmAction;
    }

    @Override protected int maxPanelWidth() { return 420; }
    @Override protected int compactThreshold() { return 360; }
    @Override protected int panelHeightNormal() { return 150; }
    @Override protected int panelHeightCompact() { return 174; }

    @Override
    protected void buildWidgets() {
        int il = innerLeft();
        int iw = innerWidth();
        int btnY = this.panelTop + 102;
        if (compact) {
            this.addButton(new StyledButton(il, btnY, iw, UiConstants.BTN_HEIGHT, new StringTextComponent(this.confirmLabel), StyledButton.Variant.PRIMARY, button -> onConfirm()));
            this.addButton(new StyledButton(il, btnY + UiConstants.BTN_STRIDE, iw, UiConstants.BTN_HEIGHT, new StringTextComponent("取消"), StyledButton.Variant.SECONDARY, button -> this.minecraft.setScreen(this.parent)));
        } else {
            this.addButton(new StyledButton(il, btnY, 110, UiConstants.BTN_HEIGHT, new StringTextComponent(this.confirmLabel), StyledButton.Variant.PRIMARY, button -> onConfirm()));
            this.addButton(new StyledButton(this.panelLeft + 136, btnY, 110, UiConstants.BTN_HEIGHT, new StringTextComponent("取消"), StyledButton.Variant.SECONDARY, button -> this.minecraft.setScreen(this.parent)));
        }
    }

    private void onConfirm() {
        try {
            this.confirmAction.run();
        } catch (RuntimeException exception) {
            setStatus(exception.getMessage() == null ? "操作失败。" : exception.getMessage());
        }
    }

    @Override
    protected void renderContent(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        UiRender.drawSectionTitle(matrixStack, this.font, this.heading, "", innerLeft(), this.panelTop + 16);
        UiRender.drawWrappedText(matrixStack, this.font, this.description, innerLeft(), this.panelTop + 42, innerWidth(), UiPalette.TEXT_MUTED, 4);
    }
}
