package dglabmc.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EncryptedChatSignalTest {
    @Test
    void extractsPlainDglabPayloadWithoutEscapedDollar() {
        String message = "DGLAB$oTRSUlT_f5LiuETiSkmYAlQ9SC-9BOsKGr3yMcdJeh1Yc2NkZVS_isQIygonvTfd";

        assertEquals("oTRSUlT_f5LiuETiSkmYAlQ9SC-9BOsKGr3yMcdJeh1Yc2NkZVS_isQIygonvTfd", EncryptedChatSignal.extractEncodedPayload(message));
        assertEquals("player_hurt", EncryptedChatSignal.extractSignalTrigger(message, "ju_jiang"));
    }
}
