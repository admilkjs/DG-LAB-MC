package dglabmc.platform;

public final class PlatformServices {
    private static PlatformPaths paths;
    private static PlatformClientBridge clientBridge;

    private PlatformServices() {
    }

    public static synchronized void configure(PlatformPaths platformPaths, PlatformClientBridge bridge) {
        paths = platformPaths;
        clientBridge = bridge;
    }

    public static synchronized PlatformPaths paths() {
        if (paths == null) {
            throw new IllegalStateException("平台路径服务尚未配置。");
        }
        return paths;
    }

    public static synchronized PlatformClientBridge client() {
        if (clientBridge == null) {
            throw new IllegalStateException("平台客户端桥接尚未配置。");
        }
        return clientBridge;
    }
}
