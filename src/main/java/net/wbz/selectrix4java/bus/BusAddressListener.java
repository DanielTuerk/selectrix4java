package net.wbz.selectrix4java.bus;

/**
 * Listener for value change events of the data from an {@link net.wbz.selectrix4java.bus.BusAddress}.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public interface BusAddressListener {

    /**
     * Data of the {@link net.wbz.selectrix4java.bus.BusAddress} changed.
     *
     * @param oldValue byte
     * @param newValue byte
     */
    public void dataChanged(byte oldValue, byte newValue);
}
