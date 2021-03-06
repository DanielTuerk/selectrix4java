package net.wbz.selectrix4java.device.test;

import net.wbz.selectrix4java.bus.BusDataDispatcher;
import net.wbz.selectrix4java.data.BusDataChannel;
import net.wbz.selectrix4java.device.AbstractDevice;
import net.wbz.selectrix4java.device.DeviceAccessException;

/**
 * Simple test device which mock an connection. The bus is simulated by the {@link
 * net.wbz.selectrix4java.device.test.TestBus} for read and write operations.
 *
 * @author Daniel Tuerk
 */
public class TestDevice extends AbstractDevice {

    private boolean connected = false;

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public String getDeviceId() {
        return "test";
    }

    @Override
    protected BusDataChannel doConnect(BusDataDispatcher busDataDispatcher) throws DeviceAccessException {
        if (isConnected()) {
            throw new DeviceAccessException("already connected");
        }
        connected = true;
        TestBus testBus = new TestBus();
        return new BusDataChannel(testBus.getInputStream(), testBus.getOutputStream(), busDataDispatcher);
    }

    @Override
    public void doDisconnect() {
        connected = false;
    }


}
