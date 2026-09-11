package dglabmc.device;

import io.netty.channel.Channel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class DeviceSession {
    public final Channel channel;
    public final String clientId;
    public final String targetId;
    public boolean bound;
    public long connectedAt = System.currentTimeMillis();
    public int currentStrengthA;
    public int currentStrengthB;
    public int maxStrengthA;
    public int maxStrengthB;
    public final Deque<String> recentMessages = new ArrayDeque<String>();

    public DeviceSession(Channel channel, String clientId, String targetId) {
        this.channel = channel;
        this.clientId = clientId;
        this.targetId = targetId;
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
