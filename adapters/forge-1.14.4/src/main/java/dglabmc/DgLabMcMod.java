package dglabmc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dglabmc.config.StartupConfig;
import dglabmc.platform.NoopPlatformClientBridge;
import dglabmc.platform.PlatformServices;
import dglabmc.platform.forge.ForgePlatformPaths;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(DgLabMcMod.MODID)
public final class DgLabMcMod {
    public static final String MODID = "dglabmc";
    public static final String VERSION = "1.0.1";
    public static final Logger LOGGER = LogManager.getLogger();
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public DgLabMcMod() {
        PlatformServices.configure(new ForgePlatformPaths(), new NoopPlatformClientBridge());
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, StartupConfig.SPEC);
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
            () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (remote, server) -> true));
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> AppServices.get().shutdown(), "dglabmc-shutdown"));
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        AppServices.get().initialize();
    }
}
