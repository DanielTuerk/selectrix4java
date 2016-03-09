package net.wbz.selectrix4java.bus;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import net.wbz.selectrix4java.bus.consumption.AbstractBusDataConsumer;
import net.wbz.selectrix4java.bus.consumption.AllBusDataConsumer;
import net.wbz.selectrix4java.bus.consumption.BusAddressData;
import net.wbz.selectrix4java.bus.consumption.BusAddressDataConsumer;
import net.wbz.selectrix4java.bus.consumption.BusBitConsumer;
import net.wbz.selectrix4java.bus.consumption.BusMultiAddressDataConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
    private Map<Integer, byte[]> busData = Maps.newConcurrentMap();

    /**
     * Consumers to call for bus data changes.
     */
    private List<AbstractBusDataConsumer> consumers = Lists.newCopyOnWriteArrayList();

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
    private <T extends AbstractBusDataConsumer> Collection<T> getConsumersOfType(
            Collection<AbstractBusDataConsumer> consumers, final Class<? extends T> type) {
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
     * Consumer is called initially with the actual data values after registration.
     *
     * @param consumer {@link AbstractBusDataConsumer}
     */
    public void registerConsumer(final AbstractBusDataConsumer consumer) {
        registerConsumers(Lists.newArrayList(consumer));
    }

    /**
     * Register given list of {@link AbstractBusDataConsumer}s.
     *
     * @see #registerConsumer(AbstractBusDataConsumer)
     * @param consumers list of {@link AbstractBusDataConsumer} to register
     */
    public void registerConsumers(List<AbstractBusDataConsumer> consumers) {
        this.consumers.addAll(consumers);

        for (Integer busNr : busData.keySet()) {
            callConsumers(consumers, busNr, busData.get(busNr), busData.get(busNr), true);
        }
    }

    /**
     * Unregister the given consumer.
     *
     * @param consumer {@link AbstractBusDataConsumer}
     */
    public void unregisterConsumer(AbstractBusDataConsumer consumer) {
        consumers.remove(consumer);
    }

    /**
     * Unregister all given consumers.
     *
     * @param consumers list of {@link AbstractBusDataConsumer}
     */
    public void unregisterConsumers(List<AbstractBusDataConsumer> consumers) {
        this.consumers.removeAll(consumers);
    }

    @Override
    public void received(final int busNr, byte[] data) {
        final boolean initialCall;
        final byte[] oldData;

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
        callConsumers(consumers, busNr, data, oldData, initialCall);

        // store actual data to compare as old data by next call
        busData.put(busNr, data);
    }

    /**
     * Call the registered consumers for the given bus data.
     * Changes of bus data can be checked from the new and old data to call consumers.
     *
     * @param consumers consumers to call
     * @param busNr number of bus
     * @param data new data of the bus
     * @param oldData old data of the bus
     * @param initialCall indicate the first call of the given consumers
     */
    private void callConsumers(List<AbstractBusDataConsumer> consumers, int busNr, byte[] data, byte[] oldData,
            boolean initialCall) {
        final Collection<AbstractBusDataConsumer> consumersToCall = Lists.newArrayList(consumers);

        // call the multi address consumers
        Collection<BusMultiAddressDataConsumer> multiAddressConsumers = getConsumersOfType(consumersToCall,
                BusMultiAddressDataConsumer.class);
        for (final BusMultiAddressDataConsumer multiAddressDataConsumer : multiAddressConsumers) {
            fireMultiAddressChange(multiAddressDataConsumer, busNr, oldData, data, initialCall);
        }
        consumersToCall.removeAll(multiAddressConsumers);

        // fire changes one after another for each address
        for (int i = 0; i < data.length; i++) {
            if (initialCall || Byte.compare(data[i], oldData[i]) != 0) {

                // TODO: refactor to consumerDispatchers ?

                for (AbstractBusDataConsumer consumer : consumersToCall) {
                    if (consumer instanceof AllBusDataConsumer) {
                        callAllBusDataConsumers(busNr, i, oldData[i], data[i], (AllBusDataConsumer) consumer);
                    } else if (consumer instanceof BusBitConsumer) {
                        callBitAddressConsumer((BusBitConsumer) consumer, busNr, i, oldData[i], data[i],
                                initialCall);
                    } else if (consumer instanceof BusAddressDataConsumer) {
                        callBusAddressDataConsumer((BusAddressDataConsumer) consumer, busNr, i, oldData[i], data[i]);
                    } else {
                        String errorMsg = "unknown consumer: " + consumer.getClass().getName();
                        log.error(errorMsg);
                        throw new RuntimeException(errorMsg);
                    }
                }
            }
        }
    }

    /**
     * Call the {@link AllBusDataConsumer}.
     *
     * @param busNr number of bus
     * @param address address of bus
     * @param oldData old data of address
     * @param newData new data of address
     * @param consumer consumer to call
     */
    private void callAllBusDataConsumers(final int busNr, final int address, final int oldData, final int newData,
            final AllBusDataConsumer consumer) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                consumer.valueChanged(busNr, address, oldData, newData);
            }
        });
    }

    /**
     * Call the {@link BusBitConsumer}.
     *
     * @param busNr number of bus
     * @param address address of bus
     * @param oldData old data of address
     * @param newData new data of address
     * @param consumer consumer to call
     * @param initialCall indicate the first call for the given consumer
     */
    private void callBitAddressConsumer(final BusBitConsumer consumer, final int busNr, final int address,
            final int oldData, final int newData, boolean initialCall) {
        // bit change
        if (consumer.getAddress() == address && consumer.getBus() == busNr) {
            final boolean oldBitState = BigInteger.valueOf(oldData)
                    .testBit(consumer.getBit() - 1);
            final boolean newBitState = BigInteger.valueOf(newData)
                    .testBit(consumer.getBit() - 1);
            if (initialCall || oldBitState != newBitState) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        consumer.valueChanged(oldBitState ? 1 : 0, newBitState ? 1 : 0);
                    }
                });
            }
        }
    }

    /**
     * Call the {@link BusAddressDataConsumer}.
     *
     * @param busNr number of bus
     * @param address address of bus
     * @param oldData old data of address
     * @param newData new data of address
     * @param consumer consumer to call
     */
    private void callBusAddressDataConsumer(final BusAddressDataConsumer consumer, final int busNr, final int address,
            final int oldData, final int newData) {
        if (consumer.getAddress() == address && consumer.getBus() == busNr) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    consumer.valueChanged(oldData, newData);
                }
            });
        }
    }

    /**
     * Call asynchronous the
     * {@link net.wbz.selectrix4java.bus.consumption.BusMultiAddressDataConsumer}
     * s for data value changes of the addresses.
     *
     * @param multiAddressDataConsumer consumer to call
     * @param busNr number of bus
     * @param oldData old data of bus
     * @param data new data of bus
     * @param initialCall {@code true} to fire instead of same old and new data
     *            value
     */
    private void fireMultiAddressChange(final BusMultiAddressDataConsumer multiAddressDataConsumer, int busNr,
            byte[] oldData, byte[] data, boolean initialCall) {
        boolean anyAddressDataChanged = false;
        if (busNr == multiAddressDataConsumer.getBus()) {
            final Set<BusAddressData> busAddressData = Sets.newHashSet();
            for (int addressIndex = 0; addressIndex < multiAddressDataConsumer.getAddresses().length; addressIndex++) {
                int busAddress = multiAddressDataConsumer.getAddresses()[addressIndex];
                if (Byte.compare(data[busAddress], oldData[busAddress]) != 0) {
                    anyAddressDataChanged = true;
                }
                busAddressData.add(new BusAddressData(busNr, busAddress, oldData[busAddress],
                        data[busAddress]));
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
