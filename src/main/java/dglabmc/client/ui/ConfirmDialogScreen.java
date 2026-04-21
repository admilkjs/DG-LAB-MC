package dglabmc.client.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfirmDialogScreen extends Screen {
    private final Screen parent;
    private final String heading;
    private final String description;
    private final String confirmLabel;
    private final Runnable confirmAction;
    private String status = "";

    public ConfirmDialogScreen(Screen parent, String heading, String description, String confirmLabel, Runnable confirmAction) {
        super(Component.literal(heading));
        this.parent = parent;
        this.heading = heading;
        this.description = description;
        this.confirmLabel = confirmLabel;
        this.confirmAction = confirmAction;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(420, this.width - 24);
        boolean compact = panelWidth < 360;
        int panelHeight = Math.min(compact ? 174 : 150, this.height - 24);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        if (compact) {
            this.addRenderableWidget(new StyledButton(left + 18, top + 102, panelWidth - 36, 20, Component.literal(this.confirmLabel), StyledButton.Variant.PRIMARY, button -> onConfirm()));
            this.addRenderableWidget(new StyledButton(left + 18, top + 126, panelWidth - 36, 20, Component.literal("取消"), StyledButton.Variant.SECONDARY, button -> this.minecraft.setScreen(this.parent)));
        } else {
            this.addRenderableWidget(new StyledButton(left + 18, top + 102, 110, 20, Component.literal(this.confirmLabel), StyledButton.Variant.PRIMARY, button -> onConfirm()));
            this.addRenderableWidget(new StyledButton(left + 136, top + 102, 110, 20, Component.literal("取消"), StyledButton.Variant.SECONDARY, button -> this.minecraft.setScreen(this.parent)));
        }
    }

    private void onConfirm() {
        try {
            this.confirmAction.run();
        } catch (RuntimeException exception) {
            this.status = exception.getMessage() == null ? "操作失败。" : exception.getMessage();
        }
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        fillGradient(matrixStack, 0, 0, this.width, this.height, UiPalette.BACKGROUND_TOP, UiPalette.BACKGROUND_BOTTOM);
        int panelWidth = Math.min(420, this.width - 24);
        boolean compact = panelWidth < 360;
        int panelHeight = Math.min(compact ? 174 : 150, this.height - 24);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        UiRender.drawPanel(matrixStack, left, top, panelWidth, panelHeight, UiPalette.PANEL, UiPalette.ACCENT);
        UiRender.drawSectionTitle(matrixStack, this.font, this.heading, "", left + 18, top + 16);
        UiRender.drawWrappedText(matrixStack, this.font, this.description, left + 18, top + 42, panelWidth - 36, UiPalette.TEXT_MUTED, 4);
        if (!this.status.isEmpty()) {
            this.font.draw(matrixStack, this.status, (float) (left + 18), (float) (top + panelHeight - 22), UiPalette.WARNING);
        }
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }
}
