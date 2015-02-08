package net.wbz.selectrix4java.data.recording;

/**
 * Exception for the recordings of the bus data.
 *
 * @author Daniel Tuerk
 */
public class RecordingException extends Exception {

    public RecordingException(String message) {
        super(message);
    }

    public RecordingException(String message, Throwable cause) {
        super(message, cause);
    }
}
