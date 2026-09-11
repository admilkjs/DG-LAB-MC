package dglabmc.platform;

import java.nio.file.Path;
import java.util.UUID;

public final class NoopPlatformClientBridge implements PlatformClientBridge {
    @Override
    public void openControlCenter() {
    }

    @Override
    public void showPlayerMessage(String message) {
    }

    @Override
    public void copyToClipboard(String value) {
    }

    @Override
    public String readClipboard() {
        return "";
    }

    @Override
    public void openInFileManager(Path path) {
    }

    @Override
    public boolean isCurrentLocalPlayer(UUID playerId) {
        return false;
    }
}


