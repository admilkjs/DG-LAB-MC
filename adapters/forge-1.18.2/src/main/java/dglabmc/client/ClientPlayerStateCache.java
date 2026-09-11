package dglabmc.client;

import dglabmc.AppServices;
import dglabmc.device.DeviceSessionManager;
import dglabmc.core.multiplayer.PlayerChannelState;
import dglabmc.network.DgLabNetwork;
import dglabmc.core.rule.RuleEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientPlayerStateCache {
    private static final Map<UUID, PlayerChannelState> SYNCED_STATES = new LinkedHashMap<UUID, PlayerChannelState>();
    private static PlayerChannelState localState;
    private static PlayerChannelState lastSentState;
    private static long lastSentTick;

    private ClientPlayerStateCache() {
    }

    public static synchronized void tick(LocalPlayer player, long clientTick) {
        if (player == null) {
            reset();
            return;
        }
        localState = captureLocalState(player);
        if (!DgLabNetwork.canSendToServer() || localState == null) {
            return;
        }
        if (shouldSend(localState, clientTick)) {
            DgLabNetwork.sendLocalStateToServer(localState);
            lastSentState = localState;
            lastSentTick = clientTick;
        }
    }

    public static synchronized void applySyncedState(PlayerChannelState state) {
        if (state != null) {
            SYNCED_STATES.put(state.playerId, state);
        }
    }

    public static synchronized void removeSyncedState(UUID playerId) {
        if (playerId != null) {
            SYNCED_STATES.remove(playerId);
        }
    }

    public static synchronized void reset() {
        SYNCED_STATES.clear();
        localState = null;
        lastSentState = null;
        lastSentTick = 0L;
    }

    public static synchronized PlayerChannelState getDisplayState(Player player) {
        if (player == null) {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && player.getUUID().equals(minecraft.player.getUUID()) && localState != null) {
            return localState;
        }
        return SYNCED_STATES.get(player.getUUID());
    }

    private static boolean shouldSend(PlayerChannelState state, long clientTick) {
        if (lastSentState == null) {
            return true;
        }
        if (!state.sameVisualState(lastSentState)) {
            return true;
        }
        return clientTick - lastSentTick >= 40L;
    }

    private static PlayerChannelState captureLocalState(LocalPlayer player) {
        RuleEngine.RuntimeSnapshot runtime = AppServices.get().getRuleRuntimeSnapshot();
        DeviceSessionManager.DeviceSnapshot device = AppServices.get().getDeviceSnapshot();
        int strengthA = device.bound ? device.currentStrengthA : runtime.channelA.currentStrength;
        int strengthB = device.bound ? device.currentStrengthB : runtime.channelB.currentStrength;
        return new PlayerChannelState(
            player.getUUID(),
            strengthA,
            strengthB,
            runtime.channelA.outputActive,
            runtime.channelB.outputActive
        );
    }
}


