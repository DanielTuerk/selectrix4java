package net.wbz.selectrix4java.device;

import java.io.Serializable;

import net.wbz.selectrix4java.block.BlockModule;
import net.wbz.selectrix4java.block.FeedbackBlockModule;
import net.wbz.selectrix4java.bus.BusAddress;
import net.wbz.selectrix4java.bus.BusDataDispatcher;
import net.wbz.selectrix4java.bus.consumption.AbstractBusDataConsumer;
import net.wbz.selectrix4java.data.BusDataChannel;
import net.wbz.selectrix4java.train.TrainModule;

/**
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public interface Device extends Serializable {

    /**
     * Possible system formats for the device.
     */
    enum SYSTEM_FORMAT {
        UNKNOWN, ONLY_SX1, SX1_SX2, SX1_SX2_DCC, ONLY_DCC, SX1_SX2_MM, ONLY_MM, SX1_SX2_DCC_MM
    }

    /**
     * Register the given listener to the device to receive connection state changes.
     *
     * @param listener {@link DeviceConnectionListener}
     */
    void addDeviceConnectionListener(DeviceConnectionListener listener);

    /**
     * Remove the given and already registered listener.
     *
     * @param listener {@link DeviceConnectionListener}
     */
    void removeDeviceConnectionListener(DeviceConnectionListener listener);

    /**
     * Register the given listener to the device to receive state changes of the device.
     *
     * @param listener {@link DeviceListener}
     */
    void addDeviceListener(DeviceListener listener);

    /**
     * Remove the given and already registered listener.
     *
     * @param listener {@link DeviceListener}
     */
    void removeDeviceListener(DeviceListener listener);

    /**
     * Create connection of the device.
     */
    void connect() throws DeviceAccessException;

    /**
     * Disconnect the open connection.
     */
    void disconnect() throws DeviceAccessException;

    /**
     * Check connection state.
     *
     * @return actual state
     */
    boolean isConnected();

    /**
     * Access to the {@link net.wbz.selectrix4java.block.BlockModule} for the given address or multi addresses.
     *
     * @param bus number of bus
     * @param address address of the block module
     * @return {@link net.wbz.selectrix4java.block.BlockModule}
     * @throws DeviceAccessException
     */
    BlockModule getBlockModule(int address) throws DeviceAccessException;

    /**
     * TODO
     *
     * @param address
     * @param feedbackAddress
     * @param additionalAddress
     * @return {@link net.wbz.selectrix4java.block.FeedbackBlockModule}
     * @throws DeviceAccessException
     */
    FeedbackBlockModule getFeedbackBlockModule(int address, int feedbackAddress, int additionalAddress) throws DeviceAccessException;

    /**
     * State of the rail voltage.
     *
     * @return {@link java.lang.Boolean} the current state
     * @throws DeviceAccessException
     */
    boolean getRailVoltage() throws DeviceAccessException;

    /**
     * Return the {@link BusAddress} for the rail voltage.
     *
     * @return {@link BusAddress}
     * @throws DeviceAccessException
     */
    BusAddress getRailVoltageAddress() throws DeviceAccessException;

    /**
     * Change the state of the rail voltage.
     *
     * @param state {@link java.lang.Boolean} new state
     * @throws DeviceAccessException
     */
    void setRailVoltage(boolean state) throws DeviceAccessException;

    /**
     * Send the given byte array to the device output.
     * Not recommended! Instead use the {@link net.wbz.selectrix4java.bus.BusAddress}.
     *
     * @param data bytes to send
     */
    void sendNative(byte[] data);

    /**
     * Switch the device to the next system format.
     */
    void switchDeviceSystemFormat();

    /**
     * @return {@link net.wbz.selectrix4java.device.Device.SYSTEM_FORMAT}
     */
    SYSTEM_FORMAT getActualSystemFormat() throws DeviceAccessException;

    /**
     * Direct access to the bus. Any operation will not affect the modules of this device.
     * <p/>
     * Only internal usage recommended!
     * Use the {@link net.wbz.selectrix4java.bus.BusAddressListener}s for an
     * {@link net.wbz.selectrix4java.bus.BusAddress} instead of the {@link AbstractBusDataConsumer}s
     * by the {@link net.wbz.selectrix4java.bus.BusDataDispatcher}!
     *
     * @return {@link net.wbz.selectrix4java.bus.BusDataDispatcher}
     */
    BusDataDispatcher getBusDataDispatcher();

    /**
     * Running channel for communication or {@code null} if not connected.
     *
     * @return {@link net.wbz.selectrix4java.data.BusDataChannel} or {@code null}
     */
    BusDataChannel getBusDataChannel();

    /**
     * Get or create the {@link net.wbz.selectrix4java.bus.BusAddress}.
     *
     * @param bus number of bus
     * @param address address
     * @return {@link net.wbz.selectrix4java.bus.BusAddress}
     * @throws DeviceAccessException
     */
    BusAddress getBusAddress(int bus, int address) throws DeviceAccessException;

    /**
     * TODO
     * @param address
     * @return
     * @throws DeviceAccessException
     */
    TrainModule getTrainModule(int address, int... additionalAddresses) throws DeviceAccessException;

}
