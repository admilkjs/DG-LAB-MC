package dglabmc.core;

import dglabmc.core.config.AppConfig;
import dglabmc.core.config.ConfigRepository;
import dglabmc.core.rule.TriggerRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigRepositoryTest {
    @Test
    void createsAndNormalizesDefaultConfiguration() throws Exception {
        Path root = Files.createTempDirectory("dglabmc-core-");
        ConfigRepository repository = new ConfigRepository(root);
        AppConfig config = repository.load();
        assertEquals(AppConfig.CURRENT_SCHEMA_VERSION, config.schemaVersion);
        assertNotNull(config.connection.deviceClientId);
        assertFalse(config.connection.deviceClientId.isEmpty());
        assertTrue(config.rules.stream().anyMatch(rule -> TriggerRegistry.PLAYER_HURT.equals(rule.trigger)));
        assertTrue(Files.exists(root.resolve("app-config.json")));
    }
}
