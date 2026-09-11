package dglabmc.core.device;

/** Minimal platform-neutral connection handle implemented by a loader adapter. */
public interface DeviceConnection {
    boolean isOpen();
    void sendText(String text);
    void close();
}
