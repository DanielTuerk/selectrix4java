package net.wbz.selectrix4java.api.bus;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public void registerConsumer(BusDataConsumer consumer) {
        consumers.add(consumer);

        if (consumer instanceof AllBusDataConsumer) {
            for (int bus : busData.keySet()) {
                for (int i = 0; i < busData.get(bus).length; i++) {
                    ((AllBusDataConsumer) consumer).valueChanged(bus, i, 0, busData.get(bus)[i]);
                }
            }
        } else {
            if (busData.containsKey(consumer.getBus()) && busData.get(consumer.getBus()).length > consumer.getAddress()) {
                consumer.valueChanged(0, busData.get(consumer.getBus())[consumer.getAddress()]);
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

    /**
     * Perform an update by calling all registered consumers with the current data of the bus addresses.
     * Each consumer will be called for an value change.
     */
    public void requestConsumersDataUpdate() {

    }

    @Override
    public void received(int busNr, byte[] data) {
        boolean firstTimeDataReceived = !busData.containsKey(busNr);
        if (busData.containsKey(busNr)) {
            byte[] oldData = busData.get(busNr);
            for (int i = 0; i < data.length; i++) {
                if (firstTimeDataReceived || Byte.compare(data[i], oldData[i]) != 0) {
                    fireChange(busNr, i, oldData[i], data[i]);
                }
            }
        }
        busData.put(busNr, data);
    }

    private void fireChange(final int busNr, final int address, final int oldData, final int newData) {
        //TODO: async consumer calls?
        for (final BusDataConsumer consumer : consumers) {
            if (consumer instanceof AllBusDataConsumer) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        ((AllBusDataConsumer) consumer).valueChanged(busNr, address, oldData, newData);
                    }
                });
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
