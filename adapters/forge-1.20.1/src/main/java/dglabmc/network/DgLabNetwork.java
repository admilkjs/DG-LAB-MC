package dglabmc.network;

import dglabmc.DgLabMcMod;
import dglabmc.client.ClientPlayerStateCache;
import dglabmc.core.multiplayer.PlayerChannelState;
import dglabmc.multiplayer.ServerPlayerStateRelay;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Collection;
import java.util.UUID;
import java.util.function.Supplier;

public final class DgLabNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(DgLabMcMod.MODID, "player_state"),
        () -> PROTOCOL_VERSION,
        NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION),
        NetworkRegistry.acceptMissingOr(PROTOCOL_VERSION)
    );

    private static boolean registered;

    private DgLabNetwork() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        int index = 0;
        CHANNEL.messageBuilder(ClientStatePacket.class, index++, NetworkDirection.PLAY_TO_SERVER)
            .encoder(ClientStatePacket::encode)
            .decoder(ClientStatePacket::decode)
            .consumerMainThread(ClientStatePacket::handle)
            .add();
        CHANNEL.messageBuilder(ServerStatePacket.class, index++, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(ServerStatePacket::encode)
            .decoder(ServerStatePacket::decode)
            .consumerMainThread(ServerStatePacket::handle)
            .add();
        CHANNEL.messageBuilder(RemoveStatePacket.class, index, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(RemoveStatePacket::encode)
            .decoder(RemoveStatePacket::decode)
            .consumerMainThread(RemoveStatePacket::handle)
            .add();
        registered = true;
    }

    public static boolean canSendToServer() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() == null) {
            return false;
        }
        Connection connection = minecraft.getConnection().getConnection();
        return connection != null && CHANNEL.isRemotePresent(connection);
    }

    public static void sendLocalStateToServer(PlayerChannelState state) {
        if (state != null && canSendToServer()) {
            CHANNEL.sendToServer(new ClientStatePacket(state));
        }
    }

    public static void sendKnownStatesToPlayer(ServerPlayer player, Collection<PlayerChannelState> states) {
        if (player == null || states == null || states.isEmpty() || !isRemotePresent(player)) {
            return;
        }
        for (PlayerChannelState state : states) {
            CHANNEL.sendTo(new ServerStatePacket(state), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
        }
    }

    public static void broadcastState(ServerPlayer sender, PlayerChannelState state) {
        if (sender == null || state == null || sender.server == null) {
            return;
        }
        for (ServerPlayer target : sender.server.getPlayerList().getPlayers()) {
            if (isRemotePresent(target)) {
                CHANNEL.sendTo(new ServerStatePacket(state), target.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
            }
        }
    }

    public static void broadcastRemoval(ServerPlayer sender, UUID playerId) {
        if (sender == null || playerId == null || sender.server == null) {
            return;
        }
        for (ServerPlayer target : sender.server.getPlayerList().getPlayers()) {
            if (isRemotePresent(target)) {
                CHANNEL.sendTo(new RemoveStatePacket(playerId), target.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
            }
        }
    }

    private static boolean isRemotePresent(ServerPlayer player) {
        return player != null && player.connection != null && CHANNEL.isRemotePresent(player.connection.connection);
    }

    private static final class ClientStatePacket {
        private final int strengthA;
        private final int strengthB;
        private final boolean outputA;
        private final boolean outputB;

        private ClientStatePacket(PlayerChannelState state) {
            this(state.strengthA, state.strengthB, state.outputA, state.outputB);
        }

        private ClientStatePacket(int strengthA, int strengthB, boolean outputA, boolean outputB) {
            this.strengthA = strengthA;
            this.strengthB = strengthB;
            this.outputA = outputA;
            this.outputB = outputB;
        }

        private static void encode(ClientStatePacket packet, FriendlyByteBuf buffer) {
            buffer.writeVarInt(packet.strengthA);
            buffer.writeVarInt(packet.strengthB);
            buffer.writeBoolean(packet.outputA);
            buffer.writeBoolean(packet.outputB);
        }

        private static ClientStatePacket decode(FriendlyByteBuf buffer) {
            return new ClientStatePacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean());
        }

        private static void handle(ClientStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                ServerPlayerStateRelay.handleClientState(sender, packet.toState(sender.getUUID()));
            }
        }

        private PlayerChannelState toState(UUID playerId) {
            return new PlayerChannelState(playerId, strengthA, strengthB, outputA, outputB);
        }
    }

    private static final class ServerStatePacket {
        private final UUID playerId;
        private final int strengthA;
        private final int strengthB;
        private final boolean outputA;
        private final boolean outputB;

        private ServerStatePacket(PlayerChannelState state) {
            this(state.playerId, state.strengthA, state.strengthB, state.outputA, state.outputB);
        }

        private ServerStatePacket(UUID playerId, int strengthA, int strengthB, boolean outputA, boolean outputB) {
            this.playerId = playerId;
            this.strengthA = strengthA;
            this.strengthB = strengthB;
            this.outputA = outputA;
            this.outputB = outputB;
        }

        private static void encode(ServerStatePacket packet, FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.playerId);
            buffer.writeVarInt(packet.strengthA);
            buffer.writeVarInt(packet.strengthB);
            buffer.writeBoolean(packet.outputA);
            buffer.writeBoolean(packet.outputB);
        }

        private static ServerStatePacket decode(FriendlyByteBuf buffer) {
            return new ServerStatePacket(buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt(), buffer.readBoolean(), buffer.readBoolean());
        }

        private static void handle(ServerStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            ClientPlayerStateCache.applySyncedState(packet.toState());
        }

        private PlayerChannelState toState() {
            return new PlayerChannelState(playerId, strengthA, strengthB, outputA, outputB);
        }
    }

    private static final class RemoveStatePacket {
        private final UUID playerId;

        private RemoveStatePacket(UUID playerId) {
            this.playerId = playerId;
        }

        private static void encode(RemoveStatePacket packet, FriendlyByteBuf buffer) {
            buffer.writeUUID(packet.playerId);
        }

        private static RemoveStatePacket decode(FriendlyByteBuf buffer) {
            return new RemoveStatePacket(buffer.readUUID());
        }

        private static void handle(RemoveStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
            ClientPlayerStateCache.removeSyncedState(packet.playerId);
        }
    }
}
