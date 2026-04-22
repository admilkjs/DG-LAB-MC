package dglabmc.client.ui;

import dglabmc.AppServices;
import dglabmc.platform.PlatformServices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class PairingQrScreen extends Screen {
    private final Screen parent;
    private String pairingLink = "";
    private QrCodeHelper.QrMatrix pairingQrMatrix;
    private String errorMessage = "";
    private String serverStatusText = "";
    private String serverDetailText = "";
    private String serverErrorText = "";
    private int statusRefreshCooldown;

    public PairingQrScreen(Screen parent) {
        super(Component.literal("扫码连接"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        reloadPairingLink(false);
        refreshServerStatus();

        int panelWidth = Math.min(560, this.width - 24);
        boolean compact = panelWidth < 420;
        int panelHeight = Math.min(compact ? 448 : 432, this.height - 24);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int innerWidth = panelWidth - 36;
        int buttonWidth = compact ? innerWidth : (innerWidth - 16) / 3;
        int buttonY = top + panelHeight - 34;

        if (compact) {
            this.addRenderableWidget(new StyledButton(left + 18, buttonY - 48, innerWidth, 20, Component.literal("刷新链接"), StyledButton.Variant.GHOST, button -> {
                reloadPairingLink(true);
                init();
            }));
            this.addRenderableWidget(new StyledButton(left + 18, buttonY - 24, innerWidth, 20, Component.literal("复制链接"), StyledButton.Variant.SECONDARY, button -> PlatformServices.client().copyToClipboard(this.pairingLink)));
            this.addRenderableWidget(new StyledButton(left + 18, buttonY, innerWidth, 20, Component.literal("关闭"), StyledButton.Variant.PRIMARY, button -> onClose()));
            return;
        }

        this.addRenderableWidget(new StyledButton(left + 18, buttonY, buttonWidth, 20, Component.literal("刷新链接"), StyledButton.Variant.GHOST, button -> {
            reloadPairingLink(true);
            init();
        }));
        this.addRenderableWidget(new StyledButton(left + 26 + buttonWidth, buttonY, buttonWidth, 20, Component.literal("复制链接"), StyledButton.Variant.SECONDARY, button -> PlatformServices.client().copyToClipboard(this.pairingLink)));
        this.addRenderableWidget(new StyledButton(left + 34 + buttonWidth * 2, buttonY, buttonWidth, 20, Component.literal("关闭"), StyledButton.Variant.PRIMARY, button -> onClose()));
    }

    @Override
    public void tick() {
        if (--this.statusRefreshCooldown <= 0) {
            refreshServerStatus();
            this.statusRefreshCooldown = 10;
        }
        if (AppServices.get().isDeviceBound()) {
            onClose();
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        guiGraphics.fillGradient(0, 0, this.width, this.height, UiPalette.BACKGROUND_TOP, UiPalette.BACKGROUND_BOTTOM);

        int panelWidth = Math.min(560, this.width - 24);
        boolean compact = panelWidth < 420;
        int panelHeight = Math.min(compact ? 448 : 432, this.height - 24);
        int left = (this.width - panelWidth) / 2;
        int top = (this.height - panelHeight) / 2;
        int statusBlockHeight = compact ? 70 : 58;
        int qrAreaTop = top + 52 + statusBlockHeight;
        int qrAreaBottom = top + panelHeight - (compact ? 92 : 56);
        int qrSize = Math.max(160, Math.min(panelWidth - 80, qrAreaBottom - qrAreaTop));
        int qrX = left + ((panelWidth - qrSize) / 2);
        int qrY = qrAreaTop + Math.max(0, ((qrAreaBottom - qrAreaTop) - qrSize) / 2);

        UiRender.drawPanel(guiGraphics, left, top, panelWidth, panelHeight, UiPalette.PANEL, UiPalette.ACCENT);
        UiRender.drawSectionTitle(guiGraphics, this.font, "扫码连接", "绑定后自动关闭，Esc 退出", left + 18, top + 16);
        drawServerStatus(guiGraphics, left + 18, top + 42, panelWidth - 36, compact);

        if (this.pairingQrMatrix != null) {
            QrCodeHelper.draw(guiGraphics, this.pairingQrMatrix, qrX, qrY, qrSize);
        } else {
            UiRender.drawPanel(guiGraphics, qrX, qrY, qrSize, qrSize, 0xFFF8FAFC, 0xFFCBD5E1);
            String text = "二维码失败";
            guiGraphics.drawString(this.font, text, qrX + ((qrSize - this.font.width(text)) / 2), qrY + (qrSize / 2) - 4, 0xFF0F172A);
        }

        int linkY = qrY + qrSize + 10;
        UiRender.drawWrappedText(guiGraphics, this.font, this.pairingLink, left + 18, linkY, panelWidth - 36, UiPalette.TEXT_MUTED, compact ? 2 : 3);
        if (!this.errorMessage.isEmpty()) {
            UiRender.drawWrappedText(guiGraphics, this.font, this.errorMessage, left + 18, linkY + 28, panelWidth - 36, UiPalette.WARNING, 3);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void reloadPairingLink(boolean refresh) {
        try {
            this.pairingLink = refresh ? AppServices.get().refreshPairingLink() : AppServices.get().getPairingLink();
            this.pairingQrMatrix = QrCodeHelper.encode(this.pairingLink);
            this.errorMessage = "";
        } catch (Throwable throwable) {
            this.pairingLink = "";
            this.pairingQrMatrix = null;
            String message = throwable.getMessage();
            this.errorMessage = message == null || message.trim().isEmpty() ? throwable.getClass().getSimpleName() : message;
        }
        refreshServerStatus();
    }

    private void refreshServerStatus() {
        boolean running = AppServices.get().isDeviceServerRunning();
        int port = running ? AppServices.get().getDevicePort() : -1;
        String bindAddress = AppServices.get().getDeviceServerBindAddress();
        this.serverStatusText = running ? "服务状态：运行中" : "服务状态：未启动";
        this.serverDetailText = running ? "监听地址 " + bindAddress + ":" + port : "Ws 服务未监听";
        String serverError = AppServices.get().getDeviceServerError();
        this.serverErrorText = serverError == null ? "" : serverError;
    }

    private void drawServerStatus(GuiGraphics guiGraphics, int x, int y, int width, boolean compact) {
        boolean running = AppServices.get().isDeviceServerRunning();
        int badgeBackground = running ? 0x3322C55E : 0x33EF4444;
        int badgeBorder = running ? UiPalette.SUCCESS : UiPalette.DANGER;
        UiRender.drawStatusBadge(guiGraphics, this.font, this.serverStatusText, x, y, badgeBackground, badgeBorder);
        guiGraphics.drawString(this.font, this.serverDetailText, x, y + 18, UiPalette.TEXT_MUTED);
        if (!this.serverErrorText.isEmpty()) {
            UiRender.drawWrappedText(guiGraphics, this.font, this.serverErrorText, x, y + (compact ? 32 : 30), width, UiPalette.WARNING, compact ? 3 : 2);
        }
    }
}
