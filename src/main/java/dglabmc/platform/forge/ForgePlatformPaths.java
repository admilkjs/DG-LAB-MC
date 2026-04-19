package dglabmc.platform.forge;

import dglabmc.platform.PlatformPaths;

import java.nio.file.Path;

public class ForgePlatformPaths implements PlatformPaths {
    private final Path configRoot;

    public ForgePlatformPaths(Path configRoot) {
        this.configRoot = configRoot;
    }

    @Override
    public Path resolveConfigDirectory(String modId) {
        return this.configRoot.resolve(modId);
    }
}
