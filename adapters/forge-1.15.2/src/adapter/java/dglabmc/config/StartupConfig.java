package dglabmc.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class StartupConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue DEVICE_WS_PORT;
    public static final ForgeConfigSpec.BooleanValue OPEN_MENU_ON_LOGIN;
    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("ui"); OPEN_MENU_ON_LOGIN = b.define("openMenuOnLogin", false); b.pop();
        b.push("device"); DEVICE_WS_PORT = b.defineInRange("deviceWebSocketPort", 21733, 1024, 65535); b.pop();
        SPEC = b.build();
    }
    private StartupConfig() {}
    public static int resolveDevicePort() { return DEVICE_WS_PORT.get(); }
}
