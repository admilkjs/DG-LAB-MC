package dglabmc.network;

import dglabmc.client.ClientPlayerStateCache;
import dglabmc.core.multiplayer.PlayerChannelState;
import dglabmc.multiplayer.ServerPlayerStateRelay;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Collection;
import java.util.UUID;

public final class DgLabNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static boolean registered;
    private static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_STREAM = StreamCodec.of(
        (RegistryFriendlyByteBuf buffer, UUID value) -> buffer.writeUUID(value),
        (RegistryFriendlyByteBuf buffer) -> buffer.readUUID()
    );


    private DgLabNetwork() {
    }

    public static synchronized void register(RegisterPayloadHandlersEvent event) {
        if (registered) {
            return;
        }
        event.registrar(PROTOCOL_VERSION)
            .playToServer(ClientStatePayload.TYPE, ClientStatePayload.CODEC, (payload, context) -> {
                if (context.player() instanceof ServerPlayer player) {
                    ServerPlayerStateRelay.handleClientState(player, payload.toState(player.getUUID()));
                }
            })
            .playToClient(ServerStatePayload.TYPE, ServerStatePayload.CODEC,
                (payload, context) -> ClientPlayerStateCache.applySyncedState(payload.toState()))
            .playToClient(RemoveStatePayload.TYPE, RemoveStatePayload.CODEC,
                (payload, context) -> ClientPlayerStateCache.removeSyncedState(payload.playerId));
        registered = true;
    }

    public static boolean canSendToServer() {
        return Minecraft.getInstance().getConnection() != null;
    }

    public static void sendLocalStateToServer(PlayerChannelState state) {
        if (state != null && canSendToServer()) {
            PacketDistributor.sendToServer(new ClientStatePayload(state));
        }
    }

    public static void sendKnownStatesToPlayer(ServerPlayer player, Collection<PlayerChannelState> states) {
        if (player == null || states == null || states.isEmpty()) {
            return;
        }
        for (PlayerChannelState state : states) {
            PacketDistributor.sendToPlayer(player, new ServerStatePayload(state));
        }
    }

    public static void broadcastState(ServerPlayer sender, PlayerChannelState state) {
        if (sender == null || state == null || sender.server == null) {
            return;
        }
        for (ServerPlayer target : sender.server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(target, new ServerStatePayload(state));
        }
    }

    public static void broadcastRemoval(ServerPlayer sender, UUID playerId) {
        if (sender == null || playerId == null || sender.server == null) {
            return;
        }
        for (ServerPlayer target : sender.server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(target, new RemoveStatePayload(playerId));
        }
    }

    private static final class ClientStatePayload implements CustomPacketPayload {
        private static final Type<ClientStatePayload> TYPE = CustomPacketPayload.createType("dglabmc:client_state");
        private static final StreamCodec<RegistryFriendlyByteBuf, ClientStatePayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, payload -> payload.strengthA,
                ByteBufCodecs.VAR_INT, payload -> payload.strengthB,
                ByteBufCodecs.BOOL, payload -> payload.outputA,
                ByteBufCodecs.BOOL, payload -> payload.outputB,
                ClientStatePayload::new);
        private final int strengthA;
        private final int strengthB;
        private final boolean outputA;
        private final boolean outputB;

        private ClientStatePayload(PlayerChannelState state) {
            this(state.strengthA, state.strengthB, state.outputA, state.outputB);
        }

        private ClientStatePayload(int strengthA, int strengthB, boolean outputA, boolean outputB) {
            this.strengthA = strengthA;
            this.strengthB = strengthB;
            this.outputA = outputA;
            this.outputB = outputB;
        }

        private PlayerChannelState toState(UUID playerId) {
            return new PlayerChannelState(playerId, strengthA, strengthB, outputA, outputB);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static final class ServerStatePayload implements CustomPacketPayload {
        private static final Type<ServerStatePayload> TYPE = CustomPacketPayload.createType("dglabmc:server_state");
        private static final StreamCodec<RegistryFriendlyByteBuf, ServerStatePayload> CODEC =
            StreamCodec.composite(UUID_STREAM, payload -> payload.playerId,
                ByteBufCodecs.VAR_INT, payload -> payload.strengthA,
                ByteBufCodecs.VAR_INT, payload -> payload.strengthB,
                ByteBufCodecs.BOOL, payload -> payload.outputA,
                ByteBufCodecs.BOOL, payload -> payload.outputB,
                ServerStatePayload::new);
        private final UUID playerId;
        private final int strengthA;
        private final int strengthB;
        private final boolean outputA;
        private final boolean outputB;

        private ServerStatePayload(PlayerChannelState state) {
            this(state.playerId, state.strengthA, state.strengthB, state.outputA, state.outputB);
        }

        private ServerStatePayload(UUID playerId, int strengthA, int strengthB, boolean outputA, boolean outputB) {
            this.playerId = playerId;
            this.strengthA = strengthA;
            this.strengthB = strengthB;
            this.outputA = outputA;
            this.outputB = outputB;
        }

        private PlayerChannelState toState() {
            return new PlayerChannelState(playerId, strengthA, strengthB, outputA, outputB);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static final class RemoveStatePayload implements CustomPacketPayload {
        private static final Type<RemoveStatePayload> TYPE = CustomPacketPayload.createType("dglabmc:remove_state");
        private static final StreamCodec<RegistryFriendlyByteBuf, RemoveStatePayload> CODEC =
            UUID_STREAM.map(RemoveStatePayload::new, payload -> payload.playerId);
        private final UUID playerId;

        private RemoveStatePayload(UUID playerId) {
            this.playerId = playerId;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

}

