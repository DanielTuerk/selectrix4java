package net.wbz.selectrix4java.api.bus;

import net.wbz.selectrix4java.api.data.BusData;
import net.wbz.selectrix4java.api.data.BusDataChannel;

import java.math.BigInteger;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Address of an bus. Wrap the data value and send state change events.
 *
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class BusAddress {

    private final int bus;
    private final byte address;
    private byte data = 0;
    private final BusDataChannel busDataChannel;

    private final BusDataConsumer busDataConsumer;

    private final Queue<BusAddressListener> listeners = new ConcurrentLinkedQueue<>();

    public BusAddress(final int bus, final byte address, BusDataChannel busDataChannel) {
        this.bus = bus;
        this.address = address;
        this.busDataChannel = busDataChannel;

        busDataConsumer = new BusDataConsumer(bus, address) {
            @Override
            public void valueChanged(int oldValue, int newValue) {
                //TODO async?
                for (BusAddressListener listener : listeners) {
                    listener.dataChanged((byte) oldValue, (byte) newValue);
                }
            }
        };
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
    public void sendData(byte data) {
        this.data = data;
        send();
    }

    /**
     * Send the actual data of this address to the bus.
     */
    public void send() {
        //TODO: check -> BusData object needed?
        busDataChannel.send(new BusData(bus, address, data));
    }

    /**
     * Turn bit on.
     *
     * @param bit number of bit (1-8)
     * @return {@link net.wbz.selectrix4java.api.bus.BusAddress}
     */
    public BusAddress setBit(int bit) {
        data = BigInteger.valueOf(data).setBit(bit - 1).byteValue();
        return this;
    }

    /**
     * Turn bit off.
     *
     * @param bit number of bit (1-8)
     * @return {@link net.wbz.selectrix4java.api.bus.BusAddress}
     */
    public BusAddress clearBit(int bit) {
        data = BigInteger.valueOf(data).clearBit(bit - 1).byteValue();
        return this;
    }

    /**
     * State of the given bit in the data byte.
     *
     * @param bit 1-8
     * @return state
     */
    public boolean getBitState(int bit) {
        return BigInteger.valueOf(data).testBit(bit - 1);
    }

    /**
     * Add listener to receive data changes.
     * After call of this method the listener will immediately receive the
     * actual data from the {@link net.wbz.selectrix4java.api.bus.BusAddress}.
     *
     * @param listener {@link net.wbz.selectrix4java.api.bus.BusAddressListener}
     */
    public void addListener(BusAddressListener listener) {
        listeners.add(listener);
        listener.dataChanged((byte) 0, data);
    }

    /**
     * Remove the given listener.
     *
     * @param listener {@link net.wbz.selectrix4java.api.bus.BusAddressListener}
     */
    public void removeListener(BusAddressListener listener) {
        listeners.remove(listener);
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
     * Consumer for this {@link net.wbz.selectrix4java.api.bus.BusAddress} to refresh the data value.
     *
     * @return {@link net.wbz.selectrix4java.api.bus.BusDataConsumer}
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
