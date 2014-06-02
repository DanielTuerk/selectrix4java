package net.wbz.selecttrix4java.api.bus;

/**
 * The receiver will be informed for received data from the SX bus.
 *
 * Must be registered to the active connected device.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public interface BusDataReceiver {

    public void received(int busNr, byte[] data);
}
