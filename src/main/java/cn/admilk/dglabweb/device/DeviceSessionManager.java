package cn.admilk.dglabweb.device;

import cn.admilk.dglabweb.DgLabWebMod;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeviceSessionManager {
    private static final Pattern STRENGTH_PATTERN = Pattern.compile("strength-(\\d+)\\+(\\d+)\\+(\\d+)\\+(\\d+)");

    private final Gson gson = DgLabWebMod.GSON;
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "dglabweb-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    private volatile DeviceSession activeSession;

    public DeviceSessionManager() {
        this.heartbeatExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                sendHeartbeat();
            }
        }, 45L, 45L, TimeUnit.SECONDS);
    }

    public synchronized DeviceSession getActiveSession() {
        return activeSession;
    }

    public synchronized void onHandshakeComplete(Channel channel, String requestUri, String fallbackClientId) {
        String clientId = extractClientId(requestUri, fallbackClientId);
        DeviceSession session = new DeviceSession(channel, clientId, UUID.randomUUID().toString());
        this.activeSession = session;
        session.pushMessage("Connected: " + requestUri);
        sendRaw(session, new DeviceMessage("bind", session.targetId, "", "targetId"));
    }

    public synchronized void onMessage(Channel channel, String rawJson) {
        DeviceSession session = this.activeSession;
        if (session == null || session.channel != channel) {
            return;
        }
        session.pushMessage("IN " + rawJson);

        DeviceMessage message;
        try {
            message = gson.fromJson(rawJson, DeviceMessage.class);
        } catch (JsonSyntaxException exception) {
            sendRaw(session, new DeviceMessage("error", "", "", "403"));
            return;
        }

        if (message == null || message.type == null) {
            sendRaw(session, new DeviceMessage("error", "", "", "403"));
            return;
        }

        if ("bind".equalsIgnoreCase(message.type)) {
            session.bound = true;
            sendRaw(session, new DeviceMessage("bind", message.clientId, message.targetId, "200"));
            return;
        }

        if ("msg".equalsIgnoreCase(message.type) && message.message != null) {
            Matcher matcher = STRENGTH_PATTERN.matcher(message.message);
            if (matcher.find()) {
                session.currentStrengthA = parseSafe(matcher.group(1));
                session.currentStrengthB = parseSafe(matcher.group(2));
                session.maxStrengthA = parseSafe(matcher.group(3));
                session.maxStrengthB = parseSafe(matcher.group(4));
            }
        }
    }

    public synchronized void onDisconnect(Channel channel) {
        if (this.activeSession != null && this.activeSession.channel == channel) {
            this.activeSession = null;
        }
    }

    public synchronized void shutdown() {
        DeviceSession session = this.activeSession;
        if (session != null && session.channel.isOpen()) {
            sendRaw(session, new DeviceMessage("break", session.clientId, session.targetId, "209"));
            session.channel.close();
        }
        this.activeSession = null;
        this.heartbeatExecutor.shutdownNow();
    }

    public synchronized boolean isConnected() {
        return this.activeSession != null && this.activeSession.channel.isOpen();
    }

    public synchronized boolean isBound() {
        return this.activeSession != null && this.activeSession.channel.isOpen() && this.activeSession.bound;
    }

    public synchronized void setStrength(DeviceChannel channel, int value) {
        sendProtocolMessage("strength-" + channel.getNumericId() + "+2+" + clamp(channel, value));
    }

    public synchronized void addStrength(DeviceChannel channel, int value) {
        sendProtocolMessage("strength-" + channel.getNumericId() + "+1+" + clamp(channel, value));
    }

    public synchronized void reduceStrength(DeviceChannel channel, int value) {
        sendProtocolMessage("strength-" + channel.getNumericId() + "+0+" + clamp(channel, value));
    }

    public synchronized void clearPulse(DeviceChannel channel) {
        sendProtocolMessage("clear-" + channel.getNumericId());
    }

    public synchronized void sendPulse(DeviceChannel channel, List<String> frames, boolean clearBeforeSend) {
        if (clearBeforeSend) {
            clearPulse(channel);
        }
        StringBuilder payload = new StringBuilder();
        payload.append("pulse-").append(channel.getProtocolName()).append(":[");
        for (int i = 0; i < frames.size(); i++) {
            if (i > 0) {
                payload.append(',');
            }
            payload.append('"').append(frames.get(i)).append('"');
        }
        payload.append(']');
        sendProtocolMessage(payload.toString());
    }

    public synchronized DeviceSnapshot snapshot() {
        DeviceSession session = this.activeSession;
        DeviceSnapshot snapshot = new DeviceSnapshot();
        snapshot.connected = session != null && session.channel.isOpen();
        if (session != null) {
            snapshot.bound = session.bound;
            snapshot.clientId = session.clientId;
            snapshot.targetId = session.targetId;
            snapshot.currentStrengthA = session.currentStrengthA;
            snapshot.currentStrengthB = session.currentStrengthB;
            snapshot.maxStrengthA = session.maxStrengthA;
            snapshot.maxStrengthB = session.maxStrengthB;
            snapshot.recentMessages = session.recentMessages();
        } else {
            snapshot.recentMessages = new ArrayList<String>();
        }
        return snapshot;
    }

    private void sendHeartbeat() {
        DeviceSession session = this.activeSession;
        if (session == null || !session.channel.isOpen()) {
            return;
        }
        sendRaw(session, new DeviceMessage("heartbeat", session.clientId, session.targetId, "200"));
    }

    private void sendProtocolMessage(String payload) {
        DeviceSession session = this.activeSession;
        if (session == null || !session.channel.isOpen()) {
            return;
        }
        sendRaw(session, new DeviceMessage("msg", session.clientId, session.targetId, payload));
    }

    private void sendRaw(DeviceSession session, DeviceMessage message) {
        String serialized = gson.toJson(message);
        session.pushMessage("OUT " + serialized);
        session.channel.writeAndFlush(new TextWebSocketFrame(serialized));
    }

    private String extractClientId(String requestUri, String fallbackClientId) {
        if (requestUri == null || requestUri.trim().isEmpty() || "/".equals(requestUri.trim())) {
            return fallbackClientId;
        }
        String sanitized = requestUri.trim();
        if (sanitized.startsWith("/")) {
            sanitized = sanitized.substring(1);
        }
        if (sanitized.endsWith("/")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        return sanitized.isEmpty() ? fallbackClientId : sanitized;
    }

    private int parseSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(200, value));
    }

    private int clamp(DeviceChannel channel, int value) {
        return Math.max(0, Math.min(resolveDeviceCap(channel), clamp(value)));
    }

    private int resolveDeviceCap(DeviceChannel channel) {
        DeviceSession session = this.activeSession;
        if (session == null || !session.bound) {
            return 200;
        }
        int reportedMax;
        switch (channel) {
            case B:
                reportedMax = session.maxStrengthB;
                break;
            case A:
            default:
                reportedMax = session.maxStrengthA;
                break;
        }
        return reportedMax > 0 ? Math.min(reportedMax, 200) : 200;
    }

    public static class DeviceSnapshot {
        public boolean connected;
        public boolean bound;
        public String clientId = "";
        public String targetId = "";
        public int currentStrengthA;
        public int currentStrengthB;
        public int maxStrengthA;
        public int maxStrengthB;
        public List<String> recentMessages;
    }
}
