package dglabmc.config;

import net.minecraftforge.common.config.Configuration;
import java.io.File;

/** Forge 1.12 startup settings adapter. */
public final class StartupConfig {
    private static final int LEGACY_DEFAULT_PORT = 27852;
    private static final int CURRENT_DEFAULT_PORT = 21733;
    private static int deviceWebSocketPort = CURRENT_DEFAULT_PORT;
    private static boolean openMenuOnLogin;
    private StartupConfig() {}
    public static synchronized void load(File configDirectory) {
        if (configDirectory == null) return;
        Configuration configuration = new Configuration(new File(configDirectory, "dglabmc.cfg"));
        try {
            configuration.load();
            deviceWebSocketPort = configuration.getInt("deviceWebSocketPort", "device", CURRENT_DEFAULT_PORT, 1024, 65535, "DG-LAB WebSocket port");
            openMenuOnLogin = configuration.getBoolean("openMenuOnLogin", "ui", false, "Open control center after login");
        } finally { if (configuration.hasChanged()) configuration.save(); }
    }
    public static synchronized boolean shouldOpenMenuOnLogin() { return openMenuOnLogin; }
    public static synchronized int resolveDevicePort() { return deviceWebSocketPort == LEGACY_DEFAULT_PORT ? CURRENT_DEFAULT_PORT : deviceWebSocketPort; }
}
