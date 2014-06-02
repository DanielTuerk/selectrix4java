package net.wbz.selecttrix4java.api.bus;

import net.wbz.selecttrix4java.api.data.BusData;
import net.wbz.selecttrix4java.api.data.BusDataChannel;

import java.math.BigInteger;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Daniel Tuerk (daniel.tuerk@w-b-z.com)
 */
public class BusAddress {

    private final int bus;
    private final byte address;
    private byte data;
    private final BusDataChannel busDataChannel;


    private final BusDataConsumer busDataConsumer;

    private final Queue<BusAddressListener> listeners = new ConcurrentLinkedQueue<>();

    public BusAddress(final int bus, final byte address, BusDataChannel busDataChannel) {
        this.bus = bus;
        this.address = address;
        this.busDataChannel = busDataChannel;

        busDataConsumer = new BusDataConsumer(bus, address) {
            @Override
            public void valueChanged(int value) {
                //TODO async?
                for (BusAddressListener listener : listeners) {
                    listener.dataChanged(data, (byte) value);
                }
                data = (byte) value;
            }
        };
    }

    public byte getData() {
        return data;
    }

    public void sendData(byte data) {
        this.data = data;
        send();
    }

    public void send() {
        //TODO: check -> BusData object needed?
        busDataChannel.send(new BusData(bus, address, data));
    }

    public BusAddress setBit(int bit) {
        data = BigInteger.valueOf(data).setBit(bit).byteValue();
        return this;
    }

    public BusAddress clearBit(int bit) {
        data = BigInteger.valueOf(data).clearBit(bit).byteValue();
        return this;
    }

    public void addListener(BusAddressListener listener) {
        listeners.add(listener);
    }

    public void removeListener(BusAddressListener listener) {
        listeners.remove(listener);
    }

    public byte getAddress() {
        return address;
    }

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
