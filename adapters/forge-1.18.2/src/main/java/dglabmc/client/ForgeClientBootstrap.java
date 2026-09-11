package dglabmc.client;

import dglabmc.platform.PlatformServices;
import dglabmc.platform.forge.ForgePlatformClientBridge;

public final class ForgeClientBootstrap {
    private ForgeClientBootstrap() {
    }

    public static void init() {
        PlatformServices.configureClientBridge(new ForgePlatformClientBridge());
    }
}


