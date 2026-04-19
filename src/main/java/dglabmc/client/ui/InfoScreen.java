package dglabmc.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class InfoScreen extends Screen {
    private final Screen parent;
    private final String heading;
    private final String subtitle;
    private final List<String> lines;

    public InfoScreen(Screen parent, String heading, String subtitle, List<String> lines) {
        super(Component.literal(heading));
        this.parent = parent;
        this.heading = heading;
        this.subtitle = subtitle;
        this.lines = new ArrayList<String>(lines);
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(560, this.width - 24);
        int panelHeight = Math.min(300, this.height - 24);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        this.addRenderableWidget(new StyledButton(left + panelWidth - 128, top + panelHeight - 34, 110, 20, Component.literal("返回"), StyledButton.Variant.SECONDARY, button -> this.minecraft.setScreen(this.parent)));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        guiGraphics.fillGradient( 0, 0, this.width, this.height, UiPalette.BACKGROUND_TOP, UiPalette.BACKGROUND_BOTTOM);
        int panelWidth = Math.min(560, this.width - 24);
        int panelHeight = Math.min(300, this.height - 24);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        UiRender.drawPanel(guiGraphics, left, top, panelWidth, panelHeight, UiPalette.PANEL, UiPalette.ACCENT);
        UiRender.drawSectionTitle(guiGraphics, this.font, this.heading, this.subtitle, left + 18, top + 16);
        int drawY = top + 52;
        for (String line : this.lines) {
            drawY += UiRender.drawWrappedText(guiGraphics, this.font, line, left + 18, drawY, panelWidth - 36, UiPalette.TEXT_MUTED, 3) * 12;
            drawY += 8;
            if (drawY > top + panelHeight - 56) {
                break;
            }
        }
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }
}

