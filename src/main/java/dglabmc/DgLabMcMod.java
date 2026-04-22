package dglabmc;

import dglabmc.client.ClientHooks;
import dglabmc.config.StartupConfig;
import dglabmc.platform.PlatformServices;
import dglabmc.platform.forge.ForgePlatformClientBridge;
import dglabmc.platform.forge.ForgeCommandRegistrar;
import dglabmc.platform.forge.ForgePlatformPaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(DgLabMcMod.MODID)
public class DgLabMcMod {
    public static final String MODID = "dglabmc";
    public static final String VERSION = "1.0.1";
    public static final Logger LOGGER = LogManager.getLogger();
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public DgLabMcMod(IEventBus modEventBus, ModContainer modContainer) {
        PlatformServices.configure(new ForgePlatformPaths(), new ForgePlatformClientBridge());
        modContainer.registerConfig(ModConfig.Type.CLIENT, StartupConfig.SPEC);

        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.register(ClientHooks.class);
        NeoForge.EVENT_BUS.register(ForgeCommandRegistrar.class);
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
                AppServices.get().initialize();
            }
        });
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        ClientHooks.registerKeyBindings(event);
    }
}
