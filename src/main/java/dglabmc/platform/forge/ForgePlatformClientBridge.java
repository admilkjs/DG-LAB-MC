package dglabmc.platform.forge;

import dglabmc.client.ui.ControlCenterScreen;
import dglabmc.client.ui.PasswordGateScreen;
import dglabmc.platform.PlatformClientBridge;
import dglabmc.security.DailyPasswordLock;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.StringTextComponent;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ForgePlatformClientBridge implements PlatformClientBridge {
    @Override
    public void openControlCenter() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.player != null) {
                DailyPasswordLock.clearExpiredLock();
                minecraft.displayGuiScreen(DailyPasswordLock.isUnlocked() ? new ControlCenterScreen() : new PasswordGateScreen(new ControlCenterScreen()));
            }
        });
    }

    @Override
    public void showPlayerMessage(String message) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.player != null) {
                minecraft.player.sendMessage(new StringTextComponent(message));
            }
        });
    }

    @Override
    public void copyToClipboard(String value) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(value == null ? "" : value), null);
    }

    @Override
    public String readClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                Object value = clipboard.getData(DataFlavor.stringFlavor);
                return value instanceof String ? (String) value : "";
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    @Override
    public void openInFileManager(Path path) {
        if (path == null) {
            return;
        }
        Path target = Files.isDirectory(path) ? path : path.getParent();
        if (target == null) {
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(target.toFile());
                return;
            }
        } catch (IOException ignored) {
        }
        try {
            Runtime.getRuntime().exec(new String[]{"explorer.exe", target.toAbsolutePath().toString()});
        } catch (IOException ignored) {
        }
    }
}
