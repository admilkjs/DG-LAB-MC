package dglabmc.core.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncryptedChatSignalTest {
    @Test
    void extractsPlainDglabPayloadWithoutEscapedDollar() {
        String message = "DGLAB$oTRSUlT_f5LiuETiSkmYAlQ9SC-9BOsKGr3yMcdJeh1Yc2NkZVS_isQIygonvTfd";

        assertEquals("oTRSUlT_f5LiuETiSkmYAlQ9SC-9BOsKGr3yMcdJeh1Yc2NkZVS_isQIygonvTfd",
            EncryptedChatSignal.extractEncodedPayload(message));

        String generated = EncryptedChatSignal.createSignal("ju_jiang", "player_hurt");
        assertTrue(generated.startsWith("DGLAB$"), "信号前缀必须使用普通美元符号");
        assertEquals("player_hurt", EncryptedChatSignal.extractSignalTrigger(generated, "ju_jiang"));
    }
}
