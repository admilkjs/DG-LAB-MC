package cn.admilk.dglabweb.platform;

import java.nio.file.Path;

public interface PlatformPaths {
    Path resolveConfigDirectory(String modId);
}
