package dglabmc.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ConfigArchiveService {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public Path exportToDefaultLocation(Path rootDirectory, AppConfig config, String modVersion) throws IOException {
        Path exportDirectory = rootDirectory.resolve("exports");
        Files.createDirectories(exportDirectory);
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        Path target = exportDirectory.resolve("dglabmc-config-" + timestamp + ".zip");
        try (OutputStream outputStream = Files.newOutputStream(target)) {
            writeArchive(outputStream, config, modVersion, "manual-export");
        }
        return target;
    }

    public void writeArchive(OutputStream outputStream, AppConfig config, String modVersion) throws IOException {
        writeArchive(outputStream, config, modVersion, "export");
    }

    public Path backupLastConfig(Path rootDirectory, AppConfig config, String modVersion, String reason) throws IOException {
        Path backupDirectory = rootDirectory.resolve("backups");
        Files.createDirectories(backupDirectory);
        Path target = backupDirectory.resolve("last-config-backup.zip");
        try (OutputStream outputStream = Files.newOutputStream(target)) {
            writeArchive(outputStream, config, modVersion, reason);
        }
        return target;
    }

    public void writeArchive(OutputStream outputStream, AppConfig config, String modVersion, String reason) throws IOException {
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8);
        zipOutputStream.putNextEntry(new ZipEntry("manifest.json"));
        OutputStreamWriter manifestWriter = new OutputStreamWriter(zipOutputStream, StandardCharsets.UTF_8);
        gson.toJson(new ArchiveManifest(modVersion, reason), manifestWriter);
        manifestWriter.flush();
        zipOutputStream.closeEntry();

        zipOutputStream.putNextEntry(new ZipEntry("config.json"));
        OutputStreamWriter configWriter = new OutputStreamWriter(zipOutputStream, StandardCharsets.UTF_8);
        gson.toJson(config, configWriter);
        configWriter.flush();
        zipOutputStream.closeEntry();
        zipOutputStream.finish();
    }

    public AppConfig readArchive(InputStream inputStream) throws IOException {
        ZipInputStream zipInputStream = new ZipInputStream(inputStream, StandardCharsets.UTF_8);
        ArchiveManifest manifest = null;
        AppConfig imported = null;
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            byte[] bytes = readAllBytes(zipInputStream);
            if ("manifest.json".equals(entry.getName())) {
                Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8);
                manifest = gson.fromJson(reader, ArchiveManifest.class);
            } else if ("config.json".equals(entry.getName())) {
                Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8);
                imported = gson.fromJson(reader, AppConfig.class);
            }
            zipInputStream.closeEntry();
        }

        if (manifest == null) {
            throw new IOException("压缩包缺少 manifest.json。");
        }
        if (manifest.schemaVersion > AppConfig.CURRENT_SCHEMA_VERSION || manifest.schemaVersion <= 0) {
            throw new IOException("不支持的配置版本： " + manifest.schemaVersion);
        }
        if (imported == null) {
            throw new IOException("压缩包缺少 config.json。");
        }
        return imported;
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    private static final class ArchiveManifest {
        final int schemaVersion;
        final String modVersion;
        final String loaderFlavor;
        final String exportedAt;
        final String reason;

        private ArchiveManifest(String modVersion, String reason) {
            this.schemaVersion = AppConfig.CURRENT_SCHEMA_VERSION;
            this.modVersion = modVersion;
            this.loaderFlavor = "neoforge-1.20.2";
            this.exportedAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date());
            this.reason = reason == null ? "export" : reason;
        }
    }
}
