package net.wbz.selectrix4java.device.test;

import net.wbz.selectrix4java.bus.BusAddress;
import net.wbz.selectrix4java.bus.BusDataDispatcher;
import net.wbz.selectrix4java.data.BusDataChannel;
import net.wbz.selectrix4java.device.AbstractDevice;
import net.wbz.selectrix4java.device.DeviceAccessException;

/**
 * Simple test device which mock a connection. The bus is simulated by the {@link
 * TestSerialPort} for read and write operations.
 *
 * @author Daniel Tuerk
 */
public class TestDevice extends AbstractDevice {

    private final String deviceId;
    private boolean connected = false;
    private boolean railvoltage = false;
    private SYSTEM_FORMAT actualSystemFormat = SYSTEM_FORMAT.ONLY_SX1;

    public TestDevice(String deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public String getDeviceId() {
        return deviceId;
    }

    @Override
    protected void initRailVoltageListener() {
        fireRailvoltage();
    }

    @Override
    protected void initSystemFormatListener() {
        fireSystemFormat();
    }

    @Override
    protected BusDataChannel doConnect(BusDataDispatcher busDataDispatcher) throws DeviceAccessException {
        if (isConnected()) {
            throw new DeviceAccessException("already connected");
        }
        connected = true;
        return new BusDataChannel(new TestSerialPort(), busDataDispatcher);
    }

    @Override
    public void doDisconnect() {
        connected = false;
    }

    @Override
    public boolean getRailVoltage() {
        return railvoltage;
    }

    @Override
    public void setRailVoltage(boolean state) {
        this.railvoltage = state;

        fireRailvoltage();
    }

    @Override
    public BusAddress getRailVoltageAddress() {
        return null;
    }

    @Override
    public void switchDeviceSystemFormat() {
        var values = SYSTEM_FORMAT.values();
        int nextOrdinal = (actualSystemFormat.ordinal() + 1) % values.length;
        actualSystemFormat = values[nextOrdinal];

        fireSystemFormat();
    }

    private void fireSystemFormat() {
        getSystemFormatListeners().forEach(listener -> listener.systemFormatChanged(actualSystemFormat));
    }

    private void fireRailvoltage() {
        getRailVoltageListeners().forEach(listener -> listener.changed(railvoltage));
    }

    @Override
    public SYSTEM_FORMAT getActualSystemFormat() {
        return actualSystemFormat;
    }

    @Override
    public String toString() {
        return "TestDevice{deviceId=%s, connected=%s}".formatted(getDeviceId(), connected);
    }
}
