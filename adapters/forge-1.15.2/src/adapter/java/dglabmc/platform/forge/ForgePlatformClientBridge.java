package dglabmc.platform.forge;

import dglabmc.platform.PlatformClientBridge;
import dglabmc.client.ui.ControlCenterScreen;
import dglabmc.client.ui.PasswordGateScreen;
import dglabmc.core.security.DailyPasswordLock;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.StringTextComponent;
import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class ForgePlatformClientBridge implements PlatformClientBridge {
    @Override public void openControlCenter() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            DailyPasswordLock.clearExpiredLock();
            mc.displayGuiScreen(DailyPasswordLock.isUnlocked()
                ? new ControlCenterScreen()
                : new PasswordGateScreen(new ControlCenterScreen()));
        }
    }
    @Override public void showPlayerMessage(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.sendMessage(new StringTextComponent(message));
    }
    @Override public void copyToClipboard(String value) {
        try { Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value == null ? "" : value), null); } catch (RuntimeException ignored) {}
    }
    @Override public String readClipboard() {
        try { Clipboard c=Toolkit.getDefaultToolkit().getSystemClipboard(); if(c.isDataFlavorAvailable(DataFlavor.stringFlavor)){ Object v=c.getData(DataFlavor.stringFlavor); return v instanceof String ? (String)v : ""; } } catch (Exception ignored) {}
        return "";
    }
    @Override public void openInFileManager(Path path) {
        if (path == null) return; Path target = Files.isDirectory(path) ? path : path.getParent(); if(target == null) return;
        try { if(Desktop.isDesktopSupported()){ Desktop.getDesktop().open(target.toFile()); return; } } catch(IOException ignored) {}
    }
    @Override public boolean isCurrentLocalPlayer(UUID playerId) {
        return playerId != null && Minecraft.getInstance().player != null && playerId.equals(Minecraft.getInstance().player.getUniqueID());
    }
}
