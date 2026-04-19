package dglabmc.device;

public enum DeviceChannel {
    A(1, "A"),
    B(2, "B");

    private final int numericId;
    private final String protocolName;

    DeviceChannel(int numericId, String protocolName) {
        this.numericId = numericId;
        this.protocolName = protocolName;
    }

    public int getNumericId() {
        return numericId;
    }

    public String getProtocolName() {
        return protocolName;
    }
}
