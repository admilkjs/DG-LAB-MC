package dglabmc.core.device;

/** Immutable snapshot of channel output state shared between core and platform adapters. */
public final class DeviceChannelState {
    public final DeviceChannel channel;
    public final int currentStrength;
    public final int maxStrength;
    public final boolean outputActive;

    public DeviceChannelState(DeviceChannel channel, int currentStrength, int maxStrength, boolean outputActive) {
        this.channel = channel;
        this.currentStrength = currentStrength;
        this.maxStrength = maxStrength;
        this.outputActive = outputActive;
    }
}
