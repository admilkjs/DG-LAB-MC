package dglabmc.core.device;

import java.util.ArrayList;
import java.util.List;

/** Detached view of the currently connected device. */
public final class DeviceSnapshot {
    public boolean connected;
    public boolean bound;
    public String clientId = "";
    public String targetId = "";
    public int currentStrengthA;
    public int currentStrengthB;
    public int maxStrengthA;
    public int maxStrengthB;
    public List<String> recentMessages = new ArrayList<String>();

    public DeviceSnapshot() {
    }

    public DeviceSnapshot(DeviceSnapshot source) {
        if (source == null) {
            return;
        }
        this.connected = source.connected;
        this.bound = source.bound;
        this.clientId = source.clientId == null ? "" : source.clientId;
        this.targetId = source.targetId == null ? "" : source.targetId;
        this.currentStrengthA = source.currentStrengthA;
        this.currentStrengthB = source.currentStrengthB;
        this.maxStrengthA = source.maxStrengthA;
        this.maxStrengthB = source.maxStrengthB;
        this.recentMessages = source.recentMessages == null
            ? new ArrayList<String>()
            : new ArrayList<String>(source.recentMessages);
    }

    /** Returns a disconnected snapshot suitable for an unavailable runtime. */
    public static DeviceSnapshot disconnected() {
        return new DeviceSnapshot();
    }

    /** Returns a defensive copy for callers that retain the value. */
    public DeviceSnapshot copy() {
        return new DeviceSnapshot(this);
    }
}
