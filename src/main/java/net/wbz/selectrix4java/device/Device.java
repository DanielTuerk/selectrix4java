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
 * Device to establish connection. The connected device have the access to {@link BusAddress} to read and write data.
 *
 * @author Daniel Tuerk
 */
public interface Device extends Serializable {

    /**
     * Possible system formats for the device.
     */
    enum SYSTEM_FORMAT {
        UNKNOWN, ONLY_SX1, SX1_SX2, SX1_SX2_DCC, ONLY_DCC, SX1_SX2_MM, ONLY_MM, SX1_SX2_DCC_MM
    }

    /**
     * Id of the device.
     *
     * @return {@link String}
     */
    String getDeviceId();

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
     *
     * @throws DeviceAccessException no access
     */
    void connect() throws DeviceAccessException;

    /**
     * Disconnect the open connection.
     *
     * @throws DeviceAccessException no access
     */
    void disconnect() throws DeviceAccessException;

    /**
     * Check connection state.
     *
     * @return actual state
     */
    boolean isConnected();

    /**
     * Access to the {@link net.wbz.selectrix4java.block.BlockModule} for the given address.
     *
     * @param address address of the block module
     * @return {@link net.wbz.selectrix4java.block.BlockModule}
     * @throws DeviceAccessException no access
     */
    BlockModule getBlockModule(int address) throws DeviceAccessException;

    /**
     * Access to the {@link net.wbz.selectrix4java.block.FeedbackBlockModule} for the given address.
     *
     * @param address address of the block module
     * @param feedbackAddress address for feedback data
     * @param additionalAddress additional address to receive block states
     * @return {@link net.wbz.selectrix4java.block.FeedbackBlockModule}
     * @throws DeviceAccessException no access
     */
    FeedbackBlockModule getFeedbackBlockModule(int address, int feedbackAddress, int additionalAddress) throws
            DeviceAccessException;

    /**
     * State of the rail voltage.
     *
     * @return {@link java.lang.Boolean} the current state
     * @throws DeviceAccessException no access
     */
    boolean getRailVoltage() throws DeviceAccessException;

    /**
     * Change the state of the rail voltage.
     *
     * @param state {@link java.lang.Boolean} new state
     * @throws DeviceAccessException no access
     */
    void setRailVoltage(boolean state) throws DeviceAccessException;

    /**
     * Return the {@link BusAddress} for the rail voltage.
     *
     * @return {@link BusAddress}
     * @throws DeviceAccessException no access
     */
    BusAddress getRailVoltageAddress() throws DeviceAccessException;

    /**
     * Add listener for the state change of the rail voltage.
     *
     * @param listener {@link RailVoltageListener}
     */
    void addRailVoltageListener(RailVoltageListener listener);

    /**
     * Remove given listener instance.
     *
     * @param listener {@link RailVoltageListener}
     */
    void removeRailVoltageListener(RailVoltageListener listener);

    /**
     * Send the given byte array to the device output. Not recommended! Instead use the {@link
     * net.wbz.selectrix4java.bus.BusAddress}.
     *
     * @param data bytes to send
     */
    void sendNative(byte[] data);

    /**
     * Switch the device to the next system format.
     */
    void switchDeviceSystemFormat();

    /**
     * Actual system format.
     *
     * @return {@link net.wbz.selectrix4java.device.Device.SYSTEM_FORMAT}
     * @throws DeviceAccessException no access
     */
    SYSTEM_FORMAT getActualSystemFormat() throws DeviceAccessException;

    /**
     * Direct access to the bus. Any operation will not affect the modules of this device. Only internal usage
     * recommended! Use the {@link net.wbz.selectrix4java.bus.BusAddressListener}s for an {@link
     * net.wbz.selectrix4java.bus.BusAddress} instead of the {@link AbstractBusDataConsumer}s by the {@link
     * net.wbz.selectrix4java.bus.BusDataDispatcher}!
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
     * @throws DeviceAccessException no access
     */
    BusAddress getBusAddress(int bus, int address) throws DeviceAccessException;

    /**
     * Access the {@link TrainModule} for the given train on bus 0.
     *
     * @param address address of train
     * @param additionalAddresses additional addresses for train
     * @return {@link TrainModule}
     * @throws DeviceAccessException no access
     * @throws DeviceAccessException no access
     */
    TrainModule getTrainModule(int address, int... additionalAddresses) throws DeviceAccessException;

}
