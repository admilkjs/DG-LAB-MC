package dglabmc.client.ui;

import dglabmc.AppServices;
import dglabmc.platform.PlatformServices;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;

public class PairingQrScreen extends BaseScreen {
    private String pairingLink = "";
    private QrCodeHelper.QrMatrix pairingQrMatrix;
    private String errorMessage = "";

    public PairingQrScreen(Screen parent) {
        super(new StringTextComponent("扫码连接"), parent);
    }

    @Override protected int maxPanelWidth() { return 560; }
    @Override protected int compactThreshold() { return 420; }
    @Override protected int panelHeightNormal() { return 408; }
    @Override protected int panelHeightCompact() { return 420; }

    @Override
    protected void buildWidgets() {
        reloadPairingLink(false);

        int il = innerLeft();
        int iw = innerWidth();
        int buttonWidth = compact ? iw : (iw - UiConstants.PAD_LG) / 3;
        int buttonY = this.panelTop + this.panelHeight - 34;

        if (compact) {
            this.addButton(new StyledButton(il, buttonY - 48, iw, UiConstants.BTN_HEIGHT, new StringTextComponent("刷新链接"), StyledButton.Variant.GHOST, button -> {
                reloadPairingLink(true);
                init();
            }));
            this.addButton(new StyledButton(il, buttonY - 24, iw, UiConstants.BTN_HEIGHT, new StringTextComponent("复制链接"), StyledButton.Variant.SECONDARY, button -> PlatformServices.client().copyToClipboard(this.pairingLink)));
            this.addButton(new StyledButton(il, buttonY, iw, UiConstants.BTN_HEIGHT, new StringTextComponent("关闭"), StyledButton.Variant.PRIMARY, button -> onClose()));
            return;
        }

        this.addButton(new StyledButton(il, buttonY, buttonWidth, UiConstants.BTN_HEIGHT, new StringTextComponent("刷新链接"), StyledButton.Variant.GHOST, button -> {
            reloadPairingLink(true);
            init();
        }));
        this.addButton(new StyledButton(il + buttonWidth + UiConstants.PAD_SM, buttonY, buttonWidth, UiConstants.BTN_HEIGHT, new StringTextComponent("复制链接"), StyledButton.Variant.SECONDARY, button -> PlatformServices.client().copyToClipboard(this.pairingLink)));
        this.addButton(new StyledButton(il + (buttonWidth + UiConstants.PAD_SM) * 2, buttonY, buttonWidth, UiConstants.BTN_HEIGHT, new StringTextComponent("关闭"), StyledButton.Variant.PRIMARY, button -> onClose()));
    }

    @Override
    public void tick() {
        if (AppServices.get().isDeviceBound()) {
            onClose();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void renderContent(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        int il = innerLeft();
        int iw = innerWidth();
        int qrAreaTop = this.panelTop + 52;
        int qrAreaBottom = this.panelTop + this.panelHeight - (compact ? 92 : 56);
        int qrSize = Math.max(160, Math.min(this.panelWidth - 80, qrAreaBottom - qrAreaTop));
        int qrX = this.panelLeft + ((this.panelWidth - qrSize) / 2);
        int qrY = qrAreaTop + Math.max(0, ((qrAreaBottom - qrAreaTop) - qrSize) / 2);

        UiRender.drawSectionTitle(matrixStack, this.font, "扫码连接", "绑定后自动关闭，Esc 退出", il, this.panelTop + 16);

        if (this.pairingQrMatrix != null) {
            QrCodeHelper.draw(matrixStack, this.pairingQrMatrix, qrX, qrY, qrSize);
        } else {
            UiRender.drawPanel(matrixStack, qrX, qrY, qrSize, qrSize, UiPalette.QR_BG, UiPalette.QR_BORDER);
            this.font.drawString("二维码失败", (float) (qrX + ((qrSize - this.font.getStringWidth("二维码失败")) / 2)), (float) (qrY + (qrSize / 2) - 4), UiPalette.QR_TEXT);
        }

        int linkY = qrY + qrSize + 10;
        UiRender.drawWrappedText(matrixStack, this.font, this.pairingLink, il, linkY, iw, UiPalette.TEXT_MUTED, compact ? 2 : 3);
        if (!this.errorMessage.isEmpty()) {
            UiRender.drawWrappedText(matrixStack, this.font, this.errorMessage, il, linkY + 28, iw, UiPalette.WARNING, 3);
        }
    }

    private void reloadPairingLink(boolean refresh) {
        this.pairingLink = refresh ? AppServices.get().refreshPairingLink() : AppServices.get().getPairingLink();
        try {
            this.pairingQrMatrix = QrCodeHelper.encode(this.pairingLink);
            this.errorMessage = "";
        } catch (Throwable throwable) {
            this.pairingQrMatrix = null;
            this.errorMessage = UiUtil.safeMessage(throwable, throwable.getClass().getSimpleName());
        }
    }
}
