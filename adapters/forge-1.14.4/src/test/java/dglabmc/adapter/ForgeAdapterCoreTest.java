package dglabmc.adapter;

import dglabmc.core.rule.TriggerRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies that this legacy adapter compiles against the shared Core source tree. */
class ForgeAdapterCoreTest {
    @Test
    void sharedCoreTriggersAreAvailable() {
        assertTrue(TriggerRegistry.isKnown(TriggerRegistry.PLAYER_HURT));
        assertTrue(TriggerRegistry.isKnown(TriggerRegistry.ATTACK_CRITICAL_ALL));
    }
}
