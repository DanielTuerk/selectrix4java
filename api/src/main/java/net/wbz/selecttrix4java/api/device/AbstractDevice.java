package net.wbz.selecttrix4java.api.device;

import com.google.common.collect.Maps;
import net.wbz.selecttrix4java.api.bus.BusAddress;
import net.wbz.selecttrix4java.api.bus.BusDataDispatcher;
import net.wbz.selecttrix4java.api.data.BusDataChannel;
import net.wbz.selecttrix4java.api.train.TrainModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.FutureTask;

/**
 * The device implementation manage the connection.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public abstract class AbstractDevice implements Device {

    private static final Logger log = LoggerFactory.getLogger(AbstractDevice.class);

    /**
     * Used {@link net.wbz.selecttrix4java.api.train.TrainModule}s by main {@link net.wbz.selecttrix4java.api.bus.BusAddress}.
     * Single instance of each module to prevent event-traffic.
     */
    private Map<BusAddress, TrainModule> trainModules = Maps.newHashMap();

    /**
     * Used {@link net.wbz.selecttrix4java.api.bus.BusAddress}s with descriptor as {@link java.lang.String}
     * in the format 'bus:address'.
     * <p/>
     * Single instance of each module to prevent event-traffic.
     */
    private Map<String, BusAddress> busAddresses = Maps.newHashMap();

    /**
     * Channel to send signals to the connected bus.
     */
    private BusDataChannel busDataChannel;

    /**
     * Corresponding dispatcher to read the bus and dispatch the data ot the customers.
     */
    private final BusDataDispatcher busDataDispatcher = new BusDataDispatcher();

    /**
     * Registered listener of {@link net.wbz.selecttrix4java.api.device.DeviceConnectionListener}.
     * Usage of {@link java.util.Queue} for synchronization to remove listener while event handling is in progress.
     */
    private Queue<DeviceConnectionListener> listeners = new ConcurrentLinkedQueue<>();

    /**
     * Open the connection for the device.
     *
     * @throws DeviceAccessException
     */
    @Override
    public void connect() throws DeviceAccessException {
        log.info("connect device");
        try {
            busDataChannel = doConnect(busDataDispatcher);
        } catch (Exception e) {
            throw new DeviceAccessException("can't connect", e);
        }
        log.info("device connected");

        for (final DeviceConnectionListener listener : listeners) {
            new FutureTask<>(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    listener.connected(AbstractDevice.this);
                    return null;
                }
            }).run();
        }
    }

    /**
     * Establish the connection to the OS and return the {@link net.wbz.selecttrix4java.api.data.BusDataChannel}
     * for the open streams.
     *
     * @param busDataDispatcher {@link net.wbz.selecttrix4java.api.bus.BusDataDispatcher}
     * @return {@link net.wbz.selecttrix4java.api.data.BusDataChannel}
     * @throws DeviceAccessException
     */
    abstract public BusDataChannel doConnect(BusDataDispatcher busDataDispatcher) throws DeviceAccessException;

    /**
     * Close the active connection of the device and clear all caches.
     *
     * @throws DeviceAccessException
     */
    @Override
    public void disconnect() throws DeviceAccessException {
        log.debug("close channel");
        if (busDataChannel != null) {
            busDataChannel.shutdownNow();
        }
        busDataChannel = null;
        log.info("disconnecting device");

        try {
            doDisconnect();

            log.info("device disconnected");

            for (final DeviceConnectionListener listener : listeners) {
                new FutureTask<>(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        listener.disconnected(AbstractDevice.this);
                        return null;
                    }
                }).run();
            }

        } finally {
            trainModules.clear();
            // address after train because the train map has the address as key
            busAddresses.clear();
        }
    }

    /**
     * Close the connection to the OS.
     *
     * @throws DeviceAccessException
     */
    abstract public void doDisconnect() throws DeviceAccessException;

//    abstract public boolean isConnected();

    /**
     * Get {@link net.wbz.selecttrix4java.api.bus.BusAddress} to read the data value or send new values.
     * <p/>
     * {@link net.wbz.selecttrix4java.api.bus.BusAddress} is created by the first access and cached for future access.
     *
     * @param bus     number of bus
     * @param address address to access
     * @return {@link net.wbz.selecttrix4java.api.bus.BusAddress}
     * @throws DeviceAccessException
     */
    public BusAddress getBusAddress(int bus, byte address) throws DeviceAccessException {
        checkConnected();

        String busAddressIdentifier = bus + ":" + address;
        if (!busAddresses.containsKey(String.valueOf(busAddressIdentifier))) {
            BusAddress busAddress = new BusAddress(bus, address, busDataChannel);
            busDataDispatcher.registerConsumer(busAddress.getConsumer());
            busAddresses.put(busAddressIdentifier, busAddress);
        }
        return busAddresses.get(busAddressIdentifier);
    }

    private void checkConnected() throws DeviceAccessException {
        if (!isConnected()) {
            throw new DeviceAccessException("serial device not connected");
        }
    }

    /**
     * Get {@link net.wbz.selecttrix4java.api.train.TrainModule} with actual data for the address.
     * <p/>
     * Module is created by the first access and cached for future access.
     *
     * @param addresses addresses of the train
     * @return {@link net.wbz.selecttrix4java.api.train.TrainModule}
     * @throws DeviceAccessException
     */
    public synchronized TrainModule getTrainModule(byte... addresses) throws DeviceAccessException {
        BusAddress busAddress = getBusAddress(0, addresses[0]);
        if (!trainModules.containsKey(busAddress)) {
            // TODO: additional addresses
            TrainModule module = new TrainModule(busAddress);
            trainModules.put(busAddress, module);
        }
        return trainModules.get(busAddress);
    }

    /**
     * Read the actual value of the rail voltage.
     *
     * @return {@link boolean} state
     * @throws DeviceAccessException
     */
    public boolean getRailVoltage() throws DeviceAccessException {
        return BigInteger.valueOf(getBusAddress(1, (byte) 127).getData()).testBit(6);
    }

    /**
     * Change rail voltage.
     *
     * @param state {@link boolean} state
     * @throws DeviceAccessException
     */
    public void setRailVoltage(boolean state) throws DeviceAccessException {
        BusAddress busAddress = getBusAddress(1, (byte) 127);
        if (state) {
            busAddress.setBit(6);
        } else {
            busAddress.clearBit(6);
        }
        busAddress.send();
    }

    /**
     * Dispatcher for the read and write operation of the device.
     * Used to register {@link net.wbz.selecttrix4java.api.bus.BusDataConsumer}s.
     * <p/>
     * Dispatcher is also available in offline mode and will inform all consumers after an connection is established.
     *
     * @return {@link net.wbz.selecttrix4java.api.bus.BusDataDispatcher}
     */
    public BusDataDispatcher getBusDataDispatcher() {
        return busDataDispatcher;
    }

    /**
     * Add listener for the connection state of the device.
     *
     * @param listener {@link net.wbz.selecttrix4java.api.device.DeviceConnectionListener}
     */
    public void addDeviceConnectionListener(DeviceConnectionListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove an existing listener.
     *
     * @param listener {@link net.wbz.selecttrix4java.api.device.DeviceConnectionListener}
     */
    public void removeDeviceConnectionListener(DeviceConnectionListener listener) {
        listeners.remove(listener);
    }

}
