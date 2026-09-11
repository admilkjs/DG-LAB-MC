package dglabmc.core.device;

/**
 * Complete device runtime exposed by a platform adapter.
 *
 * <p>The runtime combines the command transport used by {@code RuleEngine}
 * with the server lifecycle and a diagnostic snapshot. Implementations may
 * internally use separate session and server objects.</p>
 */
public interface DeviceRuntime extends DeviceTransport {
    /** Returns the platform-owned server lifecycle port. */
    DeviceServer server();

    /** Returns a point-in-time copy of the active device state. */
    DeviceSnapshot snapshot();

    /** Releases device session, timers and server resources. */
    void shutdown();
}
