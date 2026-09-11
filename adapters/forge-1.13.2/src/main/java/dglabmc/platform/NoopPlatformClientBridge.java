package dglabmc.platform;

import java.nio.file.Path;

/** Safe common-side bridge; the Forge client bridge is installed on client setup. */
public final class NoopPlatformClientBridge implements PlatformClientBridge {
    @Override public void openControlCenter() { }
    @Override public void showPlayerMessage(String message) { }
    @Override public void copyToClipboard(String value) { }
    @Override public String readClipboard() { return ""; }
    @Override public void openInFileManager(Path path) { }
}
