package dglabmc;

import dglabmc.client.ClientHooks;
import dglabmc.multiplayer.ServerPlayerStateRelay;
import dglabmc.network.DgLabNetwork;
import dglabmc.config.StartupConfig;
import dglabmc.platform.NoopPlatformClientBridge;
import dglabmc.platform.PlatformServices;
import dglabmc.platform.forge.ForgeCommandRegistrar;
import dglabmc.platform.forge.ForgePlatformPaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(DgLabMcMod.MODID)
public class DgLabMcMod {
    public static final String MODID = "dglabmc";
    public static final String VERSION = "1.0.1";
    public static final Logger LOGGER = LogManager.getLogger();
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public DgLabMcMod() {
        PlatformServices.configure(new ForgePlatformPaths(), new NoopPlatformClientBridge());
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, StartupConfig.SPEC);
        ModLoadingContext.get().registerDisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (remote, server) -> true);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        MinecraftForge.EVENT_BUS.register(ForgeCommandRegistrar.class);
        MinecraftForge.EVENT_BUS.register(ServerPlayerStateRelay.class);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> dglabmc.client.ForgeClientBootstrap::init);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(new Runnable() {
            @Override
            public void run() {
                DgLabNetwork.register();
                if (FMLEnvironment.dist != Dist.DEDICATED_SERVER) {
                    AppServices.get().initialize();
                }
            }
        });
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(ClientHooks::registerKeyBinding);
    }
}
