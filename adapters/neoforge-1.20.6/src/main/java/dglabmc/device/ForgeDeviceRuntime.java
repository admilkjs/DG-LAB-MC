package dglabmc.device;

import dglabmc.core.device.DeviceChannel;
import dglabmc.core.device.DeviceChannelState;
import dglabmc.core.device.DeviceRuntime;
import dglabmc.core.device.DeviceServer;
import dglabmc.core.device.DeviceSnapshot;

import java.util.ArrayList;
import java.util.List;

/** NeoForge 1.20.6 composition of the Netty device session and server. */
public final class ForgeDeviceRuntime implements DeviceRuntime {
    private final DeviceSessionManager sessionManager;
    private final DeviceWebSocketServer server;

    public ForgeDeviceRuntime(DeviceSessionManager sessionManager, DeviceWebSocketServer server) {
        this.sessionManager = sessionManager;
        this.server = server;
    }

    @Override
    public DeviceServer server() {
        return server;
    }

    @Override
    public DeviceSnapshot snapshot() {
        DeviceSessionManager.DeviceSnapshot source = sessionManager.snapshot();
        DeviceSnapshot snapshot = new DeviceSnapshot();
        snapshot.connected = source.connected;
        snapshot.bound = source.bound;
        snapshot.clientId = source.clientId;
        snapshot.targetId = source.targetId;
        snapshot.currentStrengthA = source.currentStrengthA;
        snapshot.currentStrengthB = source.currentStrengthB;
        snapshot.maxStrengthA = source.maxStrengthA;
        snapshot.maxStrengthB = source.maxStrengthB;
        snapshot.recentMessages = source.recentMessages == null
            ? new ArrayList<String>()
            : new ArrayList<String>(source.recentMessages);
        return snapshot;
    }

    @Override
    public void shutdown() {
        server.stop();
        sessionManager.shutdown();
    }

    @Override
    public boolean isBound() {
        return sessionManager.isBound();
    }

    @Override
    public void setStrength(DeviceChannel channel, int strength) {
        sessionManager.setStrength(channel, strength);
    }

    @Override
    public void sendPulse(DeviceChannel channel, List<String> frames, boolean clearBeforeSend) {
        sessionManager.sendPulse(channel, frames, clearBeforeSend);
    }

    @Override
    public void clearPulse(DeviceChannel channel) {
        sessionManager.clearPulse(channel);
    }

    @Override
    public DeviceChannelState snapshot(DeviceChannel channel) {
        return sessionManager.snapshot(channel);
    }
}
