package dglabmc.platform;

import java.nio.file.Path;
import java.util.UUID;

public interface PlatformClientBridge {
    void openControlCenter();

    void showPlayerMessage(String message);

    void copyToClipboard(String value);

    String readClipboard();

    void openInFileManager(Path path);

    boolean isCurrentLocalPlayer(UUID playerId);
}


