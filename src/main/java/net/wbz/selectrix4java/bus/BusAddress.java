package net.wbz.selectrix4java.bus;

import com.google.common.collect.Maps;
import net.wbz.selectrix4java.bus.consumption.BusAddressDataConsumer;
import net.wbz.selectrix4java.bus.consumption.BusDataConsumer;
import net.wbz.selectrix4java.data.BusData;
import net.wbz.selectrix4java.data.BusDataChannel;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.FutureTask;

/**
 * Address of an bus. Wrap the data value and send state change events.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class BusAddress {

    /**
     * Bus number for the address.
     */
    private final int bus;

    /**
     * Number of the address in the bus.
     */
    private final byte address;

    /**
     * Current data for the bus address received from device.
     */
    private byte data = 0;

    /**
     * Bit state to toggle by next {#send} call. Set the state of bit by {#setBit} and {#clearBit}.
     */
    private Map<Integer, Boolean> bitsToUpdate = Maps.newConcurrentMap();

    private final BusDataChannel busDataChannel;

    private final BusDataConsumer busDataConsumer;

    private final Queue<BusListener> listeners = new ConcurrentLinkedQueue<>();

    public BusAddress(final int bus, final byte address, BusDataChannel busDataChannel) {
        this.bus = bus;
        this.address = address;
        this.busDataChannel = busDataChannel;

        busDataConsumer = new BusAddressDataConsumer(bus, address) {
            @Override
            public void valueChanged(int oldValue, int newValue) {
                if ((byte) newValue != data) {
                    data = (byte) newValue;
                    // only fire changes, initial data changed call for the current value is done by addListener
                    fireDataChanged(oldValue, newValue);
                }
            }
        };
    }

    /**
     * Call the registered listeners for value change of the bus address.
     *
     * @param oldValue old data value
     * @param newValue new data value
     */
    private void fireDataChanged(final int oldValue, final int newValue) {
        for (final BusListener listener : listeners) {
            new FutureTask<>(new Callable<Void>() {
                @Override
                public Void call() throws Exception {

                    if (listener instanceof BusAddressListener) {
                        ((BusAddressListener) listener).dataChanged((byte) oldValue, (byte) newValue);
                    } else if (listener instanceof BusAddressBitListener) {
                        BusAddressBitListener busAddressBitListener = (BusAddressBitListener) listener;
                        boolean oldBitValue = BigInteger.valueOf(oldValue).testBit(busAddressBitListener.getBitNr() - 1);
                        boolean newBitValue = BigInteger.valueOf(newValue).testBit(busAddressBitListener.getBitNr() - 1);

                        if (!busAddressBitListener.isCalled() || oldBitValue != newBitValue) {
                            busAddressBitListener.bitChanged(oldBitValue, newBitValue);
                            busAddressBitListener.setCalled(true);
                        }
                    } else {
                        throw new RuntimeException("unknown bus listener instance: " + listener.getClass().getName());
                    }

                    return null;
                }
            }).run();
        }
    }

    /**
     * Actual data of the address.
     *
     * @return data
     */
    public byte getData() {
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
    }

    /**
     * Send the actual data of this address to the bus.
     */
    public synchronized void send() {
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
    public BusAddress setBit(int bit) {
        bitsToUpdate.put(bit, true);
        return this;
    }

    /**
     * Turn bit off.
     *
     * @param bit number of bit (1-8)
     * @return {@link net.wbz.selectrix4java.bus.BusAddress}
     */
    public BusAddress clearBit(int bit) {
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
        this.listeners.add(listener);
        fireDataChanged(0, data);
    }

    /**
     * Remove the given listener.
     *
     * @param listener {@link net.wbz.selectrix4java.bus.BusListener}
     */
    public void removeListener(BusListener listener) {
        this.listeners.remove(listener);
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

    /**
     * Address on the bus.
     *
     * @return byte
     */
    public byte getAddress() {
        return address;
    }

    /**
     * Consumer for this {@link net.wbz.selectrix4java.bus.BusAddress} to refresh the data value.
     *
     * @return {@link net.wbz.selectrix4java.bus.consumption.BusDataConsumer}
     */
    public BusDataConsumer getConsumer() {
        return busDataConsumer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BusAddress address1 = (BusAddress) o;

        return address == address1.address && bus == address1.bus;
    }

    @Override
    public int hashCode() {
        int result = bus;
        result = 31 * result + (int) address;
        return result;
    }
}
