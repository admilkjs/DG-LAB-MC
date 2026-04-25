package dglabmc.multiplayer;

import java.util.UUID;

public class PlayerChannelState {
    public final UUID playerId;
    public final int strengthA;
    public final int strengthB;
    public final boolean outputA;
    public final boolean outputB;

    public PlayerChannelState(UUID playerId, int strengthA, int strengthB, boolean outputA, boolean outputB) {
        this.playerId = playerId;
        this.strengthA = clamp(strengthA);
        this.strengthB = clamp(strengthB);
        this.outputA = outputA;
        this.outputB = outputB;
    }

    public boolean sameVisualState(PlayerChannelState other) {
        return other != null
            && playerId.equals(other.playerId)
            && strengthA == other.strengthA
            && strengthB == other.strengthB
            && outputA == other.outputA
            && outputB == other.outputB;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(200, value));
    }
}
