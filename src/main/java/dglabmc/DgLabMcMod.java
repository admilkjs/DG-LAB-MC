package dglabmc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dglabmc.client.ClientHooks;
import dglabmc.config.StartupConfig;
import dglabmc.platform.PlatformServices;
import dglabmc.platform.forge.ForgeCommandRegistrar;
import dglabmc.platform.forge.ForgePlatformClientBridge;
import dglabmc.platform.forge.ForgePlatformPaths;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = DgLabMcMod.MODID,
    name = "DG-LAB MC",
    version = DgLabMcMod.VERSION,
    clientSideOnly = true,
    acceptedMinecraftVersions = "[1.12.2]"
)
public class DgLabMcMod {
    public static final String MODID = "dglabmc";
    public static final String VERSION = "1.0.1";
    public static final Logger LOGGER = LogManager.getLogger();
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        PlatformServices.configure(new ForgePlatformPaths(event.getModConfigurationDirectory().toPath()), new ForgePlatformClientBridge());
        StartupConfig.load(PlatformServices.paths().resolveConfigDirectory(MODID));
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                AppServices.get().shutdown();
            }
        }, "dglabmc-shutdown"));
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        AppServices.get().initialize();
        ClientHooks.registerKeyBinding();
        ForgeCommandRegistrar.register();
        MinecraftForge.EVENT_BUS.register(new ClientHooks());
        MinecraftForge.EVENT_BUS.register(new dglabmc.client.ClientHudOverlay());
    }
}
