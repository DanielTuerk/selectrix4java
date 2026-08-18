package io.github.danieltuerk.selectrix4java.bus;

/**
 * The receiver will be informed for received data from the SX bus. Must be registered to the active connected device.
 *
 * @author Daniel Tuerk
 */
public interface BusDataReceiver {

    /**
     * Data was received for the given bus.
     *
     * @param busNr number of the bus
     * @param data  received data
     */
    void received(int busNr, byte[] data);
}
