package dglabmc.multiplayer;

import dglabmc.network.DgLabNetwork;
import dglabmc.core.multiplayer.PlayerChannelState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ServerPlayerStateRelay {
    private static final Map<UUID, PlayerChannelState> KNOWN_STATES = new LinkedHashMap<UUID, PlayerChannelState>();

    private ServerPlayerStateRelay() {
    }

    public static synchronized void handleClientState(ServerPlayer player, PlayerChannelState incomingState) {
        if (player == null || incomingState == null) {
            return;
        }
        PlayerChannelState normalized = new PlayerChannelState(player.getUUID(), incomingState.strengthA, incomingState.strengthB, incomingState.outputA, incomingState.outputB);
        PlayerChannelState previous = KNOWN_STATES.put(player.getUUID(), normalized);
        if (normalized.sameVisualState(previous)) {
            return;
        }
        DgLabNetwork.broadcastState(player, normalized);
    }

    @SubscribeEvent
    public static synchronized void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DgLabNetwork.sendKnownStatesToPlayer(player, new ArrayList<PlayerChannelState>(KNOWN_STATES.values()));
        }
    }

    @SubscribeEvent
    public static synchronized void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KNOWN_STATES.remove(player.getUUID());
            DgLabNetwork.broadcastRemoval(player, player.getUUID());
        }
    }
}


