package net.wbz.selectrix4java.data;

/**
 * @author Daniel Tuerk
 */
public class BusData {

    private final int bus;
    private final int address;
    private final int data;

    public BusData(int bus, int address, int data) {
        this.bus = bus;
        this.address = address;
        this.data = data;
    }

    public int getBus() {
        return bus;
    }

    public int getAddress() {
        return address;
    }

    public int getData() {
        return data;
    }

}
