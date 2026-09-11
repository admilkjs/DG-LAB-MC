package dglabmc.device;

import dglabmc.core.device.DeviceChannel;
import dglabmc.core.device.DeviceSnapshot;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ForgeDeviceRuntimeTest {
    @Test
    void mapsCoreTransportCommandsToLegacyNettySession() {
        DeviceSessionManager manager = new DeviceSessionManager();
        DeviceWebSocketServer server = new DeviceWebSocketServer(manager, "test-client");
        ForgeDeviceRuntime runtime = new ForgeDeviceRuntime(manager, server);
        EmbeddedChannel channel = new EmbeddedChannel();
        try {
            assertSame(server, runtime.server());
            assertFalse(runtime.snapshot().connected);
            manager.onHandshakeComplete(channel, "/test-client", "fallback");
            releaseFrame(channel.readOutbound());
            manager.onMessage(channel, "{\"type\":\"bind\",\"clientId\":\"test-client\",\"targetId\":\"test-target\",\"message\":\"200\"}");
            releaseFrame(channel.readOutbound());
            assertTrue(runtime.isBound());
            runtime.setStrength(DeviceChannel.A, 25);
            TextWebSocketFrame frame = channel.readOutbound();
            assertNotNull(frame);
            try { assertTrue(frame.text().contains("strength-1+2+25")); }
            finally { frame.release(); }
            DeviceSnapshot snapshot = runtime.snapshot();
            assertTrue(snapshot.connected);
            assertTrue(snapshot.bound);
            assertEquals("test-client", snapshot.clientId);
        } finally {
            runtime.shutdown();
            channel.finishAndReleaseAll();
        }
    }

    private static void releaseFrame(Object message) {
        if (message instanceof TextWebSocketFrame) ((TextWebSocketFrame) message).release();
    }
}
