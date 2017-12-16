package net.wbz.selectrix4java.bus;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

import net.wbz.selectrix4java.bus.consumption.AbstractBusDataConsumer;
import net.wbz.selectrix4java.bus.consumption.BusAddressDataConsumer;
import net.wbz.selectrix4java.data.BusData;
import net.wbz.selectrix4java.data.BusDataChannel;

/**
 * Address of an bus. Wrap the data value and send state change events.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class BusAddress {
    private static final Logger LOG = LoggerFactory.getLogger(BusAddress.class);

    /**
     * Bus number for the address.
     */
    private final int bus;

    /**
     * Number of the address in the bus.
     */
    private final int address;
    private final BusDataChannel busDataChannel;
    private final AbstractBusDataConsumer busDataConsumer;
    private final BusAddressDataDispatcher dispatcher = new BusAddressDataDispatcher();
    /**
     * Bit state to toggle by next {#send} call. Set the state of bit by {#setBit} and {#clearBit}.
     */
    private final Map<Integer, Boolean> bitsToUpdate = new ConcurrentHashMap<>(8);
    /**
     * Current data for the bus address before called {#send}.
     */
    private volatile byte data = 0;
    /**
     * Last received data. Is only updated by received changed data.
     */
    private volatile byte lastReceivedData = -1;

    public BusAddress(final int bus, final int address, BusDataChannel busDataChannel) {
        this.bus = bus;
        this.address = address;
        this.busDataChannel = busDataChannel;

        // add consumer for the address to receive the actual data for this address
        busDataConsumer = new BusAddressDataConsumer(bus, address) {
            @Override
            public void valueChanged(int oldValue, int newValue) {
                synchronized (BusAddress.this) {
                    if ((byte) newValue != lastReceivedData) {
                        lastReceivedData = (byte) newValue;
                        if (bitsToUpdate.isEmpty()) {
                            LOG.trace("update data {} to last received {}", data, lastReceivedData);
                            data = lastReceivedData;
                        }
                        /*
                         * Only fire changes, initial data changed call for the current value is done by initial call in
                         * the consumer
                         */
                        dispatcher.fireValueChanged(oldValue, newValue);
                    }
                }
            }
        };
    }

    /**
     * Actual data of the address.
     *
     * @return data
     */
    public synchronized byte getData() {
        return data;
    }

    /**
     * Update data of the address and send to bus.
     *
     * @param data new data
     */
    public synchronized void sendData(byte data) {
        // send new data value to channel; actual data value is updated async by consumer
        busDataChannel.send(new BusData(bus, address, data));
        this.data = data;
    }

    /**
     * Send the actual data of this address to the bus.
     */
    public synchronized void send() {
        LOG.trace("{} -> send - data {}", this.toString(), data);
        BigInteger dataToSend = BigInteger.valueOf(data);
        // check for bit manipulation to send for current data value
        if (!bitsToUpdate.isEmpty()) {
            for (Map.Entry<Integer, Boolean> entry : bitsToUpdate.entrySet()) {
                if (entry.getValue()) {
                    dataToSend = dataToSend.setBit(entry.getKey() - 1);
                } else {
                    dataToSend = dataToSend.clearBit(entry.getKey() - 1);
                }
            }
            data = dataToSend.byteValue();
            bitsToUpdate.clear();
        }
        busDataChannel.send(new BusData(bus, address, dataToSend.byteValue()));
    }

    /**
     * Turn bit on.
     *
     * @param bit number of bit (1-8)
     * @return {@link net.wbz.selectrix4java.bus.BusAddress}
     */
    public synchronized BusAddress setBit(int bit) {
        bitsToUpdate.put(bit, true);
        return this;
    }

    /**
     * Turn bit off.
     *
     * @param bit number of bit (1-8)
     * @return {@link net.wbz.selectrix4java.bus.BusAddress}
     */
    public synchronized BusAddress clearBit(int bit) {
        bitsToUpdate.put(bit, false);
        return this;
    }

    /**
     * State of the given bit in the data byte.
     *
     * @param bit 1-8
     * @return state
     */
    public boolean getBitState(int bit) {
        // check first bits in update session to update the data value during next send call
        if (bitsToUpdate.containsKey(bit)) {
            return bitsToUpdate.get(bit);
        }
        // check bit state of actual data value
        return BigInteger.valueOf(data).testBit(bit - 1);
    }

    /**
     * Add listener to receive data changes.
     * After call of this method the listener will immediately receive the
     * actual data from the {@link net.wbz.selectrix4java.bus.BusAddress}.
     *
     * @param listener {@link net.wbz.selectrix4java.bus.BusListener}
     */
    public void addListener(BusListener listener) {
        dispatcher.addListener(listener);
    }

    /**
     * Remove the given listener.
     *
     * @param listener {@link net.wbz.selectrix4java.bus.BusListener}
     */
    public void removeListener(BusListener listener) {
        dispatcher.removeListener(listener);
    }

    public void addListeners(List<BusListener> listeners) {
        for (BusListener listener : listeners) {
            addListener(listener);
        }
    }

    public void removeListeners(List<BusListener> listeners) {
        for (BusListener listener : listeners) {
            removeListener(listener);
        }
    }

    public int getBus() {
        return bus;
    }

    /**
     * Address on the bus.
     *
     * @return byte
     */
    public int getAddress() {
        return address;
    }

    /**
     * Consumer for this {@link net.wbz.selectrix4java.bus.BusAddress} to refresh the data value.
     *
     * @return {@link AbstractBusDataConsumer}
     */
    public AbstractBusDataConsumer getConsumer() {
        return busDataConsumer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        BusAddress address1 = (BusAddress) o;

        return address == address1.address && bus == address1.bus;
    }

    @Override
    public int hashCode() {
        int result = bus;
        result = 31 * result + address;
        return result;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("bus", bus)
                .add("address", address)
                .toString();
    }
}
