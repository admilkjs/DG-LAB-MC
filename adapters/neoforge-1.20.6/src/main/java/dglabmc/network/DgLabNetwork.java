package dglabmc.network;

import dglabmc.DgLabMcMod;
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

/** NeoForge 1.20.6 custom-payload transport for multiplayer state. */
public final class DgLabNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private DgLabNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(PROTOCOL_VERSION)
            .optional()
            .playToServer(ClientStatePayload.TYPE, ClientStatePayload.STREAM_CODEC, DgLabNetwork::handleClientState)
            .playToClient(ServerStatePayload.TYPE, ServerStatePayload.STREAM_CODEC, DgLabNetwork::handleServerState)
            .playToClient(RemoveStatePayload.TYPE, RemoveStatePayload.STREAM_CODEC, DgLabNetwork::handleRemoveState);
    }

    public static boolean canSendToServer() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getConnection() != null && minecraft.getConnection().hasChannel(ClientStatePayload.TYPE);
    }

    public static void sendLocalStateToServer(PlayerChannelState state) {
        if (state != null && canSendToServer()) {
            PacketDistributor.sendToServer(new ClientStatePayload(state.strengthA, state.strengthB, state.outputA, state.outputB));
        }
    }

    public static void sendKnownStatesToPlayer(ServerPlayer player, Collection<PlayerChannelState> states) {
        if (player == null || states == null || states.isEmpty() || !hasChannel(player)) {
            return;
        }
        for (PlayerChannelState state : states) {
            PacketDistributor.sendToPlayer(player, new ServerStatePayload(state.playerId, state.strengthA, state.strengthB, state.outputA, state.outputB));
        }
    }

    public static void broadcastState(ServerPlayer sender, PlayerChannelState state) {
        if (sender == null || state == null || sender.server == null) {
            return;
        }
        for (ServerPlayer target : sender.server.getPlayerList().getPlayers()) {
            if (hasChannel(target)) {
                PacketDistributor.sendToPlayer(target, new ServerStatePayload(state.playerId, state.strengthA, state.strengthB, state.outputA, state.outputB));
            }
        }
    }

    public static void broadcastRemoval(ServerPlayer sender, UUID playerId) {
        if (sender == null || playerId == null || sender.server == null) {
            return;
        }
        for (ServerPlayer target : sender.server.getPlayerList().getPlayers()) {
            if (hasChannel(target)) {
                PacketDistributor.sendToPlayer(target, new RemoveStatePayload(playerId));
            }
        }
    }

    private static boolean hasChannel(ServerPlayer player) {
        return player.connection != null && player.connection.hasChannel(ClientStatePayload.TYPE);
    }

    private static void handleClientState(ClientStatePayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            ServerPlayerStateRelay.handleClientState(player, payload.toState(player.getUUID()));
        }
    }

    private static void handleServerState(ServerStatePayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        ClientPlayerStateCache.applySyncedState(payload.toState());
    }

    private static void handleRemoveState(RemoveStatePayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        ClientPlayerStateCache.removeSyncedState(payload.playerId);
    }

    public record ClientStatePayload(int strengthA, int strengthB, boolean outputA, boolean outputB) implements CustomPacketPayload {
        public static final Type<ClientStatePayload> TYPE = new Type<>(new ResourceLocation(DgLabMcMod.MODID, "client_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ClientStatePayload::strengthA,
            ByteBufCodecs.VAR_INT, ClientStatePayload::strengthB,
            ByteBufCodecs.BOOL, ClientStatePayload::outputA,
            ByteBufCodecs.BOOL, ClientStatePayload::outputB,
            ClientStatePayload::new
        );

        @Override
        public Type<ClientStatePayload> type() {
            return TYPE;
        }

        private PlayerChannelState toState(UUID playerId) {
            return new PlayerChannelState(playerId, strengthA, strengthB, outputA, outputB);
        }
    }

    public record ServerStatePayload(UUID playerId, int strengthA, int strengthB, boolean outputA, boolean outputB) implements CustomPacketPayload {
        public static final Type<ServerStatePayload> TYPE = new Type<>(new ResourceLocation(DgLabMcMod.MODID, "server_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ServerStatePayload> STREAM_CODEC = StreamCodec.composite(
            UUID_STREAM_CODEC, ServerStatePayload::playerId,
            ByteBufCodecs.VAR_INT, ServerStatePayload::strengthA,
            ByteBufCodecs.VAR_INT, ServerStatePayload::strengthB,
            ByteBufCodecs.BOOL, ServerStatePayload::outputA,
            ByteBufCodecs.BOOL, ServerStatePayload::outputB,
            ServerStatePayload::new
        );

        @Override
        public Type<ServerStatePayload> type() {
            return TYPE;
        }

        private PlayerChannelState toState() {
            return new PlayerChannelState(playerId, strengthA, strengthB, outputA, outputB);
        }
    }

    public record RemoveStatePayload(UUID playerId) implements CustomPacketPayload {
        public static final Type<RemoveStatePayload> TYPE = new Type<>(new ResourceLocation(DgLabMcMod.MODID, "remove_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RemoveStatePayload> STREAM_CODEC = StreamCodec.composite(
            UUID_STREAM_CODEC, RemoveStatePayload::playerId,
            RemoveStatePayload::new
        );

        @Override
        public Type<RemoveStatePayload> type() {
            return TYPE;
        }
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_STREAM_CODEC = StreamCodec.of(
        (buffer, value) -> buffer.writeUUID(value),
        buffer -> buffer.readUUID()
    );
}