package net.wbz.selectrix4java.device;

import net.wbz.selectrix4java.block.BlockModule;
import net.wbz.selectrix4java.bus.BusAddress;
import net.wbz.selectrix4java.bus.BusDataDispatcher;
import net.wbz.selectrix4java.train.TrainModule;

import java.io.Serializable;

/**
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public interface Device extends Serializable {

    public void addDeviceConnectionListener(DeviceConnectionListener listener);

    public void removeDeviceConnectionListener(DeviceConnectionListener listener);

    /**
     * Create connection of the device.
     */
    public void connect() throws DeviceAccessException;

    /**
     * Disconnect the open connection.
     */
    public void disconnect() throws DeviceAccessException;

    /**
     * Check connection state.
     *
     * @return actual state
     */
    public boolean isConnected();

    public BlockModule getBlockModule(byte... addresses) throws DeviceAccessException;

    public boolean getRailVoltage() throws DeviceAccessException;

    public void setRailVoltage(boolean state) throws DeviceAccessException;

    @Deprecated
    public BusDataDispatcher getBusDataDispatcher();

    public BusAddress getBusAddress(int bus, byte address) throws DeviceAccessException;

    public TrainModule getTrainModule(byte... address) throws DeviceAccessException;

}
