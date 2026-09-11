package dglabmc.core.device;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Runtime state for one DG-LAB WebSocket connection. */
public final class DeviceSession {
    private final DeviceConnection connection;
    public final String clientId;
    public final String targetId;
    public boolean bound;
    public final long connectedAt = System.currentTimeMillis();
    public int currentStrengthA;
    public int currentStrengthB;
    public int maxStrengthA;
    public int maxStrengthB;
    private final Deque<String> recentMessages = new ArrayDeque<String>();

    public DeviceSession(DeviceConnection connection, String clientId, String targetId) {
        this.connection = connection;
        this.clientId = clientId;
        this.targetId = targetId;
    }

    public DeviceConnection connection() {
        return connection;
    }

    public void pushMessage(String message) {
        recentMessages.addFirst(message);
        while (recentMessages.size() > 12) {
            recentMessages.removeLast();
        }
    }

    public List<String> recentMessages() {
        return new ArrayList<String>(recentMessages);
    }
}
