package net.wbz.selectrix4java.bus;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import net.wbz.selectrix4java.bus.consumption.AbstractBusDataConsumer;
import net.wbz.selectrix4java.bus.consumption.AbstractMultiAddressBusDataConsumer;
import net.wbz.selectrix4java.bus.consumption.AllBusDataConsumer;
import net.wbz.selectrix4java.bus.consumption.BusAddressData;
import net.wbz.selectrix4java.bus.consumption.BusAddressDataConsumer;
import net.wbz.selectrix4java.bus.consumption.BusBitConsumer;
import net.wbz.selectrix4java.bus.consumption.BusMultiAddressDataConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * The dispatcher stores all values for each address of the SX bus channels. To
 * receive the values it implements {@link BusDataReceiver}.
 * <p/>
 * For each value which has changed, the registered
 * {@link AbstractBusDataConsumer}s are informed.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class BusDataDispatcher implements BusDataReceiver {
    private static final Logger log = LoggerFactory.getLogger(BusDataDispatcher.class);
    /**
     * Service to call the consumers asynchronously.
     */
    private final ExecutorService executorService;
    /**
     * Hold the actual data of the bus. Used to compare old and new bit data for
     * each bus to identify changes.
     */
    // private Map<Integer, byte[]> busData = Maps.newConcurrentMap();
    private Map<Integer, byte[]> busData = Maps.newHashMap();
    /**
     * Consumers to call for bus data changes.
     */
    private Queue<AbstractBusDataConsumer> consumers = new ConcurrentLinkedQueue<>();

    /**
     * Create dispatcher with executor service for cached thread pool.
     */
    public BusDataDispatcher() {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("bus-data-dispatcher-%d").build();
        executorService = Executors.newCachedThreadPool(namedThreadFactory);
    }

    /**
     * Return the registered consumers of the given type.
     *
     * @param type class of type for consumers to return
     * @param <T> type of {@link AbstractBusDataConsumer}
     * @return collection of consumers for the given type or empty collection
     */
    private <T extends AbstractBusDataConsumer> Collection<T> getConsumersOfType(final Class<? extends T> type) {
        List<T> filtered = new ArrayList<>();
        for (AbstractBusDataConsumer consumer : consumers) {
            if (type.isAssignableFrom(consumer.getClass())) {
                filtered.add((type.cast(consumer)));
            }
        }
        return Collections.unmodifiableList(filtered);
    }

    /**
     * Return the current values of all addresses for the given SX bus.
     *
     * @param busNr number of the SX bus (e.g.0 or 1)
     * @return byte array which represents the address as index with the byte
     *         value of the address.
     */
    public byte[] getData(int busNr) {
        if (busData.containsKey(busNr)) {
            return busData.get(busNr);
        }
        throw new RuntimeException(String.format("no bus found for number '%d'", busNr));
    }

    /**
     * Register an new consumer to get state changed of for the values of an
     * address from an SX bus. To get the values of all addresses use the
     * {@link net.wbz.selectrix4java.bus.consumption.AllBusDataConsumer}.
     *
     * @param consumer {@link AbstractBusDataConsumer}
     */
    public void registerConsumer(final AbstractBusDataConsumer consumer) {
        consumers.add(consumer);

        // TODO

        // fire current value for the consumer
        if (consumer instanceof AbstractMultiAddressBusDataConsumer) {
            for (final int bus : busData.keySet()) {
                if (consumer instanceof AllBusDataConsumer) {
                    fireAllBusDataChange((AllBusDataConsumer) consumer, bus);
                } else if (consumer instanceof BusMultiAddressDataConsumer) {
                    byte[] oldData = new byte[busData.get(bus).length];
                    Arrays.fill(oldData, (byte) 0);
                    fireMultiAddressChange((BusMultiAddressDataConsumer) consumer, bus, oldData, busData.get(bus),
                            true);
                }
            }
        } else if (consumer instanceof BusAddressDataConsumer) {
            final BusAddressDataConsumer busAddressDataConsumer = (BusAddressDataConsumer) consumer;
            // check for valid consumer
            if (busData.containsKey(busAddressDataConsumer.getBus())
                    && busData.get(busAddressDataConsumer.getBus()).length > busAddressDataConsumer.getAddress()) {
                final byte addressValue = busData.get(busAddressDataConsumer.getBus())[busAddressDataConsumer
                        .getAddress()];

                callSingleAddressConsumer(busAddressDataConsumer, busAddressDataConsumer.getBus(),
                        busAddressDataConsumer.getAddress(), 0, addressValue, true);
            }
        } else {
            throw new RuntimeException(String.format("registered invalid consumer: %s", consumer.getClass().getName()));
        }

    }

    public void registerConsumers(List<AbstractBusDataConsumer> consumers) {
        for (AbstractBusDataConsumer consumer : consumers) {
            registerConsumer(consumer);
        }
    }

    public void unregisterConsumer(AbstractBusDataConsumer consumer) {
        consumers.remove(consumer);
    }

    public void unregisterConsumers(List<AbstractBusDataConsumer> consumers) {
        this.consumers.removeAll(consumers);
    }

    @Override
    public void received(final int busNr, byte[] data) {
        final boolean initialCall;
        final byte[] oldData;

        // TODO: refactor: value changed -> call effected consumers -> decide

        if (busData.containsKey(busNr)) {
            initialCall = false;
            oldData = busData.get(busNr);

        } else {
            initialCall = true;
            // first received data of the bus nr; set initial states to 0 for each address
            oldData = new byte[data.length];
            Arrays.fill(oldData, (byte) 0);
        }

        // call registered consumer
        callConsumers(busNr, data, oldData, initialCall);

        // store actual data to compare as old data by next call
        busData.put(busNr, data);
    }

    /**
     * TODO
     * 
     * @param busNr
     * @param data
     * @param oldData
     * @param initialCall
     */
    private void callConsumers(int busNr, byte[] data, byte[] oldData, boolean initialCall) {

        fireMultiAddressChange(getConsumersOfType(BusMultiAddressDataConsumer.class), busNr, oldData, data,
                initialCall);

        // fire changes one after another for each address
        for (int i = 0; i < data.length; i++) {
            if (Byte.compare(data[i], oldData[i]) != 0) {

                for (AbstractBusDataConsumer consumer : consumers) {
                    //TODO
                    if (!(consumer instanceof BusMultiAddressDataConsumer)) {
                        if (consumer instanceof AllBusDataConsumer) {
                            callAllBusDataConsumers(busNr, i, oldData[i], data[i], (AllBusDataConsumer) consumer);
                        } else if (consumer instanceof BusAddressDataConsumer) {
                            callSingleAddressConsumer((BusAddressDataConsumer) consumer, busNr, i, oldData[i], data[i], initialCall);
                        } else {
                            String errorMsg = "unknown consumer: " + consumer.getClass().getName();
                            log.error(errorMsg);
                            throw new RuntimeException(errorMsg);
                        }
                    }
                }
            }
        }
    }

    private void callAllBusDataConsumers(final int busNr, final int address, final int oldData, final int newData,
            final AllBusDataConsumer consumer) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                consumer.valueChanged(busNr, address, oldData, newData);
            }
        });
    }

    private void callSingleAddressConsumer(final BusAddressDataConsumer consumer, final int busNr, final int address,
            final int oldData, final int newData, boolean initialCall) {
        if (consumer instanceof BusBitConsumer) {
            // bit change
            final BusBitConsumer busBitConsumer = (BusBitConsumer) consumer;
            if (busBitConsumer.getAddress() == address && consumer.getBus() == busNr) {
                final boolean oldBitState = BigInteger.valueOf(oldData)
                        .testBit(((BusBitConsumer) consumer).getBit() - 1);
                final boolean newBitState = BigInteger.valueOf(newData)
                        .testBit(((BusBitConsumer) consumer).getBit() - 1);
                if (initialCall || oldBitState != newBitState) {
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            busBitConsumer.valueChanged(oldBitState ? 1 : 0, newBitState ? 1 : 0);
                        }
                    });
                }
            }
        } else {
            // address change
            if (initialCall || (consumer.getAddress() == address && consumer.getBus() == busNr)) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        consumer.valueChanged(oldData, newData);
                    }
                });
            }
        }
    }

    private void fireAllBusDataChange(final AllBusDataConsumer consumer, final int bus) {
        for (int i = 0; i < busData.get(bus).length; i++) {
            final int finalI = i;
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    consumer.valueChanged(bus, finalI, 0, busData.get(bus)[finalI]);
                }
            });
        }
    }

    /**
     * Call asynchronous the
     * {@link net.wbz.selectrix4java.bus.consumption.BusMultiAddressDataConsumer}
     * s for data value changes of the addresses.
     *
     * @param multiAddressConsumers list of
     *            {@link net.wbz.selectrix4java.bus.consumption.AbstractMultiAddressBusDataConsumer}
     * @param busNr number of bus
     * @param oldData old data of bus
     * @param data new data of bus
     * @param initialCall {@code true} to fire instead of same old and new data
     *            value
     */
    private void fireMultiAddressChange(Collection<BusMultiAddressDataConsumer> multiAddressConsumers, int busNr,
            byte[] oldData, byte[] data, boolean initialCall) {
        for (final BusMultiAddressDataConsumer multiAddressDataConsumer : multiAddressConsumers) {
            fireMultiAddressChange(multiAddressDataConsumer, busNr, oldData, data, initialCall);
        }
    }

    private void fireMultiAddressChange(final BusMultiAddressDataConsumer multiAddressDataConsumer, int busNr,
            byte[] oldData, byte[] data, boolean initialCall) {
        boolean anyAddressDataChanged = false;
        if (busNr == multiAddressDataConsumer.getBus()) {
            final BusAddressData[] busAddressData = new BusAddressData[multiAddressDataConsumer.getAddresses().length];
            for (int addressIndex = 0; addressIndex < multiAddressDataConsumer.getAddresses().length; addressIndex++) {
                int busAddress = multiAddressDataConsumer.getAddresses()[addressIndex];
                if (Byte.compare(data[busAddress], oldData[busAddress]) != 0) {
                    anyAddressDataChanged = true;
                }
                busAddressData[addressIndex] = new BusAddressData(busNr, busAddress, oldData[busAddress],
                        data[busAddress]);
            }
            if (anyAddressDataChanged || initialCall) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        multiAddressDataConsumer.valueChanged(busAddressData);
                    }
                });
            }
        }
    }
}
