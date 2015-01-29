package net.wbz.selectrix4java.device.station;

import net.wbz.selectrix4java.device.serial.SerialDevice;

/**
 * @author Daniel Tuerk
 */
public class StaerzInterfaceStation extends SerialDevice {
    /**
     * Create device to connect to an serial interface.
     *
     * @param deviceId {@link String} OS device id
     * @param baudRate {@link int} baud rate of the device
     */
    public StaerzInterfaceStation(String deviceId, int baudRate) {
        super(deviceId, baudRate);
    }
}
