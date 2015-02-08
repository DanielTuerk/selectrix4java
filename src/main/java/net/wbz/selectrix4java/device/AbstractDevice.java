package net.wbz.selectrix4java.device;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.wbz.selectrix4java.block.BlockModule;
import net.wbz.selectrix4java.block.FeedbackBlockModule;
import net.wbz.selectrix4java.bus.BusAddress;
import net.wbz.selectrix4java.bus.BusAddressListener;
import net.wbz.selectrix4java.bus.BusDataDispatcher;
import net.wbz.selectrix4java.data.BusDataChannel;
import net.wbz.selectrix4java.train.TrainModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.FutureTask;

/**
 * The device implementation manage the connection.
 * <p/>
 * Abstract device handle all state information for the bus. Common functions and delegates to access the bus within
 * an functional layer. Address values are wrapped by the {@link net.wbz.selectrix4java.bus.BusAddress} and the
 * functionality by {@link net.wbz.selectrix4java.Module} implementations
 * (e.g. {@link net.wbz.selectrix4java.train.TrainModule}) instead of reading and writing byte arrays to the bus.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public abstract class AbstractDevice implements Device {

    private static final Logger log = LoggerFactory.getLogger(AbstractDevice.class);

    /**
     * Default address for the rail voltage.
     * TODO: move later to implementations by device (e.g. FCC)
     */
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
     * Registered listener of {@link DeviceConnectionListener}.
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

        busDataChannel.setCallback(new BusDataChannel.ChannelStateCallback() {
            @Override
            public void channelClosed() {
                log.info("device connection lost");
                for (final DeviceConnectionListener listener : listeners) {
                    new FutureTask<>(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            listener.disconnected(AbstractDevice.this);
                            return null;
                        }
                    }).run();
                }
            }
        });

        busDataChannel.start();

        getBusAddress(1, (byte) 110).addListener(new BusAddressListener() {

            @Override
            public void dataChanged(byte oldValue, byte newValue) {

                BigInteger wrappedOldValue = BigInteger.valueOf(oldValue);
                BigInteger wrappedNewValue = BigInteger.valueOf(newValue);
                int oldSystemFormat = wrappedOldValue.clearBit(5).clearBit(6).clearBit(7).intValue() & 0xff;
                int newSystemFormat = wrappedNewValue.clearBit(5).clearBit(6).clearBit(7).intValue() & 0xff;

                if (oldSystemFormat != newSystemFormat) {
                    fireSystemFormat(convertSystemFormat(newSystemFormat));
                }
            }

            private void fireSystemFormat(final SYSTEM_FORMAT systemFormat) {
                for (final DeviceConnectionListener listener : listeners) {
                    if (listener instanceof DeviceListener) {
                        new FutureTask<>(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                ((DeviceListener) listener).systemFormatChanged(systemFormat);
                                return null;
                            }
                        }).run();
                    }
                }
            }
        });
    }

    /**
     * Convert the given data value for bits 1-4 to the {@link net.wbz.selectrix4java.device.Device.SYSTEM_FORMAT}.
     *
     * @param newSystemFormat integer value of the system format
     * @return {@link net.wbz.selectrix4java.device.Device.SYSTEM_FORMAT}
     */
    private SYSTEM_FORMAT convertSystemFormat(int newSystemFormat) {
        SYSTEM_FORMAT systemFormat;
        switch (newSystemFormat) {
            case 0:
                systemFormat = SYSTEM_FORMAT.ONLY_SX1;
                break;
            case 2:
                systemFormat = SYSTEM_FORMAT.SX1_SX2;
                break;
            case 4:
                systemFormat = SYSTEM_FORMAT.SX1_SX2_DCC;
                break;
            case 6:
                systemFormat = SYSTEM_FORMAT.ONLY_DCC;
                break;
            case 5:
                systemFormat = SYSTEM_FORMAT.SX1_SX2_MM;
                break;
            case 7:
                systemFormat = SYSTEM_FORMAT.ONLY_MM;
                break;
            case 11:
                systemFormat = SYSTEM_FORMAT.SX1_SX2_DCC_MM;
                break;
            default:
                systemFormat = SYSTEM_FORMAT.UNKNOWN;
        }
        return systemFormat;
    }

    /**
     * Establish the connection to the OS and return the {@link net.wbz.selectrix4java.data.BusDataChannel}
     * for the open streams.
     *
     * @param busDataDispatcher {@link net.wbz.selectrix4java.bus.BusDataDispatcher}
     * @return {@link net.wbz.selectrix4java.data.BusDataChannel}
     * @throws DeviceAccessException
     */
    abstract protected BusDataChannel doConnect(BusDataDispatcher busDataDispatcher) throws DeviceAccessException;

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

    @Override
    public synchronized FeedbackBlockModule getFeedbackBlockModule(byte address, byte feedbackAddress, byte... additionalAddresses) throws DeviceAccessException {
        BusAddress busAddress = getBusAddress(1, address);
        if (!blockModules.containsKey(busAddress) || !(blockModules.get(busAddress) instanceof FeedbackBlockModule)) {
            List<BusAddress> additionalBusAddresses = Lists.newArrayList();
            for (byte additionalAddress : additionalAddresses) {
                additionalBusAddresses.add(getBusAddress(1, additionalAddress));
            }
            FeedbackBlockModule module = new FeedbackBlockModule(trainModules, busAddress,
                    getBusAddress(1, feedbackAddress), additionalBusAddresses.toArray(new BusAddress[additionalBusAddresses.size()]));
            blockModules.put(busAddress, module);
        }
        BlockModule blockModule = blockModules.get(busAddress);
        if (blockModule instanceof FeedbackBlockModule) {
            return (FeedbackBlockModule) blockModule;
        } else {
            throw new DeviceAccessException(String.format("query %s but found %s for address %s",
                    FeedbackBlockModule.class.getSimpleName(), BlockModule.class.getSimpleName(), address));
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

    @Override
    public void sendNative(byte[] data) {
        busDataChannel.send(data);
    }

    @Override
    public void switchDeviceSystemFormat() {
        sendNative(new byte[]{(byte) 131, (byte) 160, (byte) 0, (byte) 0, (byte) 0});
    }

    @Override
    public SYSTEM_FORMAT getActualSystemFormat() throws DeviceAccessException {
        //TODO: maybe getting initial 0 value before consumer update the address data value
        BigInteger wrappedData = BigInteger.valueOf(getBusAddress(0, (byte) 110).getData());
        return convertSystemFormat(wrappedData.clearBit(5).clearBit(6).clearBit(7).intValue() & 0xff);
    }

    /**
     * Running channel for communication or {@code null} if not connected.
     *
     * @return {@link net.wbz.selectrix4java.data.BusDataChannel} or {@code null}
     */
    protected BusDataChannel getBusDataChannel() {
        return busDataChannel;
    }

    /**
     * Dispatcher for the read and write operation of the device.
     * Used to register {@link net.wbz.selectrix4java.bus.consumption.BusDataConsumer}s.
     * <p/>
     * Dispatcher is also available in offline mode and will inform all consumers after an connection is established.
     *
     * @return {@link net.wbz.selectrix4java.bus.BusDataDispatcher}
     */
    public BusDataDispatcher getBusDataDispatcher() {
        return busDataDispatcher;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDeviceConnectionListener(DeviceConnectionListener listener) {
        listeners.add(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeDeviceConnectionListener(DeviceConnectionListener listener) {
        listeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDeviceListener(DeviceListener listener) {
        addDeviceConnectionListener(listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeDeviceListener(DeviceListener listener) {
        removeDeviceConnectionListener(listener);
    }
}
