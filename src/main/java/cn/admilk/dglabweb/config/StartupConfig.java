package cn.admilk.dglabweb.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class StartupConfig {
    private static final int LEGACY_DEFAULT_PORT = 27852;
    private static final int CURRENT_DEFAULT_PORT = 21733;
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue DEVICE_WS_PORT;
    public static final ForgeConfigSpec.BooleanValue OPEN_MENU_ON_LOGIN;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("ui");
        OPEN_MENU_ON_LOGIN = builder
            .comment("客户端进入世界后自动打开 DG-LAB 控制中心。")
            .define("openMenuOnLogin", false);
        builder.pop();

        builder.push("device");
        DEVICE_WS_PORT = builder
            .comment("提供给 DG-LAB App 使用的 WebSocket 端口。")
            .defineInRange("deviceWebSocketPort", CURRENT_DEFAULT_PORT, 1024, 65535);
        builder.pop();
        SPEC = builder.build();
    }

    private StartupConfig() {
    }

    public static int resolveDevicePort() {
        int configured = DEVICE_WS_PORT.get().intValue();
        return configured == LEGACY_DEFAULT_PORT ? CURRENT_DEFAULT_PORT : configured;
    }
}
