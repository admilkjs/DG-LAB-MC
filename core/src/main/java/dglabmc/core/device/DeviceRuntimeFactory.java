package dglabmc.core.device;

/** Creates a device runtime for one application lifecycle. */
public interface DeviceRuntimeFactory {
    /**
     * Creates a runtime. The fallback client id is used when a device does
     * not provide an id in its pairing URI.
     */
    DeviceRuntime create(String fallbackClientId);
}
