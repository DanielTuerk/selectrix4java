package net.wbz.selecttrix4java.api.bus;

/**
 * Listener for value change events of the data from an {@link net.wbz.selecttrix4java.api.bus.BusAddress}.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public interface BusAddressListener {

    /**
     * Data of the {@link net.wbz.selecttrix4java.api.bus.BusAddress} changed.
     *
     * @param oldValue byte
     * @param newValue byte
     */
    public void dataChanged(byte oldValue, byte newValue);
}
