package net.wbz.selectrix4java.bus;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.wbz.selectrix4java.bus.consumption.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * The dispatcher stores all values for each address of the SX bus channels.
 * To receive the values it implements {@link BusDataReceiver}.
 * <p/>
 * For each value which has changed, the registered {@link net.wbz.selectrix4java.bus.consumption.BusDataConsumer}s
 * are informed.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class BusDataDispatcher implements BusDataReceiver {
    private static final Logger log = LoggerFactory.getLogger(BusDataDispatcher.class);

    private Map<Integer, byte[]> busData = Maps.newConcurrentMap();
    private Queue<BusDataConsumer> consumers = new ConcurrentLinkedQueue<>();
    private final ExecutorService executorService;

    public BusDataDispatcher() {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("bus-data-dispatcher-%d").build();
        executorService = Executors.newCachedThreadPool(namedThreadFactory);
    }

    /**
     * Return the current values of all addresses for the given SX bus.
     *
     * @param busNr number of the SX bus (e.g.0 or 1)
     * @return byte array which represents the address as index with the byte value of the address.
     */
    public byte[] getData(int busNr) {
        if (busData.containsKey(busNr)) {
            return busData.get(busNr);
        }
        throw new RuntimeException(String.format("no bus found for number '%d'", busNr));
    }

    /**
     * Register an new consumer to get state changed of for the values of an address from an SX bus.
     * To get the values of all addresses use the {@link net.wbz.selectrix4java.bus.consumption.AllBusDataConsumer}.
     *
     * @param consumer {@link BusDataConsumer}
     */
    public void registerConsumer(final BusDataConsumer consumer) {
        consumers.add(consumer);

        // fire current value for the consumer
        if (consumer instanceof AllBusDataConsumer || consumer instanceof BusMultiAddressDataConsumer) {
            for (final int bus : busData.keySet()) {
                for (int i = 0; i < busData.get(bus).length; i++) {
                    if (consumer instanceof AllBusDataConsumer) {
                        final int finalI = i;
                        executorService.submit(new Runnable() {
                            @Override
                            public void run() {
                                ((AllBusDataConsumer) consumer).valueChanged(bus, finalI, 0, busData.get(bus)[finalI]);
                            }
                        });
                    }
                }
                byte[] oldData = new byte[busData.get(bus).length];
                Arrays.fill(oldData, (byte) 0);
                fireMultiAddressChange(Lists.newArrayList(consumer), bus, oldData, busData.get(bus), true);
            }
        } else if (consumer instanceof BusAddressDataConsumer) {
            final BusAddressDataConsumer busAddressDataConsumer = (BusAddressDataConsumer) consumer;
            // check for valid consumer
            if (busData.containsKey(busAddressDataConsumer.getBus()) && busData.get(busAddressDataConsumer.getBus()).length > busAddressDataConsumer.getAddress()) {
                final byte addressValue = busData.get(busAddressDataConsumer.getBus())[busAddressDataConsumer.getAddress()];
                if (consumer instanceof BusBitConsumer) {
                    // fire bit state from the actual bus data for the registered bit state
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            busAddressDataConsumer.valueChanged(0, BigInteger.valueOf(addressValue).testBit(((BusBitConsumer) consumer).getBit() - 1) ? 1 : 0);
                        }
                    });
                } else {
                    // fire value of the address from the actual bus data
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            busAddressDataConsumer.valueChanged(0, addressValue);
                        }
                    });
                }
            }
        } else {
            throw new RuntimeException(String.format("registered invalid consumer: %s", consumer.getClass().getName()));
        }

    }

    public void registerConsumers(List<BusDataConsumer> consumers) {
        for (BusDataConsumer consumer : consumers) {
            registerConsumer(consumer);
        }
    }

    public void unregisterConsumer(BusDataConsumer consumer) {
        consumers.remove(consumer);
    }

    public void unregisterConsumers(List<BusDataConsumer> consumers) {
        this.consumers.removeAll(consumers);
    }

    @Override
    public void received(final int busNr, byte[] data) {
        final boolean initialCall;
        final byte[] oldData;
        if (busData.containsKey(busNr)) {
            initialCall = false;
            oldData = busData.get(busNr);
            // fire changes one after another for each address
            for (int i = 0; i < data.length; i++) {
                if (Byte.compare(data[i], oldData[i]) != 0) {
                    fireSingleAddressChange(busNr, i, oldData[i], data[i]);
                }
            }
        } else {
            initialCall = true;
            // first timestamp received data of the bus nr; fire initial states for each address
            oldData = new byte[data.length];
            Arrays.fill(oldData, (byte) 0);
            for (int i = 0; i < data.length; i++) {
                fireSingleAddressChange(busNr, i, 0, data[i]);
            }
        }

        // fire changes for multi address consumers
        fireMultiAddressChange(Collections2.filter(consumers, new Predicate<BusDataConsumer>() {
            @Override
            public boolean apply(BusDataConsumer input) {
                return input instanceof BusMultiAddressDataConsumer;
            }
        }), busNr, oldData, data, initialCall);

        // store actual data to compare as old data by next call

        busData.put(busNr, data);
    }

    /**
     * Call asynchronous the consumers by executor for the given values.
     *
     * @param busNr   0 or 1
     * @param address address of changed data value
     * @param oldData old data value of address
     * @param newData new data value of address
     */
    private void fireSingleAddressChange(final int busNr, final int address, final int oldData, final int newData) {
        for (final BusDataConsumer consumer : consumers) {
            if (consumer instanceof AllBusDataConsumer) {
                // all data
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        ((AllBusDataConsumer) consumer).valueChanged(busNr, address, oldData, newData);
                        consumer.setCalled(true);
                    }
                });
            } else if (consumer instanceof BusBitConsumer) {
                // bit change
                final BusBitConsumer busBitConsumer = (BusBitConsumer) consumer;
                if (busBitConsumer.getAddress() == address && consumer.getBus() == busNr) {
                    final boolean oldBitState = BigInteger.valueOf(oldData).testBit(((BusBitConsumer) consumer).getBit() - 1);
                    final boolean newBitState = BigInteger.valueOf(newData).testBit(((BusBitConsumer) consumer).getBit() - 1);
                    if (!consumer.isCalled() || oldBitState != newBitState) {
                        executorService.submit(new Runnable() {
                            @Override
                            public void run() {
                                busBitConsumer.valueChanged(oldBitState ? 1 : 0, newBitState ? 1 : 0);
                                busBitConsumer.setCalled(true);
                            }
                        });
                    }
                }
            } else if (consumer instanceof BusAddressDataConsumer) {
                // address change
                final BusAddressDataConsumer busAddressDataConsumer = (BusAddressDataConsumer) consumer;
                if (busAddressDataConsumer.getAddress() == address && consumer.getBus() == busNr) {
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            busAddressDataConsumer.valueChanged(oldData, newData);
                            consumer.setCalled(true);
                        }
                    });
                }
            }
        }
    }

    /**
     * Call asynchronous the {@link net.wbz.selectrix4java.bus.consumption.BusMultiAddressDataConsumer}s
     * for data value changes of the addresses.
     *
     * @param multiAddressConsumers list of {@link net.wbz.selectrix4java.bus.consumption.BusMultiAddressDataConsumer}
     * @param busNr                 number of bus
     * @param oldData               old data of bus
     * @param data                  new data of bus
     * @param initialCall           {@code true} to fire instead of same old and new data value
     */
    private void fireMultiAddressChange(Collection<BusDataConsumer> multiAddressConsumers, int busNr,
                                        byte[] oldData, byte[] data, boolean initialCall) {
        for (final BusDataConsumer consumer : multiAddressConsumers) {
            boolean anyAddressDataChanged = false;
            final BusMultiAddressDataConsumer multiAddressDataConsumer = (BusMultiAddressDataConsumer) consumer;
            if (busNr == multiAddressDataConsumer.getBus()) {
                final BusAddressData[] busAddressData = new BusAddressData[multiAddressDataConsumer.getAddresses().length];
                for (int addressIndex = 0; addressIndex < multiAddressDataConsumer.getAddresses().length; addressIndex++) {
                    int busAddress = multiAddressDataConsumer.getAddresses()[addressIndex];
                    if (Byte.compare(data[busAddress], oldData[busAddress]) != 0) {
                        anyAddressDataChanged = true;
                    }
                    busAddressData[addressIndex] = new BusAddressData(busNr, busAddress, oldData[busAddress], data[busAddress]);
                }
                if (anyAddressDataChanged || initialCall) {
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            multiAddressDataConsumer.valueChanged(busAddressData);
                            multiAddressDataConsumer.setCalled(true);
                        }
                    });
                }
            }
        }
    }
}
