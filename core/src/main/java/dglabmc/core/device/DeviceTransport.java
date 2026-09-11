package dglabmc.core.device;

import java.util.List;

/** Platform-neutral transport port used by rule and device services. */
public interface DeviceTransport {
    boolean isBound();
    void setStrength(DeviceChannel channel, int strength);
    void sendPulse(DeviceChannel channel, List<String> frames, boolean clearBeforeSend);
    void clearPulse(DeviceChannel channel);
    DeviceChannelState snapshot(DeviceChannel channel);
}
