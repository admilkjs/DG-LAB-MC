package dglabmc.core.device;

/**
 * Platform-neutral lifecycle port for the DG-LAB device WebSocket server.
 *
 * <p>The adapter owns the actual socket implementation. Core only needs to
 * start and stop it and expose a small amount of state to the UI.</p>
 */
public interface DeviceServer {
    /** Convenience overload for adapters that always bind the wildcard host. */
    default void start(int port) {
        start("0.0.0.0", port);
    }

    /** Starts listening on the supplied host and port. */
    void start(String host, int port);

    /** Stops listening and releases all server resources. */
    void stop();

    /** Returns whether the server is currently listening. */
    boolean isRunning();

    /** Returns the effective bound port, or {@code -1} when stopped. */
    int getBoundPort();

    /** Returns the host used for binding, or an empty string when stopped. */
    String getBoundHost();

    /** Returns the most recent startup/runtime error, if any. */
    String getLastErrorMessage();
}
