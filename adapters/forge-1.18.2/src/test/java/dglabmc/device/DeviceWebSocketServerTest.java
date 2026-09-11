package dglabmc.device;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceWebSocketServerTest {
    @Test
    void serverAcceptsWebSocketHandshake() throws Exception {
        DeviceSessionManager sessionManager = new DeviceSessionManager();
        DeviceWebSocketServer server = new DeviceWebSocketServer(sessionManager, "fallback-client");
        WebSocket socket = null;
        try {
            int port = 21734;
            server.start("127.0.0.1", port);
            assertEquals(port, server.getBoundPort(), "服务应报告已绑定端口");

            RecordingListener listener = new RecordingListener();
            socket = HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .buildAsync(URI.create("ws://127.0.0.1:" + port + "/client-123"), listener)
                .join();

            listener.awaitOpen();
            assertTrue(sessionManager.isConnected(), "握手完成后应存在活动连接");
            DeviceSessionManager.DeviceSnapshot snapshot = sessionManager.snapshot();
            assertEquals("client-123", snapshot.clientId, "请求路径中的 clientId 应被识别");
            assertFalse(snapshot.targetId == null || snapshot.targetId.trim().isEmpty(), "握手后应生成 targetId");
            assertFalse(snapshot.recentMessages.isEmpty(), "握手后应记录服务端消息");
        } finally {
            if (socket != null) {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "bye").join();
            }
            server.stop();
            sessionManager.shutdown();
        }
    }

    private static final class RecordingListener implements WebSocket.Listener {
        private final CompletableFuture<Void> opened = new CompletableFuture<Void>();
        private final List<String> messages = new ArrayList<String>();

        @Override
        public void onOpen(WebSocket webSocket) {
            opened.complete(null);
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            messages.add(data.toString());
            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        void awaitOpen() {
            opened.join();
        }
    }
}
