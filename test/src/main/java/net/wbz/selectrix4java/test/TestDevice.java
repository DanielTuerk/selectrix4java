package net.wbz.selectrix4java.test;

import net.wbz.selectrix4java.api.bus.BusDataDispatcher;
import net.wbz.selectrix4java.api.data.BusDataChannel;
import net.wbz.selectrix4java.api.device.AbstractDevice;
import net.wbz.selectrix4java.api.device.DeviceAccessException;

/**
 * Simple test device which mock an connection.
 * The bus is simulated by the {@link net.wbz.selectrix4java.test.TestBus} for read and write operations.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class TestDevice extends AbstractDevice {

    private boolean connected = false;

    private final TestBus testBus = new TestBus();

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public BusDataChannel doConnect(BusDataDispatcher busDataDispatcher) throws DeviceAccessException {
        if (isConnected()) {
            throw new DeviceAccessException("already connected");
        }
        connected = true;
        return new BusDataChannel(testBus.getInputStream(), testBus.getOutputStream(), busDataDispatcher);
    }

    @Override
    public void doDisconnect() throws DeviceAccessException {
        connected = false;
    }
}
