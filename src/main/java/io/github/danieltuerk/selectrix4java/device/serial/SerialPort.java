package io.github.danieltuerk.selectrix4java.device.serial;

/**
 * Abstraction of an opened OS serial port for read and write access.
 *
 * @author Daniel Tuerk
 */
public interface SerialPort {

    /**
     * Write the given bytes to the port.
     *
     * @param data bytes to send
     */
    void write(byte[] data);

    /**
     * Read from the port, blocking until data is available or the timeout elapses.
     *
     * @param buffer    buffer to fill
     * @param timeoutMs timeout in milliseconds
     * @return number of bytes read
     */
    int read(byte[] buffer, int timeoutMs);

    /**
     * Read from the port without blocking.
     *
     * @param buffer buffer to fill
     * @return number of bytes read
     */
    int readNonBlocking(byte[] buffer);

    /**
     * Number of bytes currently available to read.
     *
     * @return available bytes
     */
    int available();

    /**
     * Close the port.
     */
    void close();
}
