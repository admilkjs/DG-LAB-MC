package cn.admilk.dglabweb.platform.forge;

import cn.admilk.dglabweb.platform.PlatformPaths;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ForgePlatformPaths implements PlatformPaths {
    @Override
    public Path resolveConfigDirectory(String modId) {
        return FMLPaths.CONFIGDIR.get().resolve(modId);
    }
}
