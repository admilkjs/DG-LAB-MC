package dglabmc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dglabmc.client.ClientHooks;
import dglabmc.multiplayer.ServerPlayerStateRelay;
import dglabmc.network.DgLabNetwork;
import dglabmc.config.StartupConfig;
import dglabmc.platform.NoopPlatformClientBridge;
import dglabmc.platform.PlatformServices;
import dglabmc.platform.forge.ForgeCommandRegistrar;
import dglabmc.platform.forge.ForgePlatformPaths;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
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
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
            () -> new IExtensionPoint.DisplayTest(() -> "ANY", (remote, isServer) -> true));

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(DgLabNetwork::register);
        NeoForge.EVENT_BUS.register(ClientHooks.class);
        NeoForge.EVENT_BUS.register(ForgeCommandRegistrar.class);
        NeoForge.EVENT_BUS.register(ServerPlayerStateRelay.class);
        if (FMLEnvironment.dist.isClient()) {
            dglabmc.client.ForgeClientBootstrap.init();
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(new Runnable() {
            @Override
            public void run() {
                if (!FMLEnvironment.dist.isDedicatedServer()) {
                    AppServices.get().initialize();
                }
            }
        });
    }
}
