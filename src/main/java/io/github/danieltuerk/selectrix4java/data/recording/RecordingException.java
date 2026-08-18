package io.github.danieltuerk.selectrix4java.data.recording;

/**
 * Exception for the recordings of the bus data.
 *
 * @author Daniel Tuerk
 */
public class RecordingException extends Exception {

    /**
     * Create exception with the given message.
     *
     * @param message message
     */
    public RecordingException(String message) {
        super(message);
    }

    /**
     * Create exception with the given message and cause.
     *
     * @param message message
     * @param cause   cause
     */
    public RecordingException(String message, Throwable cause) {
        super(message, cause);
    }
}
