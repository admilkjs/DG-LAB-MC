package dglabmc.client.ui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

import java.util.ArrayList;
import java.util.List;

public class InfoScreen extends BaseScreen {
    private final String heading;
    private final String subtitle;
    private final List<String> lines;
    private int scrollOffset;

    public InfoScreen(Screen parent, String heading, String subtitle, List<String> lines) {
        super(new StringTextComponent(heading), parent);
        this.heading = heading;
        this.subtitle = subtitle;
        this.lines = new ArrayList<>(lines);
    }

    @Override protected int maxPanelWidth() { return 560; }
    @Override protected int compactThreshold() { return 0; }
    @Override protected int panelHeightNormal() { return 300; }

    @Override
    protected void buildWidgets() {
        this.scrollOffset = 0;
        this.addButton(new StyledButton(this.panelLeft + this.panelWidth - 128, this.panelTop + this.panelHeight - 34, 110, UiConstants.BTN_HEIGHT, new StringTextComponent("返回"), StyledButton.Variant.SECONDARY, button -> this.minecraft.setScreen(this.parent)));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        this.scrollOffset = Math.max(0, this.scrollOffset - (int) (delta * UiConstants.LINE_HEIGHT));
        return true;
    }

    @Override
    protected void renderContent(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        int il = innerLeft();
        int iw = innerWidth();
        UiRender.drawSectionTitle(matrixStack, this.font, this.heading, this.subtitle, il, this.panelTop + 16);
        int contentTop = this.panelTop + 52;
        int contentBottom = this.panelTop + this.panelHeight - 56;
        int drawY = contentTop - this.scrollOffset;
        for (String line : this.lines) {
            int lineH = UiRender.measureWrappedTextHeight(this.font, line, iw, 100);
            if (drawY + lineH > contentTop - lineH && drawY < contentBottom) {
                UiRender.drawWrappedText(matrixStack, this.font, line, il, drawY, iw, UiPalette.TEXT_MUTED, 100);
            }
            drawY += lineH + UiConstants.PAD_SM;
        }
    }
}
