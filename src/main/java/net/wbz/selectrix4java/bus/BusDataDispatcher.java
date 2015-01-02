package net.wbz.selectrix4java.bus;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * The dispatcher stores all values for each address of the SX bus channels.
 * To receive the values it implements {@link BusDataReceiver}.
 * <p/>
 * For each value which has changed, the registered {@link BusDataConsumer}s are informed.
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
     * To get the values of all addresses use the {@link AllBusDataConsumer}.
     *
     * @param consumer {@link BusDataConsumer}
     */
    public void registerConsumer(final BusDataConsumer consumer) {
        consumers.add(consumer);

        // fire current value for the consumer
        if (consumer instanceof AllBusDataConsumer) {
            for (int bus : busData.keySet()) {
                for (int i = 0; i < busData.get(bus).length; i++) {
                    ((AllBusDataConsumer) consumer).valueChanged(bus, i, 0, busData.get(bus)[i]);
                }
            }
        } else {
            final byte addressValue = busData.get(consumer.getBus())[consumer.getAddress()];
            if (consumer instanceof BusBitConsumer) {
                // fire bit state from the actual bus data for the registered bit state
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        consumer.valueChanged(0, BigInteger.valueOf(addressValue).testBit(((BusBitConsumer) consumer).getBit() - 1) ? 1 : 0);
                    }
                });
            } else if (busData.containsKey(consumer.getBus()) && busData.get(consumer.getBus()).length > consumer.getAddress()) {
                // fire value of the address from the actual bus data
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        consumer.valueChanged(0, addressValue);
                    }
                });
            }
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
    public void received(int busNr, byte[] data) {
        if (busData.containsKey(busNr)) {
            byte[] oldData = busData.get(busNr);
            for (int i = 0; i < data.length; i++) {
                if (Byte.compare(data[i], oldData[i]) != 0) {
                    fireChange(busNr, i, oldData[i], data[i]);
                }
            }
        } else {
            // first time received data of the bus nr; fire initial states for each address
            for (int i = 0; i < data.length; i++) {
                fireChange(busNr, i, 0, data[i]);
            }
        }
        busData.put(busNr, data);
    }

    private void fireChange(final int busNr, final int address, final int oldData, final int newData) {
        for (final BusDataConsumer consumer : consumers) {
            if (consumer instanceof AllBusDataConsumer) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        ((AllBusDataConsumer) consumer).valueChanged(busNr, address, oldData, newData);
                    }
                });
            } else if (consumer instanceof BusBitConsumer) {
                if (consumer.getAddress() == address && consumer.getBus() == busNr) {
                    final boolean oldBitState = BigInteger.valueOf(oldData).testBit(((BusBitConsumer) consumer).getBit() - 1);
                    final boolean newBitState = BigInteger.valueOf(newData).testBit(((BusBitConsumer) consumer).getBit() - 1);
                    if (oldBitState != newBitState) {
                        executorService.submit(new Runnable() {
                            @Override
                            public void run() {
                                consumer.valueChanged(oldBitState ? 1 : 0, newBitState ? 1 : 0);
                            }
                        });
                    }
                }
            } else if (consumer.getAddress() == address && consumer.getBus() == busNr) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        consumer.valueChanged(oldData, newData);
                    }
                });
            }
        }
    }
}
