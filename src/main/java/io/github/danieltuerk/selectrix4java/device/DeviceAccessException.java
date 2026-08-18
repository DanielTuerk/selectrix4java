package io.github.danieltuerk.selectrix4java.device;

/**
 * Exception for failed access to a {@link io.github.danieltuerk.selectrix4java.device.Device}.
 *
 * @author Daniel Tuerk
 */
public class DeviceAccessException extends Exception {

    /**
     * Create exception with the given message.
     *
     * @param s message
     */
    public DeviceAccessException(String s) {
        super(s);
    }

    /**
     * Create exception with the given message and cause.
     *
     * @param s         message
     * @param throwable cause
     */
    public DeviceAccessException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
