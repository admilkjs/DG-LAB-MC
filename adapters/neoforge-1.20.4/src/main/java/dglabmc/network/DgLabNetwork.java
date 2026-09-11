package dglabmc.network;

import dglabmc.DgLabMcMod;
import dglabmc.client.ClientPlayerStateCache;
import dglabmc.core.multiplayer.PlayerChannelState;
import dglabmc.multiplayer.ServerPlayerStateRelay;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.connection.ConnectionPhase;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;

import java.util.Collection;
import java.util.UUID;

/** NeoForge 1.20.4 payload registration and state synchronization. */
public final class DgLabNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static boolean registered;

    private DgLabNetwork() {
    }

    public static synchronized void register(RegisterPayloadHandlerEvent event) {
        if (registered) {
            return;
        }
        IPayloadRegistrar registrar = event.registrar(DgLabMcMod.MODID).versioned(PROTOCOL_VERSION).optional();
        registrar.play(ClientStatePacket.TYPE, ClientStatePacket::new, builder -> builder.server(ClientStatePacket::handle));
        registrar.play(ServerStatePacket.TYPE, ServerStatePacket::new, builder -> builder.client(ServerStatePacket::handle));
        registrar.play(RemoveStatePacket.TYPE, RemoveStatePacket::new, builder -> builder.client(RemoveStatePacket::handle));
        registered = true;
    }

    public static boolean canSendToServer() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            return false;
        }
        Connection connection = minecraft.getConnection().getConnection();
        return connection != null && net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance()
            .isConnected(connection, ConnectionPhase.PLAY, ClientStatePacket.TYPE);
    }

    public static void sendLocalStateToServer(PlayerChannelState state) {
        if (state != null && canSendToServer()) {
            PacketDistributor.SERVER.noArg().send(new ClientStatePacket(state));
        }
    }

    public static void sendKnownStatesToPlayer(ServerPlayer player, Collection<PlayerChannelState> states) {
        if (player == null || states == null || states.isEmpty() || !isRemotePresent(player)) {
            return;
        }
        for (PlayerChannelState state : states) {
            PacketDistributor.PLAYER.with(player).send(new ServerStatePacket(state));
        }
    }

    public static void broadcastState(ServerPlayer sender, PlayerChannelState state) {
        if (sender == null || state == null || sender.server == null) {
            return;
        }
        for (ServerPlayer target : sender.server.getPlayerList().getPlayers()) {
            if (isRemotePresent(target)) {
                PacketDistributor.PLAYER.with(target).send(new ServerStatePacket(state));
            }
        }
    }

    public static void broadcastRemoval(ServerPlayer sender, UUID playerId) {
        if (sender == null || playerId == null || sender.server == null) {
            return;
        }
        for (ServerPlayer target : sender.server.getPlayerList().getPlayers()) {
            if (isRemotePresent(target)) {
                PacketDistributor.PLAYER.with(target).send(new RemoveStatePacket(playerId));
            }
        }
    }

    private static boolean isRemotePresent(ServerPlayer player) {
        return player != null && player.connection != null && net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance()
            .isConnected(player.connection.connection, ConnectionPhase.PLAY, ServerStatePacket.TYPE);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(DgLabMcMod.MODID, path);
    }

    private static final class ClientStatePacket implements CustomPacketPayload {
        private static final ResourceLocation TYPE = DgLabNetwork.id("client_state");
        private final int strengthA;
        private final int strengthB;
        private final boolean outputA;
        private final boolean outputB;

        private ClientStatePacket(PlayerChannelState state) {
            this(state.strengthA, state.strengthB, state.outputA, state.outputB);
        }

        private ClientStatePacket(FriendlyByteBuf buffer) {
            this(buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean());
        }

        private ClientStatePacket(int strengthA, int strengthB, boolean outputA, boolean outputB) {
            this.strengthA = strengthA;
            this.strengthB = strengthB;
            this.outputA = outputA;
            this.outputB = outputB;
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(strengthA);
            buffer.writeVarInt(strengthB);
            buffer.writeBoolean(outputA);
            buffer.writeBoolean(outputB);
        }

        private static void handle(ClientStatePacket packet, PlayPayloadContext context) {
            context.workHandler().execute(() -> context.player().filter(ServerPlayer.class::isInstance)
                .map(ServerPlayer.class::cast)
                .ifPresent(sender -> ServerPlayerStateRelay.handleClientState(sender, packet.toState(sender.getUUID()))));
        }

        private PlayerChannelState toState(UUID playerId) {
            return new PlayerChannelState(playerId, strengthA, strengthB, outputA, outputB);
        }
    }

    private static final class ServerStatePacket implements CustomPacketPayload {
        private static final ResourceLocation TYPE = DgLabNetwork.id("server_state");
        private final UUID playerId;
        private final int strengthA;
        private final int strengthB;
        private final boolean outputA;
        private final boolean outputB;

        private ServerStatePacket(PlayerChannelState state) {
            this(state.playerId, state.strengthA, state.strengthB, state.outputA, state.outputB);
        }

        private ServerStatePacket(FriendlyByteBuf buffer) {
            this(buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean());
        }

        private ServerStatePacket(UUID playerId, int strengthA, int strengthB, boolean outputA, boolean outputB) {
            this.playerId = playerId;
            this.strengthA = strengthA;
            this.strengthB = strengthB;
            this.outputA = outputA;
            this.outputB = outputB;
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeUUID(playerId);
            buffer.writeVarInt(strengthA);
            buffer.writeVarInt(strengthB);
            buffer.writeBoolean(outputA);
            buffer.writeBoolean(outputB);
        }

        private static void handle(ServerStatePacket packet, PlayPayloadContext context) {
            context.workHandler().execute(() -> ClientPlayerStateCache.applySyncedState(packet.toState()));
        }

        private PlayerChannelState toState() {
            return new PlayerChannelState(playerId, strengthA, strengthB, outputA, outputB);
        }
    }

    private static final class RemoveStatePacket implements CustomPacketPayload {
        private static final ResourceLocation TYPE = DgLabNetwork.id("remove_state");
        private final UUID playerId;

        private RemoveStatePacket(UUID playerId) {
            this.playerId = playerId;
        }

        private RemoveStatePacket(FriendlyByteBuf buffer) {
            this(buffer.readUUID());
        }

        @Override
        public ResourceLocation id() {
            return TYPE;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeUUID(playerId);
        }

        private static void handle(RemoveStatePacket packet, PlayPayloadContext context) {
            context.workHandler().execute(() -> ClientPlayerStateCache.removeSyncedState(packet.playerId));
        }
    }
}
