package dglabmc.client;

import dglabmc.platform.PlatformServices;
import dglabmc.platform.forge.ForgePlatformClientBridge;
import dglabmc.platform.forge.ForgePlatformPaths;

/** Client-only bootstrap kept behind Forge's DistExecutor boundary. */
public final class ForgeClientBootstrap {
    private ForgeClientBootstrap() { }

    public static void init() {
        PlatformServices.configure(new ForgePlatformPaths(), new ForgePlatformClientBridge());
        ClientHooks.registerKeyBinding();
    }
}
