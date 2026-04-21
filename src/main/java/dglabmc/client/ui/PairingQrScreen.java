package dglabmc.client.ui;

import dglabmc.AppServices;
import dglabmc.platform.PlatformServices;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

public class PairingQrScreen extends Screen {
    private final Screen parent;
    private String pairingLink = "";
    private QrCodeHelper.QrMatrix pairingQrMatrix;
    private String errorMessage = "";

    public PairingQrScreen(Screen parent) {
        super(new StringTextComponent("扫码连接"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.buttons.clear();
        this.children.clear();
        reloadPairingLink(false);

        int panelWidth = Math.min(560, this.width - 24);
        boolean compact = panelWidth < 420;
        int panelHeight = Math.min(compact ? 420 : 408, this.height - 24);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int innerWidth = panelWidth - 36;
        int buttonWidth = compact ? innerWidth : (innerWidth - 16) / 3;
        int buttonY = top + panelHeight - 34;

        if (compact) {
            this.addButton(new StyledButton(left + 18, buttonY - 48, innerWidth, 20, new StringTextComponent("刷新链接"), StyledButton.Variant.GHOST, button -> {
                reloadPairingLink(true);
                init();
            }));
            this.addButton(new StyledButton(left + 18, buttonY - 24, innerWidth, 20, new StringTextComponent("复制链接"), StyledButton.Variant.SECONDARY, button -> PlatformServices.client().copyToClipboard(this.pairingLink)));
            this.addButton(new StyledButton(left + 18, buttonY, innerWidth, 20, new StringTextComponent("关闭"), StyledButton.Variant.PRIMARY, button -> onClose()));
            return;
        }

        this.addButton(new StyledButton(left + 18, buttonY, buttonWidth, 20, new StringTextComponent("刷新链接"), StyledButton.Variant.GHOST, button -> {
            reloadPairingLink(true);
            init();
        }));
        this.addButton(new StyledButton(left + 26 + buttonWidth, buttonY, buttonWidth, 20, new StringTextComponent("复制链接"), StyledButton.Variant.SECONDARY, button -> PlatformServices.client().copyToClipboard(this.pairingLink)));
        this.addButton(new StyledButton(left + 34 + buttonWidth * 2, buttonY, buttonWidth, 20, new StringTextComponent("关闭"), StyledButton.Variant.PRIMARY, button -> onClose()));
    }

    @Override
    public void tick() {
        if (AppServices.get().isDeviceBound()) {
            onClose();
        }
    }

    @Override
    public void onClose() {
        this.minecraft.displayGuiScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        MatrixStack matrixStack = new MatrixStack();
        this.renderBackground();
        fillGradient(0, 0, this.width, this.height, UiPalette.BACKGROUND_TOP, UiPalette.BACKGROUND_BOTTOM);

        int panelWidth = Math.min(560, this.width - 24);
        boolean compact = panelWidth < 420;
        int panelHeight = Math.min(compact ? 420 : 408, this.height - 24);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int qrAreaTop = top + 52;
        int qrAreaBottom = top + panelHeight - (compact ? 92 : 56);
        int qrSize = Math.max(160, Math.min(panelWidth - 80, qrAreaBottom - qrAreaTop));
        int qrX = left + ((panelWidth - qrSize) / 2);
        int qrY = qrAreaTop + Math.max(0, ((qrAreaBottom - qrAreaTop) - qrSize) / 2);

        UiRender.drawPanel(matrixStack, left, top, panelWidth, panelHeight, UiPalette.PANEL, UiPalette.ACCENT);
        UiRender.drawSectionTitle(matrixStack, this.font, "扫码连接", "绑定后自动关闭，Esc 退出", left + 18, top + 16);

        if (this.pairingQrMatrix != null) {
            QrCodeHelper.draw(matrixStack, this.pairingQrMatrix, qrX, qrY, qrSize);
        } else {
            UiRender.drawPanel(matrixStack, qrX, qrY, qrSize, qrSize, 0xFFF8FAFC, 0xFFCBD5E1);
            this.font.drawString("二维码失败", (float) (qrX + ((qrSize - this.font.getStringWidth("二维码失败")) / 2)), (float) (qrY + (qrSize / 2) - 4), 0xFF0F172A);
        }

        int linkY = qrY + qrSize + 10;
        UiRender.drawWrappedText(matrixStack, this.font, this.pairingLink, left + 18, linkY, panelWidth - 36, UiPalette.TEXT_MUTED, compact ? 2 : 3);
        if (!this.errorMessage.isEmpty()) {
            UiRender.drawWrappedText(matrixStack, this.font, this.errorMessage, left + 18, linkY + 28, panelWidth - 36, UiPalette.WARNING, 3);
        }

        super.render(mouseX, mouseY, partialTicks);
    }

    private void reloadPairingLink(boolean refresh) {
        this.pairingLink = refresh ? AppServices.get().refreshPairingLink() : AppServices.get().getPairingLink();
        try {
            this.pairingQrMatrix = QrCodeHelper.encode(this.pairingLink);
            this.errorMessage = "";
        } catch (Throwable throwable) {
            this.pairingQrMatrix = null;
            String message = throwable.getMessage();
            this.errorMessage = message == null || message.trim().isEmpty() ? throwable.getClass().getSimpleName() : message;
        }
    }
}
