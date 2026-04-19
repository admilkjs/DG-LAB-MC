package dglabmc.device;

public class DeviceMessage {
    public String type;
    public String clientId;
    public String targetId;
    public String message;

    public DeviceMessage() {
    }

    public DeviceMessage(String type, String clientId, String targetId, String message) {
        this.type = type;
        this.clientId = clientId;
        this.targetId = targetId;
        this.message = message;
    }
}
