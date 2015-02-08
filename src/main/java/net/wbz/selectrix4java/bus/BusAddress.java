package net.wbz.selectrix4java.bus;

import net.wbz.selectrix4java.bus.consumption.BusAddressDataConsumer;
import net.wbz.selectrix4java.bus.consumption.BusDataConsumer;
import net.wbz.selectrix4java.data.BusData;
import net.wbz.selectrix4java.data.BusDataChannel;

import java.math.BigInteger;
import java.util.List;
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

    private final Queue<BusListener> listeners = new ConcurrentLinkedQueue<>();

    public BusAddress(final int bus, final byte address, BusDataChannel busDataChannel) {
        this.bus = bus;
        this.address = address;
        this.busDataChannel = busDataChannel;

        busDataConsumer = new BusAddressDataConsumer(bus, address) {
            @Override
            public void valueChanged(int oldValue, int newValue) {
                data = (byte) newValue;
                //TODO async?
                fireDataChanged(oldValue, newValue);
            }
        };
    }

    private void fireDataChanged(int oldValue, int newValue) {
        for (BusListener listener : listeners) {
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
     * @return {@link net.wbz.selectrix4java.bus.BusAddress}
     */
    public BusAddress setBit(int bit) {
        data = BigInteger.valueOf(data).setBit(bit - 1).byteValue();
        return this;
    }

    /**
     * Turn bit off.
     *
     * @param bit number of bit (1-8)
     * @return {@link net.wbz.selectrix4java.bus.BusAddress}
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
