package dglabmc.platform.forge;

import dglabmc.client.ui.ControlCenterScreen;
import dglabmc.client.ui.PasswordGateScreen;
import dglabmc.platform.PlatformClientBridge;
import dglabmc.security.DailyPasswordLock;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;

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
                minecraft.setScreen(DailyPasswordLock.isUnlocked() ? new ControlCenterScreen() : new PasswordGateScreen(new ControlCenterScreen()));
            }
        });
    }

    @Override
    public void showPlayerMessage(String message) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (minecraft.player != null) {
                minecraft.player.sendMessage(new StringTextComponent(message), Util.NIL_UUID);
            }
        });
    }

    @Override
    public void copyToClipboard(String value) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.keyboardHandler.setClipboard(value == null ? "" : value));
    }

    @Override
    public String readClipboard() {
        return Minecraft.getInstance().keyboardHandler.getClipboard();
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
