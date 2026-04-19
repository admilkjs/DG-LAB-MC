package cn.admilk.dglabweb;

import cn.admilk.dglabweb.client.ClientHooks;
import cn.admilk.dglabweb.config.StartupConfig;
import cn.admilk.dglabweb.platform.PlatformServices;
import cn.admilk.dglabweb.platform.forge.ForgePlatformClientBridge;
import cn.admilk.dglabweb.platform.forge.ForgeCommandRegistrar;
import cn.admilk.dglabweb.platform.forge.ForgePlatformPaths;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(DgLabWebMod.MODID)
public class DgLabWebMod {
    public static final String MODID = "dglabweb";
    public static final String VERSION = "0.1.0";
    public static final Logger LOGGER = LogManager.getLogger();
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public DgLabWebMod() {
        PlatformServices.configure(new ForgePlatformPaths(), new ForgePlatformClientBridge());
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, StartupConfig.SPEC);
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST, () ->
            Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (remote, server) -> true)
        );

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        MinecraftForge.EVENT_BUS.register(ClientHooks.class);
        MinecraftForge.EVENT_BUS.register(ForgeCommandRegistrar.class);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                AppServices.get().shutdown();
            }
        }, "dglabweb-shutdown"));
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(new Runnable() {
            @Override
            public void run() {
                AppServices.get().initialize();
            }
        });
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(new Runnable() {
            @Override
            public void run() {
                ClientHooks.registerKeyBinding();
            }
        });
    }
}
