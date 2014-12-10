package net.wbz.selectrix4java.device;

import com.google.common.collect.Maps;
import net.wbz.selectrix4java.block.BlockModule;
import net.wbz.selectrix4java.block.FeedbackBlockModule;
import net.wbz.selectrix4java.bus.BusAddress;
import net.wbz.selectrix4java.bus.BusDataDispatcher;
import net.wbz.selectrix4java.data.BusDataChannel;
import net.wbz.selectrix4java.train.TrainModule;
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
    public static final int RAILVOLTAGE_ADDRESS = 127;

    /**
     * Used {@link net.wbz.selectrix4java.train.TrainModule}s by main {@link net.wbz.selectrix4java.bus.BusAddress}.
     * Single instance of each module to prevent event-traffic.
     */
    private Map<BusAddress, TrainModule> trainModules = Maps.newHashMap();

    /**
     * Used {@link net.wbz.selectrix4java.block.BlockModule}s by main {@link net.wbz.selectrix4java.bus.BusAddress}.
     * Single instance of each module to prevent event-traffic.
     */
    private Map<BusAddress, BlockModule> blockModules = Maps.newHashMap();

    /**
     * Used {@link net.wbz.selectrix4java.bus.BusAddress}s with descriptor as {@link java.lang.String}
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
     * Registered listener of {@link net.wbz.selectrix4java.device.DeviceConnectionListener}.
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
        busDataChannel.start();
    }

    /**
     * Establish the connection to the OS and return the {@link net.wbz.selectrix4java.data.BusDataChannel}
     * for the open streams.
     *
     * @param busDataDispatcher {@link net.wbz.selectrix4java.bus.BusDataDispatcher}
     * @return {@link net.wbz.selectrix4java.data.BusDataChannel}
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

    /**
     * Get {@link net.wbz.selectrix4java.bus.BusAddress} to read the data value or send new values.
     * <p/>
     * {@link net.wbz.selectrix4java.bus.BusAddress} is created by the first access and cached for future access.
     *
     * @param bus     number of bus
     * @param address address to access
     * @return {@link net.wbz.selectrix4java.bus.BusAddress}
     * @throws DeviceAccessException
     */
    @Override
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
     * Get {@link net.wbz.selectrix4java.train.TrainModule} with actual data for the address.
     * <p/>
     * Module is created by the first access and cached for future access.
     *
     * @param addresses addresses of the train
     * @return {@link net.wbz.selectrix4java.train.TrainModule}
     * @throws DeviceAccessException
     */
    @Override
    public synchronized TrainModule getTrainModule(byte... addresses) throws DeviceAccessException {
        BusAddress busAddress = getBusAddress(0, addresses[0]);
        if (!trainModules.containsKey(busAddress)) {
            // TODO: additional addresses
            TrainModule module = new TrainModule(busAddress);
            trainModules.put(busAddress, module);
        }
        return trainModules.get(busAddress);
    }

    @Override
    public synchronized BlockModule getBlockModule(byte... addresses) throws DeviceAccessException {
        BusAddress busAddress = getBusAddress(1, addresses[0]);
        if (!blockModules.containsKey(busAddress)) {
            // TODO: additional addresses
            BlockModule module = new BlockModule(busAddress);
            blockModules.put(busAddress, module);
        }
        return blockModules.get(busAddress);
    }

    public synchronized FeedbackBlockModule getFeedbackBlockModule(byte address, byte feedbackAddress, byte... additionalAddresses) throws DeviceAccessException {
        BusAddress busAddress = getBusAddress(1, address);
        if (!blockModules.containsKey(busAddress)) {
            // TODO: additional addresses
            FeedbackBlockModule module = new FeedbackBlockModule(trainModules, busAddress,
                    getBusAddress(0, feedbackAddress), getBusAddress(0, feedbackAddress));
            blockModules.put(busAddress, module);
        }
        BlockModule blockModule = blockModules.get(busAddress);
        if (blockModule instanceof FeedbackBlockModule) {
            return (FeedbackBlockModule) blockModule;
        } else {
            throw new DeviceAccessException(String.format("query %s but found %s for address %s", FeedbackBlockModule.class.getSimpleName(), BlockModule.class.getSimpleName(), address));
        }
    }

    /**
     * Read the actual value of the rail voltage.
     *
     * @return {@link boolean} state
     * @throws DeviceAccessException
     */
    public boolean getRailVoltage() throws DeviceAccessException {
        return BigInteger.valueOf(getBusAddress(1, (byte) RAILVOLTAGE_ADDRESS).getData()).testBit(8);
    }

    /**
     * Change rail voltage.
     *
     * @param state {@link java.lang.Boolean} state
     * @throws DeviceAccessException
     */
    public void setRailVoltage(boolean state) throws DeviceAccessException {
        BusAddress busAddress = getBusAddress(1, (byte) RAILVOLTAGE_ADDRESS);
        if (state) {
            busAddress.setBit(8);
        } else {
            busAddress.clearBit(8);
        }
        busAddress.send();
    }

    /**
     * Dispatcher for the read and write operation of the device.
     * Used to register {@link net.wbz.selectrix4java.bus.BusDataConsumer}s.
     * <p/>
     * Dispatcher is also available in offline mode and will inform all consumers after an connection is established.
     *
     * @return {@link net.wbz.selectrix4java.bus.BusDataDispatcher}
     */
    public BusDataDispatcher getBusDataDispatcher() {
        return busDataDispatcher;
    }

    /**
     * Add listener for the connection state of the device.
     *
     * @param listener {@link net.wbz.selectrix4java.device.DeviceConnectionListener}
     */
    public void addDeviceConnectionListener(DeviceConnectionListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove an existing listener.
     *
     * @param listener {@link net.wbz.selectrix4java.device.DeviceConnectionListener}
     */
    public void removeDeviceConnectionListener(DeviceConnectionListener listener) {
        listeners.remove(listener);
    }

}
