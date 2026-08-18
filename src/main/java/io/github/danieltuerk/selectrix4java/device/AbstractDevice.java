package io.github.danieltuerk.selectrix4java.device;

import io.github.danieltuerk.selectrix4java.Module;
import io.github.danieltuerk.selectrix4java.block.BlockModule;
import io.github.danieltuerk.selectrix4java.block.FeedbackBlockModule;
import io.github.danieltuerk.selectrix4java.bus.BusAddress;
import io.github.danieltuerk.selectrix4java.bus.BusDataDispatcher;
import io.github.danieltuerk.selectrix4java.bus.consumption.AbstractBusDataConsumer;
import io.github.danieltuerk.selectrix4java.data.BusDataChannel;
import io.github.danieltuerk.selectrix4java.data.recording.BusDataRecorder;
import io.github.danieltuerk.selectrix4java.data.recording.IsRecordable;
import io.github.danieltuerk.selectrix4java.data.recording.RecordingException;
import io.github.danieltuerk.selectrix4java.train.TrainModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.FutureTask;

/**
 * The device implementation manage the connection.
 * Abstract device handle all state information for the bus. Common functions and delegates to access the bus within a
 * functional layer. Address values are wrapped by the {@link io.github.danieltuerk.selectrix4java.bus.BusAddress} and the
 * functionality by {@link io.github.danieltuerk.selectrix4java.Module} implementations (e.g. {@link
 * io.github.danieltuerk.selectrix4java.train.TrainModule}) instead of reading and writing byte arrays to the bus.
 *
 * @author Daniel Tuerk
 */
public abstract class AbstractDevice implements Device, IsRecordable {

    private static final Logger log = LoggerFactory.getLogger(AbstractDevice.class);

    /**
     * Corresponding dispatcher to read the bus and dispatch the data ot the customers.
     */
    private final BusDataDispatcher busDataDispatcher = new BusDataDispatcher();
    /**
     * Recorder to implement {@link io.github.danieltuerk.selectrix4java.data.recording.IsRecordable}.
     */
    private final BusDataRecorder busDataRecorder = new BusDataRecorder();
    /**
     * Used {@link io.github.danieltuerk.selectrix4java.bus.BusAddress}s with descriptor as {@link java.lang.String} in the format
     * 'bus:address'. Single instance of each address to prevent event-traffic.
     */
    private final Map<String, BusAddress> busAddresses = new ConcurrentHashMap<>();
    /**
     * Used {@link io.github.danieltuerk.selectrix4java.bus.BusAddress}s with descriptor as {@link java.lang.String} in the format
     * 'bus:address'. Single instance of each module to prevent event-traffic.
     */
    private final Map<String, Module> modules = new HashMap<>();
    /**
     * Channel to send signals to the connected bus.
     */
    private BusDataChannel busDataChannel;
    /**
     * Registered listener of {@link DeviceConnectionListener}. Usage of {@link java.util.Queue} for synchronization to
     * remove listener while event handling is in progress.
     */
    private final Queue<DeviceConnectionListener> listeners = new ConcurrentLinkedQueue<>();

    /**
     * Registered listener of {@link RailVoltageListener}. Usage of {@link java.util.Queue} for synchronization to
     * remove listener while event handling is in progress.
     */
    private final Queue<RailVoltageListener> railVoltageListeners = new ConcurrentLinkedQueue<>();

    /**
     * Registered listener of {@link SystemFormatListener}. Usage of {@link java.util.Queue} for synchronization to
     * remove listener while event handling is in progress.
     */
    private final Queue<SystemFormatListener> systemFormatListeners = new ConcurrentLinkedQueue<>();

    /**
     * Create new device without an established connection.
     */
    protected AbstractDevice() {
    }

    /**
     * Registered rail voltage listeners.
     *
     * @return listeners
     */
    protected Queue<RailVoltageListener> getRailVoltageListeners() {
        return railVoltageListeners;
    }

    /**
     * Registered system format listeners.
     *
     * @return listeners
     */
    protected Queue<SystemFormatListener> getSystemFormatListeners() {
        return systemFormatListeners;
    }

    /**
     * Open the connection for the device.
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
        listeners.forEach(listener -> listener.connected(AbstractDevice.this));

        busDataChannel.setCallback(() -> {
            log.info("device connection lost");
            listeners.forEach(listener -> listener.disconnected(AbstractDevice.this));
        });

        busDataChannel.start();

        initSystemFormatListener();

        initRailVoltageListener();
    }

    /**
     * Register the listener that translates bus changes into rail voltage events.
     *
     * @throws DeviceAccessException no access
     */
    abstract protected void initRailVoltageListener() throws DeviceAccessException;

    /**
     * Return all registered {@link FeedbackBlockModule}s.
     *
     * @return modules
     */
    protected List<BlockModule> getBlockModules() {
        List<BlockModule> blockModules = new ArrayList<>();
        for (Module module : modules.values()) {
            if (module instanceof BlockModule) {
                blockModules.add((BlockModule) module);
            }
        }
        return blockModules;
    }

    /**
     * Return all registered {@link FeedbackBlockModule}s.
     *
     * @return modules
     */
    protected List<FeedbackBlockModule> getFeedbackBlockModules() {
        List<FeedbackBlockModule> feedbackBlockModules = new ArrayList<>();
        for (Module module : modules.values()) {
            if (module instanceof FeedbackBlockModule) {
                feedbackBlockModules.add((FeedbackBlockModule) module);
            }
        }
        return feedbackBlockModules;
    }

    /**
     * Register the listener that translates bus changes into system format events.
     *
     * @throws DeviceAccessException no access
     */
    abstract protected void initSystemFormatListener() throws DeviceAccessException;

    /**
     * Establish the connection to the OS and return the {@link io.github.danieltuerk.selectrix4java.data.BusDataChannel} for the open
     * streams.
     *
     * @param busDataDispatcher {@link io.github.danieltuerk.selectrix4java.bus.BusDataDispatcher}
     * @return {@link io.github.danieltuerk.selectrix4java.data.BusDataChannel}
     * @throws DeviceAccessException no access
     */
    abstract protected BusDataChannel doConnect(BusDataDispatcher busDataDispatcher) throws DeviceAccessException;

    /**
     * Close the active connection of the device and clear all caches.
     */
    @Override
    public void disconnect() {
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
                new FutureTask<>((Callable<Void>) () -> {
                    listener.disconnected(AbstractDevice.this);
                    return null;
                }).run();
            }

        } finally {
            modules.clear();
            // address after train because the train map has the address as key
            busAddresses.clear();
            busDataDispatcher.reset();
        }
    }

    /**
     * Close the connection to the OS.
     */
    abstract public void doDisconnect();

    /**
     * Get {@link io.github.danieltuerk.selectrix4java.bus.BusAddress} to read the data value or send new values.
     * {@link io.github.danieltuerk.selectrix4java.bus.BusAddress} is created by the first access and cached for future access.
     *
     * @param bus number of bus
     * @param address address to access
     * @return {@link io.github.danieltuerk.selectrix4java.bus.BusAddress}
     */
    @Override
    public synchronized BusAddress getBusAddress(int bus, int address) throws DeviceAccessException {
        checkConnected();

        String busAddressIdentifier = createIdentifier(bus, address, null);
        if (!busAddresses.containsKey(busAddressIdentifier)) {
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

    private String createIdentifier(int bus, int address, Class<? extends Module> moduleClass) {
        return String.format("%d:%d:%s", bus, address, moduleClass != null ? moduleClass.getName() : "");
    }

    /**
     * Get {@link io.github.danieltuerk.selectrix4java.train.TrainModule} with actual data for the address.
     * Module is created by the first access and cached for future access.
     *
     * @param address address of the train
     * @param additionalAddresses additional function address
     * @return {@link io.github.danieltuerk.selectrix4java.train.TrainModule}
     */
    @Override
    public synchronized TrainModule getTrainModule(int address, int... additionalAddresses) throws
            DeviceAccessException {
        if (address >= 0) {
            final int bus = 0;
            String busAddressIdentifier = createIdentifier(bus, address, TrainModule.class);
            if (additionalAddresses != null) {
                busAddressIdentifier += "-additional: " + Arrays.toString(additionalAddresses);
            }
            if (!modules.containsKey(busAddressIdentifier)) {
                List<BusAddress> additionalBusAddresses = new ArrayList<>();
                if (additionalAddresses != null) {
                    for (int additionalAddress : additionalAddresses) {
                        additionalBusAddresses.add(getBusAddress(bus, additionalAddress));
                    }
                }
                TrainModule trainModule = new TrainModule(getBusAddress(bus, address),
                        additionalBusAddresses.toArray(new BusAddress[0]));
                modules.put(busAddressIdentifier, trainModule);
            }
            return (TrainModule) modules.get(busAddressIdentifier);
        }
        throw new DeviceAccessException("train with address lower than zero is invalid!");
    }

    @Override
    public synchronized BlockModule getBlockModule(int address) throws DeviceAccessException {
        int bus = 1;
        String busAddressIdentifier = createIdentifier(bus, address, BlockModule.class);
        if (!modules.containsKey(busAddressIdentifier)) {
            BlockModule blockModule = new BlockModule(getBusAddress(bus, address));
            busDataDispatcher.registerConsumers(blockModule.getConsumers());
            modules.put(busAddressIdentifier, blockModule);
        }
        return (BlockModule) modules.get(busAddressIdentifier);
    }

    @Override
    public synchronized FeedbackBlockModule getFeedbackBlockModule(int address, int feedbackAddress,
            int additionalAddress) throws DeviceAccessException {
        int bus = 1;
        String busAddressIdentifier = createIdentifier(bus, address, FeedbackBlockModule.class);
        if (!modules.containsKey(busAddressIdentifier)) {
            FeedbackBlockModule blockModule = new FeedbackBlockModule(getBusAddress(bus, address),
                    getBusAddress(bus, feedbackAddress), getBusAddress(bus, additionalAddress));
            busDataDispatcher.registerConsumers(blockModule.getConsumers());
            modules.put(busAddressIdentifier, blockModule);
        }
        return (FeedbackBlockModule) modules.get(busAddressIdentifier);
    }


    @Override
    public void addRailVoltageListener(RailVoltageListener listener) {
        railVoltageListeners.add(listener);
    }

    @Override
    public void removeRailVoltageListener(RailVoltageListener listener) {
        railVoltageListeners.remove(listener);
    }

    @Override
    public void addSystemFormatListener(SystemFormatListener listener) {
        systemFormatListeners.add(listener);
    }

    @Override
    public void removeSystemFormatListener(SystemFormatListener listener) {
        systemFormatListeners.remove(listener);
    }

    @Override
    public void sendNative(byte[] data, byte[] expectedAnswer) {
        busDataChannel.send(data, expectedAnswer);
    }

    @Override
    public BusDataChannel getBusDataChannel() {
        return busDataChannel;
    }

    /**
     * Dispatcher for the read and write operation of the device. Used to register {@link AbstractBusDataConsumer}s.
     * Dispatcher is also available in offline mode and will inform all consumers after a connection is established.
     *
     * @return {@link io.github.danieltuerk.selectrix4java.bus.BusDataDispatcher}
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
        if (isConnected()) {
            listener.connected(this);
        } else {
            listener.disconnected(this);
        }
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

    @Override
    public void startRecording(Path destinationFolder) throws DeviceAccessException {
        if (isConnected()) {
            try {
                busDataRecorder.start(destinationFolder);
                getBusDataChannel().addBusDataReceiver(busDataRecorder);
            } catch (RecordingException e) {
                throw new DeviceAccessException("no recording possible", e);
            }
        }
    }

    @Override
    public Path stopRecording() throws DeviceAccessException {
        if (isRecording()) {
            getBusDataChannel().removeBusDataReceiver(busDataRecorder);
            busDataRecorder.stop();
            return busDataRecorder.getRecordOutput();
        }
        throw new DeviceAccessException("device isn't recording");
    }

    @Override
    public boolean isRecording() {
        return busDataRecorder.isRunning();
    }
}
