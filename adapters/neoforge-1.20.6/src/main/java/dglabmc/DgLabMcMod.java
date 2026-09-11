package dglabmc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dglabmc.client.ForgeClientBootstrap;
import dglabmc.multiplayer.ServerPlayerStateRelay;
import dglabmc.network.DgLabNetwork;
import dglabmc.config.StartupConfig;
import dglabmc.platform.PlatformServices;
import dglabmc.platform.NoopPlatformClientBridge;
import dglabmc.platform.forge.ForgeCommandRegistrar;
import dglabmc.platform.forge.ForgePlatformPaths;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
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

    public DgLabMcMod(IEventBus modEventBus, ModContainer modContainer) {
        PlatformServices.configure(new ForgePlatformPaths(), new NoopPlatformClientBridge());
        modContainer.registerConfig(ModConfig.Type.CLIENT, StartupConfig.SPEC);

        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(DgLabNetwork::register);
        NeoForge.EVENT_BUS.register(ServerPlayerStateRelay.class);
        if (FMLEnvironment.dist.isClient()) {
            NeoForge.EVENT_BUS.register(ForgeCommandRegistrar.class);
            ForgeClientBootstrap.init();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                AppServices.get().shutdown();
            }
        }, "dglabmc-shutdown"));
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(new Runnable() {
            @Override
            public void run() {
                if (FMLEnvironment.dist.isClient()) {
                    AppServices.get().initialize();
                }
            }
        });
    }

}
