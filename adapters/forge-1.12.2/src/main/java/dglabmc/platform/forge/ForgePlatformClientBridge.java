package dglabmc.platform.forge;

import dglabmc.client.ui.ControlCenterScreen;
import dglabmc.client.ui.PasswordGateScreen;
import dglabmc.platform.PlatformClientBridge;
import dglabmc.core.security.DailyPasswordLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.TextComponentString;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class ForgePlatformClientBridge implements PlatformClientBridge {
    @Override
    public void openControlCenter() {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> {
            if (minecraft.player != null) {
                DailyPasswordLock.clearExpiredLock();
                minecraft.displayGuiScreen(DailyPasswordLock.isUnlocked() ? new ControlCenterScreen() : new PasswordGateScreen(new ControlCenterScreen()));
            }
        });
    }

    @Override
    public void showPlayerMessage(String message) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> {
            if (minecraft.player != null) {
                minecraft.player.sendMessage(new TextComponentString(message));
            }
        });
    }

    @Override
    public void copyToClipboard(String value) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> GuiScreen.setClipboardString(value == null ? "" : value));
    }

    @Override
    public String readClipboard() {
        return GuiScreen.getClipboardString();
    }

    @Override
    public boolean isCurrentLocalPlayer(UUID playerId) {
        Minecraft minecraft = Minecraft.getMinecraft();
        return playerId != null && minecraft.player != null && playerId.equals(minecraft.player.getUniqueID());
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
