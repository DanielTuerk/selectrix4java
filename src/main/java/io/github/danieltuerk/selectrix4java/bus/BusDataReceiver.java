package io.github.danieltuerk.selectrix4java.bus;

/**
 * The receiver will be informed for received data from the SX bus. Must be registered to the active connected device.
 *
 * @author Daniel Tuerk
 */
public interface BusDataReceiver {

    void received(int busNr, byte[] data);
}
