package dglabmc.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class StartupConfig {
    private static final int LEGACY_DEFAULT_PORT = 27852;
    private static final int CURRENT_DEFAULT_PORT = 21733;
    private static final String FILE_NAME = "startup.properties";

    private static volatile int deviceWebSocketPort = CURRENT_DEFAULT_PORT;
    private static volatile boolean openMenuOnLogin;

    private StartupConfig() {
    }

    public static synchronized void load(Path rootDirectory) {
        if (rootDirectory == null) {
            return;
        }
        Properties properties = new Properties();
        Path file = rootDirectory.resolve(FILE_NAME);
        try {
            Files.createDirectories(rootDirectory);
            if (Files.exists(file)) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }
            }
            deviceWebSocketPort = parsePort(properties.getProperty("deviceWebSocketPort"), CURRENT_DEFAULT_PORT);
            openMenuOnLogin = Boolean.parseBoolean(properties.getProperty("openMenuOnLogin", "false"));

            properties.setProperty("deviceWebSocketPort", Integer.toString(resolveDevicePort()));
            properties.setProperty("openMenuOnLogin", Boolean.toString(openMenuOnLogin));
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                properties.store(writer, "DG-LAB MC startup settings");
            }
        } catch (IOException ignored) {
            deviceWebSocketPort = CURRENT_DEFAULT_PORT;
            openMenuOnLogin = false;
        }
    }

    public static boolean shouldOpenMenuOnLogin() {
        return openMenuOnLogin;
    }

    public static int resolveDevicePort() {
        return deviceWebSocketPort == LEGACY_DEFAULT_PORT ? CURRENT_DEFAULT_PORT : deviceWebSocketPort;
    }

    private static int parsePort(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            if (parsed < 1024 || parsed > 65535) {
                return defaultValue;
            }
            return parsed;
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }
}
